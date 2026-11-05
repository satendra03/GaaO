package com.gaao.app.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaao.app.presentation.components.PosterLyricScene
import com.gaao.app.presentation.components.StateMessage
import com.gaao.app.ui.theme.EnglishSecondary
import java.util.Locale

@Composable
fun GaaOScreen(
    viewModel: LyricsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndRefresh()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        // Enforce safe drawing bounds around the entire screen so status bar,
        // camera hole-punches, notches, and navigation bars never overlap content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(safeInsets)
        ) {
            if (uiState.appState == GaaOAppState.PLAYING || uiState.appState == GaaOAppState.PAUSED) {
                // Pure Poster Scene (Left-aligned, vertically centered, word-by-word reveal)
                PosterLyricScene(
                    currentLine = uiState.syncState.activeLine,
                    nextLine = uiState.syncState.nextLine,
                    currentPositionMs = uiState.currentPositionMs,
                    syncOffsetMs = uiState.syncOffsetMs,
                    modifier = Modifier.fillMaxSize()
                )

                // Secondary Header (Times New Roman)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val trackText = uiState.trackTitle ?: ""

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trackText,
                            fontFamily = EnglishSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.artist ?: "",
                                fontFamily = EnglishSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFFAAAAAA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (uiState.appState == GaaOAppState.PAUSED) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF262626), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PAUSED",
                                        fontFamily = EnglishSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB74D)
                                    )
                                }
                            }
                        }
                    }

                    // Discreet toggle for telemetry / sync offset
                    Box(
                        modifier = Modifier
                            .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleDiagnosticOverlay() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (uiState.showDiagnosticOverlay) "Hide" else "Stats",
                            fontFamily = EnglishSecondary,
                            fontSize = 12.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }
            } else {
                StateMessage(
                    uiState = uiState,
                    onGrantPermission = { viewModel.openNotificationSettings(context) },
                    onRetryLyrics = { viewModel.retryLyrics() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Diagnostic Telemetry & Timing Calibration Overlay
            AnimatedVisibility(
                visible = uiState.showDiagnosticOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, start = 20.dp, end = 20.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1A1A1A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Telemetry & Timing Calibration",
                            fontFamily = EnglishSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "App: ${uiState.appDisplayName ?: "None"}  •  State: ${uiState.appState.name}",
                            fontFamily = EnglishSecondary,
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Position: ${formatTime(uiState.currentPositionMs)} / ${formatTime(uiState.durationMs)}",
                            fontFamily = EnglishSecondary,
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        val activeLineText = uiState.syncState.activeLine?.text ?: "Intro"
                        Text(
                            text = "Active Line [${uiState.syncState.activeIndex + 1}/${uiState.lyrics.size}]: $activeLineText",
                            fontFamily = EnglishSecondary,
                            fontSize = 13.sp,
                            color = Color(0xFFA5D6A7),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Live timing offset calibrator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lead Offset: +${uiState.syncOffsetMs}ms",
                                fontFamily = EnglishSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D)
                            )
                            Row {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                                        .clickable { viewModel.adjustSyncOffset(-50L) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("-50ms", fontFamily = EnglishSecondary, fontSize = 12.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                                        .clickable { viewModel.adjustSyncOffset(50L) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("+50ms", fontFamily = EnglishSecondary, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
