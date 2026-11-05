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
import com.gaao.app.ui.theme.EnglishFamily
import com.gaao.app.ui.theme.EnglishSecondary

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
                        text = "NOTIFICATION ACCESS",
                        fontFamily = EnglishFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "GaaO requires notification access to identify playing music from Spotify & YouTube Music.",
                        fontFamily = EnglishSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFAAAAAA),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onGrantPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Grant Access", fontFamily = EnglishSecondary, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                GaaOAppState.IDLE -> {
                    Text(
                        text = "GAAO",
                        fontFamily = EnglishFamily,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Waiting for music...",
                        fontFamily = EnglishSecondary,
                        fontSize = 17.sp,
                        color = Color(0xFF6E6E6E),
                        textAlign = TextAlign.Center
                    )
                }

                GaaOAppState.LOADING_LYRICS -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "FINDING LYRICS...",
                        fontFamily = EnglishFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (!uiState.trackTitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.trackTitle}  •  ${uiState.artist ?: ""}",
                            fontFamily = EnglishSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                GaaOAppState.OFFLINE -> {
                    Text(
                        text = "NO INTERNET",
                        fontFamily = EnglishFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect to mobile data or Wi-Fi to load lyrics.",
                        fontFamily = EnglishSecondary,
                        fontSize = 15.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRetryLyrics,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Retry", fontFamily = EnglishSecondary, fontSize = 14.sp, color = Color.White)
                    }
                }

                GaaOAppState.LYRICS_UNAVAILABLE -> {
                    Text(
                        text = "LYRICS UNAVAILABLE",
                        fontFamily = EnglishFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (!uiState.trackTitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.trackTitle}  •  ${uiState.artist ?: ""}",
                            fontFamily = EnglishSecondary,
                            fontSize = 15.sp,
                            color = Color(0xFF777777),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                GaaOAppState.ERROR -> {
                    Text(
                        text = uiState.errorMessage ?: "An error occurred",
                        fontFamily = EnglishSecondary,
                        fontSize = 16.sp,
                        color = Color(0xFFFF5252),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetryLyrics,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Retry", fontFamily = EnglishSecondary, fontSize = 14.sp, color = Color.White)
                    }
                }

                else -> {}
            }
        }
    }
}
