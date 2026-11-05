package com.gaao.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaao.app.presentation.GaaOAppState
import com.gaao.app.presentation.GaaOUiState

@Composable
fun StateMessage(
    uiState: GaaOUiState,
    onGrantPermission: () -> Unit,
    onRetryLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = uiState.appState != GaaOAppState.PLAYING && uiState.appState != GaaOAppState.PAUSED,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState.appState) {
                GaaOAppState.PERMISSION_REQUIRED -> {
                    Text(
                        text = "Notification Access Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "GaaO needs notification listener permission to detect current song metadata from Spotify and YouTube Music.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFAAAAAA),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onGrantPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }

                GaaOAppState.IDLE -> {
                    Text(
                        text = "GaaO",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Waiting for music...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF6E6E6E),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Play a song in Spotify or YouTube Music",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF444444),
                        textAlign = TextAlign.Center
                    )
                }

                GaaOAppState.LOADING_LYRICS -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Finding lyrics...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (!uiState.trackTitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.trackTitle} • ${uiState.artist ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                GaaOAppState.OFFLINE -> {
                    Text(
                        text = "No internet connection.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect to Wi-Fi or mobile data to load lyrics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRetryLyrics,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }

                GaaOAppState.LYRICS_UNAVAILABLE -> {
                    Text(
                        text = "Lyrics unavailable for this track.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (!uiState.trackTitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.trackTitle} • ${uiState.artist ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF777777),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                GaaOAppState.ERROR -> {
                    Text(
                        text = uiState.errorMessage ?: "An error occurred",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFFF5252),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetryLyrics,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }

                else -> {}
            }
        }
    }
}

