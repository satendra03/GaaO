package com.gaao.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.gaao.app.presentation.GaaOScreen
import com.gaao.app.presentation.LyricsViewModel
import com.gaao.app.ui.theme.GaaOTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LyricsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[LyricsViewModel::class.java]

        setContent {
            GaaOTheme {
                GaaOScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check active sessions and permission when returning to app (SRS 7.3, TDD Section 5)
        viewModel.checkPermissionAndRefresh()
    }
}
