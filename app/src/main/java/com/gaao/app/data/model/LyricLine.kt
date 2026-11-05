package com.gaao.app.data.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

sealed interface LyricsResult {
    object Idle : LyricsResult
    object Loading : LyricsResult
    data class Success(
        val lines: List<LyricLine>,
        val isSynced: Boolean,
        val trackName: String,
        val artistName: String
    ) : LyricsResult
    object Unavailable : LyricsResult
    object Offline : LyricsResult
    data class Error(val message: String) : LyricsResult
}
