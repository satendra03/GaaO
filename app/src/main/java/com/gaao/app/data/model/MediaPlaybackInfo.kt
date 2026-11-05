package com.gaao.app.data.model

enum class PlaybackState {
    NONE,
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    STOPPED
}

data class MediaPlaybackInfo(
    val trackTitle: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val playbackState: PlaybackState = PlaybackState.NONE,
    val packageName: String? = null,
    val appDisplayName: String? = null,
    val lastPositionUpdateTimeMs: Long = 0L
)

