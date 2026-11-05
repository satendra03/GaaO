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
import com.gaao.app.domain.SyncState
import com.gaao.app.domain.SynchronizationEngine
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

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val isListenerConnected: StateFlow<Boolean> = MediaNotificationListener.isListenerConnected
    val playbackInfo: StateFlow<MediaPlaybackInfo?> = mediaRepository.playbackInfo
    val statusMessage: StateFlow<String> = mediaRepository.statusMessage

    private var activeTrackId: String? = null

    init {
        // High-frequency interpolation loop for smooth lyric synchronization
        viewModelScope.launch {
            while (isActive) {
                updateEstimatedPosition()
                val isPlaying = playbackInfo.value?.playbackState == PlaybackState.PLAYING
                delay(if (isPlaying) 50L else 200L)
            }
        }

        // Automatic lyrics retrieval when song changes (FR-03, FR-06)
        viewModelScope.launch {
            playbackInfo.collect { info ->
                // Immediate recalculation for seeks and playback state changes
                updateEstimatedPosition()

                val trackTitle = info?.trackTitle?.trim()
                val artist = info?.artist?.trim()

                if (trackTitle.isNullOrEmpty() || artist.isNullOrEmpty()) {
                    if (activeTrackId != null) {
                        activeTrackId = null
                        _lyricsState.value = LyricsResult.Idle
                        _syncState.value = SyncState()
                    }
                    return@collect
                }

                val trackId = "$trackTitle|$artist"
                if (trackId != activeTrackId) {
                    activeTrackId = trackId
                    _syncState.value = SyncState()
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
            updateSyncState(_estimatedPositionMs.value)
        }
    }

    private fun updateEstimatedPosition() {
        val info = playbackInfo.value ?: run {
            _estimatedPositionMs.value = 0L
            updateSyncState(0L)
            return
        }

        val calculatedPos = if (info.playbackState == PlaybackState.PLAYING) {
            val now = SystemClock.elapsedRealtime()
            val timeDelta = (now - info.lastPositionUpdateTimeMs).coerceAtLeast(0L)
            val estimated = info.positionMs + (timeDelta * info.playbackSpeed).toLong()
            if (info.durationMs > 0) {
                estimated.coerceAtMost(info.durationMs)
            } else {
                estimated
            }
        } else {
            info.positionMs
        }

        _estimatedPositionMs.value = calculatedPos
        updateSyncState(calculatedPos)
    }

    private fun updateSyncState(currentPos: Long) {
        val currentLyrics = _lyricsState.value
        if (currentLyrics is LyricsResult.Success) {
            _syncState.value = SynchronizationEngine.findActiveLine(
                lines = currentLyrics.lines,
                currentPositionMs = currentPos
            )
        } else {
            _syncState.value = SyncState()
        }
    }
}
