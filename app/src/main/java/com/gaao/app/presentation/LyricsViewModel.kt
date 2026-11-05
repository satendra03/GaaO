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
import com.gaao.app.data.model.PlaybackState
import com.gaao.app.domain.SyncState
import com.gaao.app.domain.SynchronizationEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LyricsViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaSessionRepository.initialize(application)
    private val lyricsRepository = LyricsRepository.initialize(application)

    private val _isPermissionGranted = MutableStateFlow(
        MediaNotificationListener.isPermissionGranted(application)
    )
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _estimatedPositionMs = MutableStateFlow(0L)
    val estimatedPositionMs: StateFlow<Long> = _estimatedPositionMs.asStateFlow()

    private val _lyricsResult = MutableStateFlow<LyricsResult>(LyricsResult.Idle)
    val lyricsResult: StateFlow<LyricsResult> = _lyricsResult.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Default +200ms lead offset to compensate for Android audio buffer & Bluetooth latency
    private val _syncOffsetMs = MutableStateFlow(200L)
    val syncOffsetMs: StateFlow<Long> = _syncOffsetMs.asStateFlow()

    private val _showDiagnosticOverlay = MutableStateFlow(false)
    val showDiagnosticOverlay: StateFlow<Boolean> = _showDiagnosticOverlay.asStateFlow()

    val isListenerConnected: StateFlow<Boolean> = MediaNotificationListener.isListenerConnected
    val playbackInfo = mediaRepository.playbackInfo
    val statusMessage = mediaRepository.statusMessage

    private var activeTrackKey: String? = null

    // Unified UI state combining all streams
    val uiState: StateFlow<GaaOUiState> = combine(
        _isPermissionGranted,
        isListenerConnected,
        playbackInfo,
        _lyricsResult,
        _estimatedPositionMs,
        _syncState,
        _syncOffsetMs,
        _showDiagnosticOverlay
    ) { params ->
        val granted = params[0] as Boolean
        val connected = params[1] as Boolean
        val info = params[2] as com.gaao.app.data.model.MediaPlaybackInfo?
        val lyricsRes = params[3] as LyricsResult
        val posMs = params[4] as Long
        val sync = params[5] as SyncState
        val offset = params[6] as Long
        val showDiag = params[7] as Boolean

        val appState = when {
            !granted -> GaaOAppState.PERMISSION_REQUIRED
            info == null || info.trackTitle.isNullOrBlank() -> GaaOAppState.IDLE
            lyricsRes is LyricsResult.Loading -> GaaOAppState.LOADING_LYRICS
            lyricsRes is LyricsResult.Offline -> GaaOAppState.OFFLINE
            lyricsRes is LyricsResult.Unavailable -> GaaOAppState.LYRICS_UNAVAILABLE
            lyricsRes is LyricsResult.Error -> GaaOAppState.ERROR
            lyricsRes is LyricsResult.Success -> {
                if (info.playbackState == PlaybackState.PAUSED) {
                    GaaOAppState.PAUSED
                } else {
                    GaaOAppState.PLAYING
                }
            }
            else -> GaaOAppState.IDLE
        }

        val lines = if (lyricsRes is LyricsResult.Success) lyricsRes.lines else emptyList()
        val errorMsg = if (lyricsRes is LyricsResult.Error) lyricsRes.message else null

        GaaOUiState(
            appState = appState,
            isPermissionGranted = granted,
            isListenerConnected = connected,
            trackTitle = info?.trackTitle,
            artist = info?.artist,
            album = info?.album,
            appDisplayName = info?.appDisplayName,
            durationMs = info?.durationMs ?: 0L,
            currentPositionMs = posMs,
            lyrics = lines,
            syncState = sync,
            syncOffsetMs = offset,
            errorMessage = errorMsg,
            showDiagnosticOverlay = showDiag
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GaaOUiState()
    )

    init {
        // High frequency position tracker & synchronization loop
        viewModelScope.launch {
            while (isActive) {
                updateEstimatedPosition()
                val isPlaying = playbackInfo.value?.playbackState == PlaybackState.PLAYING
                delay(if (isPlaying) 50L else 200L)
            }
        }

        // Automatic song detection & lyrics fetch
        viewModelScope.launch {
            playbackInfo.collect { info ->
                updateEstimatedPosition()

                val trackTitle = info?.trackTitle?.trim()
                val artist = info?.artist?.trim()

                if (trackTitle.isNullOrEmpty() || artist.isNullOrEmpty()) {
                    if (activeTrackKey != null) {
                        activeTrackKey = null
                        _lyricsResult.value = LyricsResult.Idle
                        _syncState.value = SyncState()
                    }
                    return@collect
                }

                val trackKey = "$trackTitle|$artist"
                if (trackKey != activeTrackKey) {
                    activeTrackKey = trackKey
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

    fun adjustSyncOffset(deltaMs: Long) {
        _syncOffsetMs.value = (_syncOffsetMs.value + deltaMs).coerceIn(-500L, 1000L)
        updateSyncState(_estimatedPositionMs.value)
    }

    fun toggleDiagnosticOverlay() {
        _showDiagnosticOverlay.value = !_showDiagnosticOverlay.value
    }

    private fun loadLyrics(title: String, artist: String, durationMs: Long, album: String?) {
        viewModelScope.launch {
            _lyricsResult.value = LyricsResult.Loading
            val result = lyricsRepository.getLyrics(
                trackTitle = title,
                artist = artist,
                durationMs = durationMs,
                album = album
            )
            _lyricsResult.value = result
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
        val currentLyrics = _lyricsResult.value
        if (currentLyrics is LyricsResult.Success) {
            _syncState.value = SynchronizationEngine.findActiveLine(
                lines = currentLyrics.lines,
                currentPositionMs = currentPos,
                offsetMs = _syncOffsetMs.value
            )
        } else {
            _syncState.value = SyncState()
        }
    }
}
