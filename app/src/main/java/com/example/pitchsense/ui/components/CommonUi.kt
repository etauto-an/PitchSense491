package com.example.pitchsense.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pitchsense.ui.theme.Dimensions
import androidx.compose.ui.text.TextStyle

/**
 * The primary branding element for each screen.
 * - Uses 'statusBarsPadding' to ensure the background color extends behind the
 * system clock and notification icons.
 * - Centralizes the app's signature navy blue color (0xFF233B90).
 */
@Composable
fun HeaderBar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF233B90))
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Text(text = title, color = Color.White, fontSize = Dimensions.titleFontSize)
    }
}

/**
 * A specialized card for displaying individual baseball metrics (e.g., BA, SLG).
 * - @param isPrimary: Determines the visual hierarchy. Primary cards use a soft
 * blue background to draw the eye to the most important data points.
 * - @param modifier: Allows calling screens to pass layout constraints like
 * 'weight' or 'padding'.
 */
@Composable
fun StatCard(title: String, value: String, isPrimary: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) Color(0xFFF0F6FF) else Color.White
        ),
        border = if (isPrimary) null else BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Displays the label (e.g., "K%") in a subtle gray
            Text(text = title, fontSize = Dimensions.labelFontSize, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            // Displays the actual numerical value in a prominent font
            Text(text = value, fontSize = Dimensions.bodyFontSize)
        }
    }
}

/**
 * A custom pill-shaped toggle button for switching between different metrics.
 * - Does not hold its own state; instead, it reports clicks to the parent screen
 * via 'onClick'.
 * - Changes colors dynamically based on the 'isSelected' boolean to provide
 * immediate visual feedback.
 */
@Composable
fun ToggleButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF233B90) else Color(0xFFF0F2F5))
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = Dimensions.bodyFontSize
        )
    }
}

/**
 * A key-value pair used to explain the HeatMap colors.
 * - Maps a specific Color box to a descriptive label (e.g., "Elite").
 * - Used primarily on the HeatMap screen to help users interpret performance zones.
 */
@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        // Representative color swatch
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Explanatory text
        Text(text = label, fontSize = Dimensions.bodyFontSize)
    }
}