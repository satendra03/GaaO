package com.gaao.app.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaao.app.data.model.LyricLine
import com.gaao.app.ui.theme.getMainLyricsFont
import com.gaao.app.ui.theme.isHindiOrDevanagari

// =========================================================================
// 🎛️ TWEAK YOUR LYRICS FONT SIZES & SPACING HERE DIRECTLY
// =========================================================================
object LyricsSizeConfig {
    // English font sizes
    val ENGLISH_HERO_SIZE = 170.sp          // Size of the BIG focal word (e.g. "KNOW,", "WANNA", "DID")
    val ENGLISH_HERO_LINE_HEIGHT = 90.sp
    val ENGLISH_NORMAL_SIZE = 90.sp        // Size of supporting words (e.g. "I SHOULD", "AND I DON'T")
    val ENGLISH_NORMAL_LINE_HEIGHT = 20.sp

    // Hindi font sizes
    val HINDI_HERO_SIZE = 120.sp            // Size of the BIG focal Hindi word
    val HINDI_HERO_LINE_HEIGHT = 10.sp
    val HINDI_NORMAL_SIZE = 90.sp          // Size of supporting Hindi words
    val HINDI_NORMAL_LINE_HEIGHT = 10.sp

    // Left margin distance from screen edge
    val LEFT_PADDING = 36.dp
}
// =========================================================================

private val COMMON_FILLER_WORDS = setOf(
    "a", "an", "the", "in", "on", "at", "by", "for", "with", "about", "into", "through",
    "to", "from", "up", "down", "over", "under", "then", "here", "there", "when", "where",
    "why", "how", "all", "any", "some", "no", "nor", "not", "only", "own", "so", "than",
    "too", "very", "can", "will", "just", "now", "i", "you", "he", "she", "it", "we",
    "they", "me", "him", "her", "us", "them", "my", "your", "his", "its", "our", "their",
    "and", "but", "if", "or", "as", "of", "is", "am", "are", "was", "were", "be", "been",
    "have", "has", "had", "do", "does", "did",
    "का", "के", "की", "को", "में", "से", "पर", "ने", "है", "हैं", "था", "थी", "थे",
    "तो", "भी", "ही", "और", "या", "कि", "जो", "ये", "वो", "हम", "तुम", "मैं", "आप"
)

data class PosterWord(
    val text: String,
    val revealTimeMs: Long
)

data class PosterLineGroup(
    val words: List<PosterWord>,
    val isHero: Boolean
)

