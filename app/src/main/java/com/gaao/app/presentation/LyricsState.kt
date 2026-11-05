package com.gaao.app.presentation

import com.gaao.app.data.model.LyricLine
import com.gaao.app.domain.SyncState

enum class GaaOAppState {
    PERMISSION_REQUIRED,
    IDLE,
    LOADING_LYRICS,
    PLAYING,
    PAUSED,
    OFFLINE,
    LYRICS_UNAVAILABLE,
    ERROR
}

data class GaaOUiState(
    val appState: GaaOAppState = GaaOAppState.IDLE,
    val isPermissionGranted: Boolean = false,
    val isListenerConnected: Boolean = false,
    val trackTitle: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val appDisplayName: String? = null,
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val lyrics: List<LyricLine> = emptyList(),
    val syncState: SyncState = SyncState(),
    val syncOffsetMs: Long = 200L,
    val errorMessage: String? = null,
    val showDiagnosticOverlay: Boolean = false
)
