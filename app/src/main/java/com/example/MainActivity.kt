package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.ui.PulseDeckApp
import com.example.ui.theme.PulseDeckTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()

            PulseDeckTheme(darkTheme = darkTheme) {
                PulseDeckApp(viewModel = viewModel)
            }
        }
    }
}
