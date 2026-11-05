package com.gaao.app

import com.gaao.app.data.lyrics.LrcParser
import com.gaao.app.data.lyrics.LrclibResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun testStandardTwoDigitTimestamp() {
        val lrc = "[00:15.30] Hello world"
        val lines = LrcParser.parse(lrc)
        assertEquals(1, lines.size)
        assertEquals(15300L, lines[0].timestampMs)
        assertEquals("Hello world", lines[0].text)
    }

    @Test
    fun testThreeDigitTimestamp() {
        val lrc = "[01:02.500] Second line"
        val lines = LrcParser.parse(lrc)
        assertEquals(1, lines.size)
        assertEquals(62500L, lines[0].timestampMs)
        assertEquals("Second line", lines[0].text)
    }

    @Test
    fun testOneDigitTimestamp() {
        val lrc = "[00:05.5] Third line"
        val lines = LrcParser.parse(lrc)
        assertEquals(1, lines.size)
        assertEquals(5500L, lines[0].timestampMs)
        assertEquals("Third line", lines[0].text)
    }

    @Test
    fun testMultipleTimestampsOnSingleLine() {
        val lrc = "[00:10.00][00:20.00] Repeated chorus"
        val lines = LrcParser.parse(lrc)
        assertEquals(2, lines.size)
        assertEquals(10000L, lines[0].timestampMs)
        assertEquals("Repeated chorus", lines[0].text)
        assertEquals(20000L, lines[1].timestampMs)
        assertEquals("Repeated chorus", lines[1].text)
    }

    @Test
    fun testChronologicalSorting() {
        val lrc = """
            [01:00.00] Later line
            [00:10.00] Earlier line
        """.trimIndent()
        val lines = LrcParser.parse(lrc)
        assertEquals(2, lines.size)
        assertEquals("Earlier line", lines[0].text)
        assertEquals("Later line", lines[1].text)
    }

    @Test
    fun testIgnoresMetadataTagsAndEmptyLines() {
        val lrc = """
            [ti:Test Song]
            [ar:Test Artist]
            [al:Test Album]
            
            [00:05.00] First real line
            [offset:0]
        """.trimIndent()
        val lines = LrcParser.parse(lrc)
        assertEquals(1, lines.size)
        assertEquals("First real line", lines[0].text)
        assertEquals(5000L, lines[0].timestampMs)
    }

    @Test
    fun testMalformedAndEmptyInput() {
        assertTrue(LrcParser.parse(null).isEmpty())
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("   \n  \n  ").isEmpty())
        assertTrue(LrcParser.parse("Just plain text with no timestamp").isEmpty())
    }

    @Test
    fun testJsonDeserialization() {
        val jsonStr = """
            {
                "id": 12345,
                "trackName": "Bohemian Rhapsody",
                "artistName": "Queen",
                "albumName": "A Night at the Opera",
                "duration": 354.0,
                "instrumental": false,
                "plainLyrics": "Is this the real life?",
                "syncedLyrics": "[00:01.00] Is this the real life?\n[00:05.50] Is this just fantasy?"
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val response = json.decodeFromString<LrclibResponse>(jsonStr)

        assertEquals("Bohemian Rhapsody", response.trackName)
        assertEquals("Queen", response.artistName)
        assertEquals(354.0, response.duration ?: 0.0, 0.001)
        val lines = LrcParser.parse(response.syncedLyrics)
        assertEquals(2, lines.size)
        assertEquals(1000L, lines[0].timestampMs)
        assertEquals("Is this the real life?", lines[0].text)
        assertEquals(5500L, lines[1].timestampMs)
        assertEquals("Is this just fantasy?", lines[1].text)
    }
}

