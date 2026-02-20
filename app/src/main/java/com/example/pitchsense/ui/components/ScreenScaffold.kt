package com.example.pitchsense.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A reusable layout wrapper for detail screens (Advanced Stats, Heat Maps, etc.).
 * * - @param title: Passed to the HeaderBar to identify the current screen.
 * - @param onBackClick: A lambda function triggered by the back button,
 * typically calling 'navController.popBackStack()'.
 * - @param content: A 'Slot' (Composables-as-parameters) that allows different
 * screens to inject their specific UI into this common frame.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBackClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Prevents the bottom of the screen from being hidden by the
            // system navigation bar (gestures or buttons).
            .navigationBarsPadding()
    ) {
        // Enforces a consistent header across the entire application.
        HeaderBar(title = title)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Standardized back navigation button for consistent UX.
            OutlinedButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Overview")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Injects the screen-specific UI content here.
            content()
        }
    }
}