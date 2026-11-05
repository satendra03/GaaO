package com.gaao.app.data.lyrics

import com.gaao.app.data.model.LyricLine
import java.util.regex.Pattern

object LrcParser {

    private val TIMESTAMP_PATTERN: Pattern = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")
    private val ID_TAG_PATTERN: Pattern = Pattern.compile("^\\[[a-zA-Z]+:.*\\]$")

    /**
     * Parses a raw LRC string into a sorted list of [LyricLine].
     * Safely handles empty input, metadata tags, multiple timestamps per line, and malformed lines.
     */
    fun parse(lrcContent: String?): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) {
            return emptyList()
        }

        val parsedLines = mutableListOf<LyricLine>()

        lrcContent.lineSequence().forEach { rawLine ->
            val trimmedLine = rawLine.trim()
            if (trimmedLine.isEmpty()) return@forEach

            // Ignore metadata tags like [ar:Singer], [ti:Song], etc.
            if (ID_TAG_PATTERN.matcher(trimmedLine).matches()) {
                return@forEach
            }

            val matcher = TIMESTAMP_PATTERN.matcher(trimmedLine)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                try {
                    val minutes = matcher.group(1)?.toLong() ?: 0L
                    val seconds = matcher.group(2)?.toLong() ?: 0L
                    val fractionStr = matcher.group(3)

                    val millis = when (fractionStr?.length) {
                        1 -> fractionStr.toLong() * 100L
                        2 -> fractionStr.toLong() * 10L
                        3 -> fractionStr.toLong()
                        else -> 0L
                    }

                    val totalMs = (minutes * 60L + seconds) * 1000L + millis
                    timestamps.add(totalMs)
                    lastMatchEnd = matcher.end()
                } catch (e: Exception) {
                    // Ignore malformed timestamp numbers safely
                }
            }

            if (timestamps.isNotEmpty()) {
                val text = if (lastMatchEnd <= trimmedLine.length) {
                    trimmedLine.substring(lastMatchEnd).trim()
                } else {
                    ""
                }

                for (ts in timestamps) {
                    parsedLines.add(LyricLine(timestampMs = ts, text = text))
                }
            }
        }

        return parsedLines.sortedBy { it.timestampMs }
    }
}
