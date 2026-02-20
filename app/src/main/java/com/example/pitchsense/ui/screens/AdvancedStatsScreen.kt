package com.example.pitchsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pitchsense.ui.components.ScreenScaffold

/**
 * Screen designed for in-depth statistical analysis of a batter.
 * - Utilizes 'ScreenScaffold' to inherit consistent padding and back-navigation.
 * - Implements 'verticalScroll' to prevent content clipping on smaller screens or
 * when the data list grows.
 */
@Composable
fun AdvancedStatsScreen(onBackClick: () -> Unit) {
    ScreenScaffold(title = "Advanced Batter Statistics", onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Summary Dashboard Card: Highlights elite metrics at a glance.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F6FF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Batter Statistics - Mike Trout", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    // First row of summary stats.
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        AdvancedStatItem("BA vs RHP", ".298")
                        AdvancedStatItem("BA vs LHP", ".265")
                        AdvancedStatItem("Hard Hit%", "45.2%")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Second row of summary stats.
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        AdvancedStatItem("Barrel%", "12.8%")
                        AdvancedStatItem("SLG", ".512")
                        AdvancedStatItem("OPS", ".874")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Detailed Breakdown Section: Performance categorized by pitch type.
            Text("Performance vs Pitch Type", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Custom Table Header: Uses 'Modifier.weight' to align with data rows below.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pitch", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("BA", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("SLG", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("Whiff", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()

            // Dynamic Data Rows: Encapsulated logic for clean, repeated UI elements.
            PitchTypeRow("4-Seam Fastball", ".312", ".587", "18.5%")
            PitchTypeRow("Slider", ".218", ".385", "35.8%")
            PitchTypeRow("Changeup", ".245", ".421", "28.3%")
        }
    }
}

/**
 * A small, vertical widget for displaying a single metric label and its value.
 * - Centers content for a "dashboard" look.
 * - Standardizes font weights and colors for labels.
 */
@Composable
fun AdvancedStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/**
 * Represents a single row in the 'Performance vs Pitch Type' table.
 * - Uses 'weight' ratios (2:1:1:1) to ensure columns align perfectly across rows.
 * - Includes a subtle divider to improve horizontal scanning for the user.
 */
@Composable
fun PitchTypeRow(name: String, ba: String, slg: String, whiff: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Pitch name gets more space (weight 2) to accommodate longer text.
        Text(name, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium)
        Text(ba, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(slg, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(whiff, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
}