@Composable
fun PosterLyricScene(
    currentLine: LyricLine?,
    nextLine: LyricLine?,
    currentPositionMs: Long,
    syncOffsetMs: Long = 400L,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.CenterStart
    ) {
        if (currentLine != null) {
            AnimatedContent(
                targetState = currentLine,
                transitionSpec = {
                    (slideInVertically(
                        initialOffsetY = { fullHeight -> (fullHeight * 0.10f).toInt() },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(280))) togetherWith
                            (slideOutVertically(
                                targetOffsetY = { fullHeight -> (-fullHeight * 0.10f).toInt() },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(220)))
                },
                label = "posterSceneTransition"
            ) { line ->
                PosterSceneContent(
                    line = line,
                    nextLine = nextLine,
                    currentPositionMs = currentPositionMs,
                    syncOffsetMs = syncOffsetMs
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PosterSceneContent(
    line: LyricLine,
    nextLine: LyricLine?,
    currentPositionMs: Long,
    syncOffsetMs: Long
) {
    val rawWords = line.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (rawWords.isEmpty()) return

    val isHindi = isHindiOrDevanagari(line.text)
    val lyricsFont = getMainLyricsFont(line.text)

    // Calculate word-by-word reveal timing
    val lineStartMs = line.timestampMs
    val lineEndMs = nextLine?.timestampMs ?: (lineStartMs + 3800L)
    val lineDurationMs = (lineEndMs - lineStartMs).coerceAtLeast(1200L)
    val playheadMs = (currentPositionMs + syncOffsetMs).coerceAtLeast(0L)

    val totalWeight = rawWords.sumOf { it.length.coerceAtLeast(2) }
    var runningWeight = 0

    val timedWords = rawWords.map { word ->
        val weight = word.length.coerceAtLeast(2)
        val wordRevealTimeMs = lineStartMs + (runningWeight.toDouble() / totalWeight * lineDurationMs).toLong()
        runningWeight += weight
        PosterWord(
            text = if (isHindi) word else word.uppercase(),
            revealTimeMs = wordRevealTimeMs
        )
    }

    // Split words into left-aligned stacked lines (hero word vs supporting lines)
    val lineGroups = layoutPosterGroups(timedWords)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = LyricsSizeConfig.LEFT_PADDING, end = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        lineGroups.forEach { group ->
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.Center
            ) {
                group.words.forEach { word ->
                    val isRevealed = playheadMs >= word.revealTimeMs

                    AnimatedVisibility(
                        visible = isRevealed,
                        enter = fadeIn(animationSpec = tween(130)) +
                                scaleIn(initialScale = 0.88f, animationSpec = tween(130, easing = FastOutSlowInEasing)) +
                                slideInVertically(initialOffsetY = { 18 }, animationSpec = tween(130))
                    ) {
                        val fontSize = if (group.isHero) {
                            if (isHindi) LyricsSizeConfig.HINDI_HERO_SIZE else LyricsSizeConfig.ENGLISH_HERO_SIZE
                        } else {
                            if (isHindi) LyricsSizeConfig.HINDI_NORMAL_SIZE else LyricsSizeConfig.ENGLISH_NORMAL_SIZE
                        }

                        val lineHeight = if (group.isHero) {
                            if (isHindi) LyricsSizeConfig.HINDI_HERO_LINE_HEIGHT else LyricsSizeConfig.ENGLISH_HERO_LINE_HEIGHT
                        } else {
                            if (isHindi) LyricsSizeConfig.HINDI_NORMAL_LINE_HEIGHT else LyricsSizeConfig.ENGLISH_NORMAL_LINE_HEIGHT
                        }

                        Text(
                            text = "${word.text} ",
                            fontFamily = lyricsFont,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            letterSpacing = if (group.isHero) (-1).sp else 0.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(if (group.isHero) 2.dp else 4.dp))
        }
    }
}

private fun layoutPosterGroups(words: List<PosterWord>): List<PosterLineGroup> {
    if (words.size <= 2) {
        return if (words.size == 1) {
            listOf(PosterLineGroup(words = words, isHero = true))
        } else {
            listOf(
                PosterLineGroup(words = listOf(words[0]), isHero = false),
                PosterLineGroup(words = listOf(words[1]), isHero = true)
            )
        }
    }

    val heroIndex = findHeroIndex(words)
    val groups = mutableListOf<PosterLineGroup>()

    // Words before the hero word
    if (heroIndex > 0) {
        val prefix = words.subList(0, heroIndex)
        if (prefix.size <= 3) {
            groups.add(PosterLineGroup(words = prefix, isHero = false))
        } else {
            val mid = prefix.size / 2
            groups.add(PosterLineGroup(words = prefix.subList(0, mid), isHero = false))
            groups.add(PosterLineGroup(words = prefix.subList(mid, prefix.size), isHero = false))
        }
    }

    // Hero word (alone on its own line for punchy impact)
    groups.add(PosterLineGroup(words = listOf(words[heroIndex]), isHero = true))

    // Words after the hero word
    if (heroIndex + 1 < words.size) {
        val suffix = words.subList(heroIndex + 1, words.size)
        if (suffix.size <= 2) {
            groups.add(PosterLineGroup(words = suffix, isHero = false))
        } else {
            val mid = (suffix.size + 1) / 2
            groups.add(PosterLineGroup(words = suffix.subList(0, mid), isHero = false))
            groups.add(PosterLineGroup(words = suffix.subList(mid, suffix.size), isHero = false))
        }
    }

    return groups
}

private fun findHeroIndex(words: List<PosterWord>): Int {
    val cleaned = words.map { it.text.replace(Regex("[^\\p{L}\\p{Nd}]"), "").lowercase() }

    val contentCandidates = cleaned.mapIndexedNotNull { index, clean ->
        if (clean.length >= 3 && clean !in COMMON_FILLER_WORDS) {
            index to clean.length
        } else {
            null
        }
    }

    return if (contentCandidates.isNotEmpty()) {
        contentCandidates.maxByOrNull { it.second }?.first ?: (words.size / 2)
    } else {
        words.indices.maxByOrNull { words[it].text.length } ?: (words.size / 2)
    }
}
