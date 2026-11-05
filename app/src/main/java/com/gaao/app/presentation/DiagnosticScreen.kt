package com.gaao.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaao.app.data.model.LyricsResult
import com.gaao.app.data.model.PlaybackState
import java.util.Locale

@Composable
fun DiagnosticScreen(
    viewModel: DiagnosticViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isConnected by viewModel.isListenerConnected.collectAsState()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val playbackInfo by viewModel.playbackInfo.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentPositionMs by viewModel.estimatedPositionMs.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()

    // Refresh on composition
    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndRefresh()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "GaaO Media Detector",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Phase 2 Prototype (Detection + LRCLIB Lyrics)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Listener & Permission status card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DiagnosticRow(
                        label = "Listener Status",
                        value = if (isConnected) "Connected" else "Disconnected",
                        valueColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DiagnosticRow(
                        label = "Notification Access",
                        value = if (isPermissionGranted) "Granted" else "Not Granted",
                        valueColor = if (isPermissionGranted) Color(0xFF4CAF50) else Color(0xFFFFAB00)
                    )

                    if (!isPermissionGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.openNotificationSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Notification Settings", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DiagnosticRow(
                        label = "Status",
                        value = statusMessage,
                        valueColor = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback information card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Session Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0xFF2C2C2C)
                    )

                    DiagnosticRow(
                        label = "Music Application",
                        value = playbackInfo?.appDisplayName ?: "None detected",
                        valueColor = if (playbackInfo != null) Color(0xFF64B5F6) else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        label = "Package Name",
                        value = playbackInfo?.packageName ?: "N/A"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        label = "Song",
                        value = playbackInfo?.trackTitle ?: "N/A",
                        valueColor = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        label = "Artist",
                        value = playbackInfo?.artist ?: "N/A",
                        valueColor = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        label = "Album",
                        value = playbackInfo?.album ?: "N/A"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        label = "Playback",
                        value = playbackInfo?.playbackState?.name ?: PlaybackState.NONE.name,
                        valueColor = when (playbackInfo?.playbackState) {
                            PlaybackState.PLAYING -> Color(0xFF4CAF50)
                            PlaybackState.PAUSED -> Color(0xFFFFB74D)
                            PlaybackState.BUFFERING -> Color(0xFF64B5F6)
                            else -> Color.Gray
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val formattedPosition = formatTime(currentPositionMs)
                    val formattedDuration = formatTime(playbackInfo?.durationMs ?: 0L)

                    DiagnosticRow(
                        label = "Position",
                        value = "$formattedPosition / $formattedDuration",
                        valueColor = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        label = "Speed",
                        value = "${playbackInfo?.playbackSpeed ?: 1.0f}x"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lyrics status card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lyrics Integration (LRCLIB)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0xFF2C2C2C)
                    )

                    when (val result = lyricsState) {
                        is LyricsResult.Idle -> {
                            DiagnosticRow(
                                label = "Status",
                                value = "Idle (waiting for music)",
                                valueColor = Color.Gray
                            )
                        }
                        is LyricsResult.Loading -> {
                            DiagnosticRow(
                                label = "Status",
                                value = "Finding lyrics...",
                                valueColor = Color(0xFF64B5F6)
                            )
                        }
                        is LyricsResult.Unavailable -> {
                            DiagnosticRow(
                                label = "Status",
                                value = "Lyrics unavailable for this track",
                                valueColor = Color(0xFFFFB74D)
                            )
                        }
                        is LyricsResult.Offline -> {
                            DiagnosticRow(
                                label = "Status",
                                value = "No internet connection",
                                valueColor = Color(0xFFFF5252)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.retryLyrics() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retry Lyrics", color = Color.White)
                            }
                        }
                        is LyricsResult.Error -> {
                            DiagnosticRow(
                                label = "Status",
                                value = result.message,
                                valueColor = Color(0xFFFF5252)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.retryLyrics() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retry Lyrics", color = Color.White)
                            }
                        }
                        is LyricsResult.Success -> {
                            DiagnosticRow(
                                label = "Status",
                                value = "Synced Lyrics Loaded",
                                valueColor = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            DiagnosticRow(
                                label = "Total Lines",
                                value = "${result.lines.size} lines",
                                valueColor = Color.White
                            )

                            if (result.lines.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Preview (First 4 Lines):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                result.lines.take(4).forEach { line ->
                                    Text(
                                        text = "${formatTime(line.timestampMs)}  ${line.text}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFFDDDDDD),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.checkPermissionAndRefresh() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Active Sessions", color = Color.White)
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFFCCCCCC)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888),
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            fontWeight = FontWeight.Medium,
            color = valueColor,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
