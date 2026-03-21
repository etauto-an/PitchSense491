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

/**
 * Amber banner shown below the header when the app is serving local demo data
 * instead of live API results. Collapses to nothing when online.
 */
@Composable
fun OfflineBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3CD))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Offline — showing demo data",
            color = Color(0xFF856404),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** Top app header used across screens for consistent branding/title treatment. */
@Composable
fun HeaderBar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF233B90))
            .padding(24.dp)
    ) {
        Text(text = title, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

/** Metric card used for compact stat display; highlights primary stats visually. */
@Composable
fun StatCard(title: String, value: String, isPrimary: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(108.dp),
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
            // Label (for example "K%" or "OBP").
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            // Emphasized metric value.
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** Stateless pill toggle used for metric/tab-style selection controls. */
@Composable
fun ToggleButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFF233B90) else Color(0xFFF0F2F5))
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Single heat-map legend row mapping a color swatch to its label. */
@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 10.dp)
    ) {
        // Color swatch for this performance bucket.
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(20.dp))
        // Human-readable bucket label.
        Text(text = label, style = MaterialTheme.typography.titleSmall)
    }
}
