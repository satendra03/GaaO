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
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaao.app.presentation.components.KineticLyricScene
import com.gaao.app.presentation.components.LyricsDisplay
import com.gaao.app.presentation.components.StateMessage
import com.gaao.app.ui.theme.BebasNeueFamily
import com.gaao.app.ui.theme.PlayfairDisplayFamily
import java.util.Locale

@Composable
fun GaaOScreen(
    viewModel: LyricsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var isVerticalMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndRefresh()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.appState == GaaOAppState.PLAYING || uiState.appState == GaaOAppState.PAUSED) {
                // Primary Display: Creative Kinetic Scene or Vertical Scroll List
                if (isVerticalMode) {
                    LyricsDisplay(
                        lines = uiState.lyrics,
                        activeLineIndex = uiState.syncState.activeIndex,
                        isPaused = uiState.appState == GaaOAppState.PAUSED,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    KineticLyricScene(
                        currentLine = uiState.syncState.activeLine,
                        nextLine = uiState.syncState.nextLine,
                        currentPositionMs = uiState.currentPositionMs,
                        syncOffsetMs = uiState.syncOffsetMs,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Minimalist cinematic header overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarPadding + 10.dp, start = 20.dp, end = 16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.trackTitle ?: "",
                                fontFamily = BebasNeueFamily,
                                fontSize = 24.sp,
                                letterSpacing = 1.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.artist ?: "",
                                    fontFamily = PlayfairDisplayFamily,
                                    fontSize = 13.sp,
                                    color = Color(0xFFAAAAAA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (uiState.appState == GaaOAppState.PAUSED) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PAUSED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB74D)
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Layout mode toggle (Kinetic Scene vs Scroll List)
                            Box(
                                modifier = Modifier
                                    .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                    .clickable { isVerticalMode = !isVerticalMode }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isVerticalMode) "Poster" else "List",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCCCCCC)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Stats / Offset drawer toggle
                            Box(
                                modifier = Modifier
                                    .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                    .clickable { viewModel.toggleDiagnosticOverlay() }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (uiState.showDiagnosticOverlay) "Hide" else "Stats",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCCCCCC)
                                )
                            }
                        }
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

            // Diagnostic Telemetry & Timing Calibrator Overlay
            AnimatedVisibility(
                visible = uiState.showDiagnosticOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding + 64.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1E1E1E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Telemetry & Timing Calibration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Source: ${uiState.appDisplayName ?: "None"}  •  State: ${uiState.appState.name}",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Position: ${formatTime(uiState.currentPositionMs)} / ${formatTime(uiState.durationMs)}",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                        val activeLineText = uiState.syncState.activeLine?.text ?: "Intro"
                        Text(
                            text = "Active Line [${uiState.syncState.activeIndex + 1}/${uiState.lyrics.size}]: $activeLineText",
                            fontSize = 12.sp,
                            color = Color(0xFFA5D6A7),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live timing adjuster
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lead Offset: +${uiState.syncOffsetMs}ms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFB74D)
                            )
                            Row {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                                        .clickable { viewModel.adjustSyncOffset(-50L) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("-50ms", fontSize = 11.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                                        .clickable { viewModel.adjustSyncOffset(50L) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("+50ms", fontSize = 11.sp, color = Color.White)
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
