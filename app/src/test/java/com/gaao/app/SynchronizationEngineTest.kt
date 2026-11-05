package com.gaao.app

import com.gaao.app.data.model.LyricLine
import com.gaao.app.domain.SynchronizationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SynchronizationEngineTest {

    private val sampleLines = listOf(
        LyricLine(timestampMs = 5000L, text = "Line 1 - intro ends"),
        LyricLine(timestampMs = 15000L, text = "Line 2 - first verse"),
        LyricLine(timestampMs = 30000L, text = "Line 3 - chorus starts"),
        LyricLine(timestampMs = 50000L, text = "Line 4 - outro")
    )

    @Test
    fun testEmptyLines() {
        val state = SynchronizationEngine.findActiveLine(emptyList(), 10000L)
        assertEquals(-1, state.activeIndex)
        assertNull(state.activeLine)
        assertNull(state.nextLine)
    }

    @Test
    fun testBeforeFirstLyric() {
        // Position at 2.5s (before first line at 5.0s)
        val state = SynchronizationEngine.findActiveLine(sampleLines, 2500L)
        assertEquals(-1, state.activeIndex)
        assertNull(state.activeLine)
        assertEquals("Line 1 - intro ends", state.nextLine?.text)
        assertEquals(2500L, state.msUntilNextLine)
    }

    @Test
    fun testExactTimestamp() {
        // Exactly at 5.0s
        val state = SynchronizationEngine.findActiveLine(sampleLines, 5000L)
        assertEquals(0, state.activeIndex)
        assertEquals("Line 1 - intro ends", state.activeLine?.text)
        assertEquals("Line 2 - first verse", state.nextLine?.text)
        assertEquals(10000L, state.msUntilNextLine)
    }

    @Test
    fun testBetweenLyrics() {
        // At 20.0s (between 15.0s and 30.0s)
        val state = SynchronizationEngine.findActiveLine(sampleLines, 20000L)
        assertEquals(1, state.activeIndex)
        assertEquals("Line 2 - first verse", state.activeLine?.text)
        assertEquals("Line 3 - chorus starts", state.nextLine?.text)
        assertEquals(10000L, state.msUntilNextLine)
        // 5s elapsed out of 15s duration -> 1/3 progress
        assertEquals(5000f / 15000f, state.progressToNextLine, 0.01f)
    }

    @Test
    fun testAtOrAfterLastLyric() {
        // Exactly at last line
        val stateAtEnd = SynchronizationEngine.findActiveLine(sampleLines, 50000L)
        assertEquals(3, stateAtEnd.activeIndex)
        assertEquals("Line 4 - outro", stateAtEnd.activeLine?.text)
        assertNull(stateAtEnd.nextLine)

        // Well past the last line
        val statePastEnd = SynchronizationEngine.findActiveLine(sampleLines, 75000L)
        assertEquals(3, statePastEnd.activeIndex)
        assertEquals("Line 4 - outro", statePastEnd.activeLine?.text)
        assertNull(statePastEnd.nextLine)
    }

    @Test
    fun testSeekingSimulation() {
        // Jump to middle
        val seekMid = SynchronizationEngine.findActiveLine(sampleLines, 35000L)
        assertEquals(2, seekMid.activeIndex)
        assertEquals("Line 3 - chorus starts", seekMid.activeLine?.text)

        // Seek backwards to start
        val seekBack = SynchronizationEngine.findActiveLine(sampleLines, 1000L)
        assertEquals(-1, seekBack.activeIndex)
        assertNull(seekBack.activeLine)
    }

    @Test
    fun testOffsetAdjustment() {
        // At 4.5s without offset -> before first line (5.0s)
        val noOffset = SynchronizationEngine.findActiveLine(sampleLines, 4500L, offsetMs = 0L)
        assertEquals(-1, noOffset.activeIndex)

        // At 4.5s with +600ms lead offset -> evaluated at 5.1s -> line 0 active
        val withLeadOffset = SynchronizationEngine.findActiveLine(sampleLines, 4500L, offsetMs = 600L)
        assertEquals(0, withLeadOffset.activeIndex)
    }
}

