package com.example.pitchsense.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pitchsense.ui.theme.Dimensions

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
                .padding(Dimensions.spacingMedium)
        ) {
            Spacer(modifier = Modifier.height(Dimensions.spacingSmall))

            // Standard back affordance for detail flows.
            OutlinedButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
                Spacer(modifier = Modifier.width(Dimensions.spacingSmall))
                Text("Back to Overview", fontSize = Dimensions.bodyFontSize)
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            // Screen-specific body.
            content()
        }
    }
}
