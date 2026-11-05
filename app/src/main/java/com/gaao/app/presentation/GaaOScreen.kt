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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaao.app.presentation.components.LyricsDisplay
import com.gaao.app.presentation.components.StateMessage
import java.util.Locale

@Composable
fun GaaOScreen(
    viewModel: LyricsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndRefresh()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main lyrics or state view
            if (uiState.appState == GaaOAppState.PLAYING || uiState.appState == GaaOAppState.PAUSED) {
                LyricsDisplay(
                    lines = uiState.lyrics,
                    activeLineIndex = uiState.syncState.activeIndex,
                    isPaused = uiState.appState == GaaOAppState.PAUSED,
                    modifier = Modifier.fillMaxSize()
                )

                // Minimalist cinematic header overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarPadding + 8.dp, start = 24.dp, end = 16.dp)
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.artist ?: "",
                                    style = MaterialTheme.typography.bodySmall,
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
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB74D)
                                        )
                                    }
                                }
                            }
                        }

                        // Discreet toggle for telemetry / diagnostic view
                        Box(
                            modifier = Modifier
                                .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleDiagnosticOverlay() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (uiState.showDiagnosticOverlay) "Hide Stats" else "Stats",
                                fontSize = 11.sp,
                                color = Color(0xFFDDDDDD)
                            )
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

            // Diagnostic Telemetry Overlay (with real-time offset fine-tuning)
            AnimatedVisibility(
                visible = uiState.showDiagnosticOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding + 60.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1E1E1E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Telemetry & Sync Engine",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "App: ${uiState.appDisplayName ?: "None"}  •  Status: ${uiState.appState.name}",
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
                        val nextIn = uiState.syncState.msUntilNextLine?.let { "${String.format(Locale.US, "%.1fs", it / 1000.0)}" } ?: "End"
                        Text(
                            text = "Next Lyric In: $nextIn",
                            fontSize = 12.sp,
                            color = Color(0xFF64B5F6)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-time sync offset calibrator
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
