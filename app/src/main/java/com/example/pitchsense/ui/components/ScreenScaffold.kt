package com.example.pitchsense.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared detail-screen scaffold with app header, optional offline banner, back action,
 * and content slot.
 *
 * Keeps spacing/navigation affordances consistent across non-overview screens.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBackClick: () -> Unit,
    isOffline: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Shared top branding/title bar.
        HeaderBar(title = title)
        // Amber banner shown whenever data is sourced from local demo fallback.
        if (isOffline) OfflineBanner()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Standard back affordance for detail flows.
            OutlinedButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Back to Overview", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Screen-specific body.
            content()
        }
    }
}
