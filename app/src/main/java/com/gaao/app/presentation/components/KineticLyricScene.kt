package com.gaao.app.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaao.app.data.model.LyricLine
import com.gaao.app.ui.theme.BebasNeueFamily
import com.gaao.app.ui.theme.PlayfairDisplayFamily

private val COMMON_FILLER_WORDS = setOf(
    "a", "an", "the", "in", "on", "at", "by", "for", "with", "about", "into", "through",
    "to", "from", "up", "down", "over", "under", "then", "here", "there", "when", "where",
    "why", "how", "all", "any", "some", "no", "nor", "not", "only", "own", "so", "than",
    "too", "very", "can", "will", "just", "now", "i", "you", "he", "she", "it", "we",
    "they", "me", "him", "her", "us", "them", "my", "your", "his", "its", "our", "their",
    "and", "but", "if", "or", "as", "of", "is", "am", "are", "was", "were", "be", "been",
    "have", "has", "had", "do", "does", "did"
)

enum class WordState {
    UPCOMING,
    ACTIVE,
    SPOKEN
}

data class TimedWord(
    val word: String,
    val isHero: Boolean,
    val state: WordState,
    val originalIndex: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KineticLyricScene(
    currentLine: LyricLine?,
    nextLine: LyricLine?,
    currentPositionMs: Long,
    syncOffsetMs: Long = 200L,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF181818), Color.Black),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (currentLine != null) {
            AnimatedContent(
                targetState = currentLine,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.7f).toInt() },
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(450)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.7f).toInt() },
                                animationSpec = tween(450, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(450))
                },
                label = "kineticSceneTransition"
            ) { line ->
                KineticLineContent(
                    line = line,
                    nextLine = nextLine,
                    currentPositionMs = currentPositionMs,
                    syncOffsetMs = syncOffsetMs
                )
            }
        } else {
            // Instrumental intro scene
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "✦   ✦   ✦",
                    fontSize = 24.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LISTEN",
                    fontFamily = BebasNeueFamily,
                    fontSize = 64.sp,
                    letterSpacing = 6.sp,
                    color = Color(0xFFAAAAAA)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Music playing...",
                    fontFamily = PlayfairDisplayFamily,
                    fontSize = 20.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        // Upcoming lyric preview at the bottom
        if (nextLine != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 36.dp)
                    .alpha(0.40f)
            ) {
                Text(
                    text = nextLine.text,
                    fontFamily = PlayfairDisplayFamily,
                    fontSize = 17.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KineticLineContent(
    line: LyricLine,
    nextLine: LyricLine?,
    currentPositionMs: Long,
    syncOffsetMs: Long
) {
    val rawWords = line.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (rawWords.isEmpty()) return

    // Calculate word-by-word timing
    val lineStartMs = line.timestampMs
    val lineEndMs = nextLine?.timestampMs ?: (lineStartMs + 4000L)
    val lineDurationMs = (lineEndMs - lineStartMs).coerceAtLeast(1500L)
    val adjustedNowMs = (currentPositionMs + syncOffsetMs).coerceAtLeast(0L)
    val elapsedInLineMs = (adjustedNowMs - lineStartMs).coerceAtLeast(0L)

    // Assign relative weight based on character length
    val totalWeight = rawWords.sumOf { it.length.coerceAtLeast(2) }
    var runningWeight = 0

    // Pick the hero word: longest non-filler word, or simply longest word
    val heroIndex = findHeroWordIndex(rawWords)

    val timedWords = rawWords.mapIndexed { index, word ->
        val weight = word.length.coerceAtLeast(2)
        val wordStartMs = (runningWeight.toDouble() / totalWeight * lineDurationMs).toLong()
        runningWeight += weight
        val wordEndMs = (runningWeight.toDouble() / totalWeight * lineDurationMs).toLong()

        val state = when {
            elapsedInLineMs < wordStartMs -> WordState.UPCOMING
            elapsedInLineMs in wordStartMs until wordEndMs -> WordState.ACTIVE
            else -> WordState.SPOKEN
        }

        TimedWord(
            word = word,
            isHero = index == heroIndex,
            state = state,
            originalIndex = index
        )
    }

    val prefixWords = timedWords.subList(0, heroIndex)
    val heroWord = timedWords[heroIndex]
    val suffixWords = if (heroIndex + 1 < timedWords.size) {
        timedWords.subList(heroIndex + 1, timedWords.size)
    } else {
        emptyList()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Row 1: Prefix words (e.g. "we were too close")
        if (prefixWords.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                prefixWords.forEach { item ->
                    KineticWord(
                        timedWord = item,
                        fontFamily = PlayfairDisplayFamily,
                        baseFontSize = 26.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Centerpiece: HERO Word (e.g. "STARS") with depth and scale
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            KineticHeroWord(timedWord = heroWord)
        }

        // Row 3: Suffix words (e.g. "to the")
        if (suffixWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                suffixWords.forEach { item ->
                    KineticWord(
                        timedWord = item,
                        fontFamily = PlayfairDisplayFamily,
                        baseFontSize = 24.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun KineticHeroWord(timedWord: TimedWord) {
    val isActive = timedWord.state == WordState.ACTIVE
    val isSpoken = timedWord.state == WordState.SPOKEN

    val targetScale = if (isActive) 1.15f else 1.0f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "heroScale"
    )

    val targetAlpha = when (timedWord.state) {
        WordState.UPCOMING -> 0.22f
        WordState.ACTIVE -> 1.0f
        WordState.SPOKEN -> 0.95f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 180),
        label = "heroAlpha"
    )

    val textShadow = if (isActive) {
        Shadow(
            color = Color.White.copy(alpha = 0.7f),
            offset = Offset(0f, 0f),
            blurRadius = 32f
        )
    } else if (isSpoken) {
        Shadow(
            color = Color(0x66FFFFFF),
            offset = Offset(2f, 4f),
            blurRadius = 14f
        )
    } else {
        Shadow(
            color = Color.Transparent,
            offset = Offset.Zero,
            blurRadius = 0f
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .scale(animatedScale)
            .alpha(animatedAlpha)
    ) {
        if (isActive) {
            Text(
                text = "✦ ",
                fontSize = 24.sp,
                color = Color.White
            )
        }

        Text(
            text = timedWord.word.uppercase(),
            fontFamily = BebasNeueFamily,
            fontSize = 72.sp,
            letterSpacing = 4.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color.White else if (isSpoken) Color(0xFFE0E0E0) else Color(0xFF666666),
            style = TextStyle(shadow = textShadow),
            textAlign = TextAlign.Center
        )

        if (isActive) {
            Text(
                text = " ✧",
                fontSize = 24.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun KineticWord(
    timedWord: TimedWord,
    fontFamily: FontFamily,
    baseFontSize: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit
) {
    val isActive = timedWord.state == WordState.ACTIVE
    val isSpoken = timedWord.state == WordState.SPOKEN

    val targetScale = if (isActive) 1.18f else 1.0f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "wordScale"
    )

    val targetAlpha = when (timedWord.state) {
        WordState.UPCOMING -> 0.20f
        WordState.ACTIVE -> 1.0f
        WordState.SPOKEN -> 0.90f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 160),
        label = "wordAlpha"
    )

    val textColor = when (timedWord.state) {
        WordState.ACTIVE -> Color.White
        WordState.SPOKEN -> Color(0xFFDDDDDD)
        WordState.UPCOMING -> Color(0xFF555555)
    }

    val shadow = if (isActive) {
        Shadow(color = Color.White.copy(alpha = 0.6f), offset = Offset(0f, 0f), blurRadius = 16f)
    } else {
        null
    }

    Text(
        text = "${timedWord.word} ",
        fontFamily = fontFamily,
        fontSize = baseFontSize,
        letterSpacing = letterSpacing,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        color = textColor,
        style = TextStyle(shadow = shadow ?: Shadow.None),
        modifier = Modifier
            .padding(horizontal = 3.dp, vertical = 2.dp)
            .scale(animatedScale)
            .alpha(animatedAlpha)
    )
}

private fun findHeroWordIndex(words: List<String>): Int {
    if (words.size <= 2) {
        // If 1 or 2 words, the last word or longest word is hero
        return words.indices.maxByOrNull { words[it].length } ?: 0
    }

    // Clean punctuation
    val cleaned = words.map { it.replace(Regex("[^a-zA-Z0-9]"), "").lowercase() }

    // Prefer non-filler words
    val candidates = cleaned.mapIndexedNotNull { index, clean ->
        if (clean.length >= 3 && clean !in COMMON_FILLER_WORDS) {
            index to clean.length
        } else {
            null
        }
    }

    return if (candidates.isNotEmpty()) {
        // Return the longest candidate content word
        candidates.maxByOrNull { it.second }?.first ?: (words.size / 2)
    } else {
        // Fallback: longest word overall
        words.indices.maxByOrNull { words[it].length } ?: (words.size / 2)
    }
}
