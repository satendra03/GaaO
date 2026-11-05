package com.gaao.app.domain

import com.gaao.app.data.model.LyricLine

data class SyncState(
    val activeIndex: Int = -1,
    val activeLine: LyricLine? = null,
    val nextLine: LyricLine? = null,
    val msUntilNextLine: Long? = null,
    val progressToNextLine: Float = 0f
)

object SynchronizationEngine {

    /**
     * Determines the active lyric line and upcoming timing details based on [currentPositionMs].
     *
     * @param lines Chronologically sorted list of [LyricLine].
     * @param currentPositionMs Current playback position in milliseconds.
     * @param offsetMs Optional synchronization offset (positive moves lyrics earlier, negative later).
     * @return [SyncState] with active index, lines, and progress.
     */
    fun findActiveLine(
        lines: List<LyricLine>,
        currentPositionMs: Long,
        offsetMs: Long = 0L
    ): SyncState {
        if (lines.isEmpty()) {
            return SyncState()
        }

        val adjustedPosition = (currentPositionMs + offsetMs).coerceAtLeast(0L)

        // Before first lyric line (introductory section)
        if (adjustedPosition < lines.first().timestampMs) {
            val firstLine = lines.first()
            val msUntil = (firstLine.timestampMs - adjustedPosition).coerceAtLeast(0L)
            return SyncState(
                activeIndex = -1,
                activeLine = null,
                nextLine = firstLine,
                msUntilNextLine = msUntil,
                progressToNextLine = 0f
            )
        }

        // At or after the last lyric line
        if (adjustedPosition >= lines.last().timestampMs) {
            return SyncState(
                activeIndex = lines.lastIndex,
                activeLine = lines.last(),
                nextLine = null,
                msUntilNextLine = null,
                progressToNextLine = 1f
            )
        }

        // Binary search to find the latest line where timestampMs <= adjustedPosition
        val binarySearchResult = lines.binarySearchBy(adjustedPosition) { it.timestampMs }

        val activeIndex = if (binarySearchResult >= 0) {
            // Exact timestamp match
            binarySearchResult
        } else {
            // binarySearchResult = -(insertion_point) - 1
            // insertion_point is the index of first element > adjustedPosition
            val insertionPoint = -(binarySearchResult + 1)
            (insertionPoint - 1).coerceAtLeast(0)
        }

        val activeLine = lines[activeIndex]
        val nextIndex = activeIndex + 1
        val nextLine = if (nextIndex < lines.size) lines[nextIndex] else null

        val msUntilNext = nextLine?.let {
            (it.timestampMs - adjustedPosition).coerceAtLeast(0L)
        }

        val progress = if (nextLine != null) {
            val segmentDuration = (nextLine.timestampMs - activeLine.timestampMs).coerceAtLeast(1L)
            val elapsedInSegment = (adjustedPosition - activeLine.timestampMs).coerceAtLeast(0L)
            (elapsedInSegment.toFloat() / segmentDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }

        return SyncState(
            activeIndex = activeIndex,
            activeLine = activeLine,
            nextLine = nextLine,
            msUntilNextLine = msUntilNext,
            progressToNextLine = progress
        )
    }
}

