package com.gaao.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaao.app.data.model.LyricLine

@Composable
fun LyricsDisplay(
    lines: List<LyricLine>,
    activeLineIndex: Int,
    isPaused: Boolean,
    onLineClick: ((LyricLine) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Smoothly center the active lyric line whenever activeLineIndex changes
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex in lines.indices) {
            listState.animateScrollToItem(
                index = activeLineIndex,
                scrollOffset = 0
            )
        } else if (activeLineIndex == -1 && lines.isNotEmpty()) {
            listState.animateScrollToItem(0, 0)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Vertical padding equals half the viewport height so the target item centers perfectly
        val halfHeight = maxHeight / 2 - 40.dp

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = halfHeight, bottom = halfHeight),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = lines,
                key = { index, line -> "${index}_${line.timestampMs}" }
            ) { index, line ->
                val isActive = index == activeLineIndex
                val isPast = index < activeLineIndex

                // Fast, snappy transitions (180ms) for tight vocal synchronization
                val targetColor = when {
                    isActive -> Color.White
                    isPast -> Color(0xFF555555)
                    else -> Color(0xFF7E7E7E)
                }

                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "textColor"
                )

                val targetAlpha = when {
                    isActive -> if (isPaused) 0.75f else 1.0f
                    isPast -> 0.45f
                    else -> 0.70f
                }

                val animatedAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(durationMillis = 180),
                    label = "textAlpha"
                )

                val targetScale = if (isActive) 1.05f else 0.98f
                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "textScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                        .scale(animatedScale)
                        .alpha(animatedAlpha)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onLineClick?.invoke(line)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = line.text.ifBlank { "♪" },
                        fontSize = if (isActive) 26.sp else 20.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = if (isActive) 34.sp else 28.sp,
                        color = animatedColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Top & Bottom subtle gradient fade masks for cinematic edge transition
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = 0f,
                        endY = 160f
                    )
                )
                .align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = 160f
                    )
                )
                .align(Alignment.BottomCenter)
        )
    }
}
