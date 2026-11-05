package com.gaao.app.presentation

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaao.app.data.lyrics.LyricsRepository
import com.gaao.app.data.media.MediaNotificationListener
import com.gaao.app.data.media.MediaSessionRepository
import com.gaao.app.data.model.LyricsResult
import com.gaao.app.data.model.MediaPlaybackInfo
import com.gaao.app.data.model.PlaybackState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaSessionRepository.initialize(application)
    private val lyricsRepository = LyricsRepository.initialize(application)

    private val _isPermissionGranted = MutableStateFlow(
        MediaNotificationListener.isPermissionGranted(application)
    )
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _estimatedPositionMs = MutableStateFlow(0L)
    val estimatedPositionMs: StateFlow<Long> = _estimatedPositionMs.asStateFlow()

    private val _lyricsState = MutableStateFlow<LyricsResult>(LyricsResult.Idle)
    val lyricsState: StateFlow<LyricsResult> = _lyricsState.asStateFlow()

    val isListenerConnected: StateFlow<Boolean> = MediaNotificationListener.isListenerConnected
    val playbackInfo: StateFlow<MediaPlaybackInfo?> = mediaRepository.playbackInfo
    val statusMessage: StateFlow<String> = mediaRepository.statusMessage

    private var activeTrackId: String? = null

    init {
        // Periodic position tracker for smooth UI display when playing
        viewModelScope.launch {
            while (isActive) {
                updateEstimatedPosition()
                delay(500)
            }
        }

        // Automatic lyrics retrieval when song changes (FR-03, FR-06)
        viewModelScope.launch {
            playbackInfo.collect { info ->
                val trackTitle = info?.trackTitle?.trim()
                val artist = info?.artist?.trim()

                if (trackTitle.isNullOrEmpty() || artist.isNullOrEmpty()) {
                    if (activeTrackId != null) {
                        activeTrackId = null
                        _lyricsState.value = LyricsResult.Idle
                    }
                    return@collect
                }

                val trackId = "$trackTitle|$artist"
                if (trackId != activeTrackId) {
                    activeTrackId = trackId
                    loadLyrics(trackTitle, artist, info.durationMs, info.album)
                }
            }
        }
    }

    fun checkPermissionAndRefresh() {
        val granted = MediaNotificationListener.isPermissionGranted(getApplication())
        _isPermissionGranted.value = granted
        if (granted) {
            mediaRepository.refreshActiveSessions()
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = MediaNotificationListener.createSettingsIntent()
        context.startActivity(intent)
    }

    fun retryLyrics() {
        val info = playbackInfo.value
        val trackTitle = info?.trackTitle?.trim()
        val artist = info?.artist?.trim()
        if (!trackTitle.isNullOrEmpty() && !artist.isNullOrEmpty()) {
            lyricsRepository.clearCache()
            loadLyrics(trackTitle, artist, info.durationMs, info.album)
        }
    }

    private fun loadLyrics(title: String, artist: String, durationMs: Long, album: String?) {
        viewModelScope.launch {
            _lyricsState.value = LyricsResult.Loading
            val result = lyricsRepository.getLyrics(
                trackTitle = title,
                artist = artist,
                durationMs = durationMs,
                album = album
            )
            _lyricsState.value = result
        }
    }

    private fun updateEstimatedPosition() {
        val info = playbackInfo.value ?: run {
            _estimatedPositionMs.value = 0L
            return
        }

        if (info.playbackState == PlaybackState.PLAYING) {
            val now = SystemClock.elapsedRealtime()
            val timeDelta = (now - info.lastPositionUpdateTimeMs).coerceAtLeast(0L)
            val estimated = info.positionMs + (timeDelta * info.playbackSpeed).toLong()
            _estimatedPositionMs.value = if (info.durationMs > 0) {
                estimated.coerceAtMost(info.durationMs)
            } else {
                estimated
            }
        } else {
            _estimatedPositionMs.value = info.positionMs
        }
    }
}
