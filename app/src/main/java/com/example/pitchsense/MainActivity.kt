package com.example.pitchsense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.example.pitchsense.ui.navigation.PitchSenseApp

/**
 * MainActivity is the host for the entire application.
 * In a Single Activity Architecture, this file stays small and delegates
 * all UI and navigation logic to Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * enableEdgeToEdge() allows the app to draw behind the system status bar
         * and navigation bar, providing a modern, immersive look.
         */
        enableEdgeToEdge()

        /**
         * setContent defines the root of the UI.
         * We wrap everything in MaterialTheme to ensure consistent colors,
         * typography, and shapes across all screens.
         */
        setContent {
            MaterialTheme {
                // PitchSenseApp is the entry point for our navigation graph.
                PitchSenseApp()
            }
        }
    }
}