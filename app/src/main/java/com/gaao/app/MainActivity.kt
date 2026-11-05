package com.gaao.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.gaao.app.presentation.DiagnosticScreen
import com.gaao.app.presentation.DiagnosticViewModel
import com.gaao.app.ui.theme.GaaOTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DiagnosticViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[DiagnosticViewModel::class.java]

        setContent {
            GaaOTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DiagnosticScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check active sessions and permission when returning to app (SRS 7.3, TDD Section 5)
        viewModel.checkPermissionAndRefresh()
    }
}
