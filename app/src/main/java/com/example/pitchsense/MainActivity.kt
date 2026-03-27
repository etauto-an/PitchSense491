package com.example.pitchsense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.pitchsense.ui.navigation.PitchSenseApp

/** Single-activity host that boots the Compose app and navigation graph. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                PitchSenseApp()
            }
        }
    }
}
