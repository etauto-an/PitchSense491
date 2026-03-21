package com.example.pitchsense.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview. Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchsense.ui.components.LegendItem
import com.example.pitchsense.ui.components.ScreenScaffold
import com.example.pitchsense.ui.components.ToggleButton
import com.example.pitchsense.ui.theme.Dimensions

/**
 * High-level screen that manages the state for batter performance visualization.
 * - Uses 'remember' to preserve the selected metric (BA, SLG, OPS) during recomposition.
 * - Wraps the content in 'ScreenScaffold' to provide a consistent back-navigation experience.
 */
@Preview(showBackground = true)
@Composable
fun HeatMapScreenPreview() {
    HeatMapScreen(onBackClick = {})
}
@Composable
fun HeatMapScreen(onBackClick: () -> Unit) {
    // Current statistical category being viewed. Changing this updates the grid data.
    var selectedMetric by remember { mutableStateOf("BA") }

    ScreenScaffold(title = "Batter Heat Maps", onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(Dimensions.spacingMedium)
                // Allows users to scroll through the legend and analysis on smaller screens.
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)
            ) {
                Text(
                    text = "Batter Performance Heat Map - Mike Trout",
                    fontSize = Dimensions.labelFontSize,
                    fontWeight = FontWeight.Bold
                )

                // Toggle buttons report back selection changes, updating 'selectedMetric'.
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)) {
                    ToggleButton("BA", selectedMetric == "BA") { selectedMetric = "BA" }
                    ToggleButton("SLG", selectedMetric == "SLG") { selectedMetric = "SLG" }
                    ToggleButton("OPS", selectedMetric == "OPS") { selectedMetric = "OPS" }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLarge)
            ) {
                // Left Column: The visual representation of the strike zone.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeatMapGrid(metric = selectedMetric)
                    Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
                    Text(
                        text = "Strike Zone View (Catcher's Perspective)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                // Right Column: Key/Legend and Strategic Analysis.
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Performance Legend", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

                    // Reusable LegendItems explain the color values used in the grid.
                    LegendItem(color = Color(0xFF00897B), label = ".350+ (Elite)")
                    LegendItem(color = Color(0xFF26A69A), label = ".320-.349 (Excellent)")
                    LegendItem(color = Color(0xFFFFCA28), label = ".240-.269 (Below Avg)")
                    LegendItem(color = Color(0xFFFFB74D), label = "<.240 (Weak)")

                    Spacer(modifier = Modifier.height(Dimensions.spacingLarge))
                    HorizontalDivider(color = Color(0xFFE0E0E0))
                    Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

                    Text(text = "Analysis", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Green zones indicate areas where the batter excels. Target low-away zones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

/**
 * Builds a dynamic 5x5 grid representing the strike zone and surrounding ball zones.
 * - Maps numerical data to specific colors for immediate visual interpretation.
 * - 'isStrikeZone' logic adds a black border to the inner 3x3 cells for clarity.
 */
@Composable
fun HeatMapGrid(metric: String) {
    val outerZoneColor = Color(0xFFE8F5E9)
    val eliteGreen = Color(0xFF00897B)
    val averageGreen = Color(0xFF80CBC4)
    val belowAvgYellow = Color(0xFFFFCA28)
    val weakOrange = Color(0xFFFFB74D)

    // Data selector based on current UI state.
    val gridData = when (metric) {
        "SLG" -> listOf(
            listOf(".310" to outerZoneColor, ".320" to outerZoneColor, ".340" to outerZoneColor, ".330" to outerZoneColor, ".300" to outerZoneColor),
            listOf(".350" to outerZoneColor, ".450" to averageGreen, ".580" to eliteGreen, ".510" to averageGreen, ".380" to outerZoneColor),
            listOf(".370" to outerZoneColor, ".505" to averageGreen, ".620" to eliteGreen, ".595" to eliteGreen, ".390" to outerZoneColor),
            listOf(".340" to outerZoneColor, ".410" to belowAvgYellow, ".440" to averageGreen, ".405" to weakOrange, ".330" to outerZoneColor),
            listOf(".290" to outerZoneColor, ".310" to outerZoneColor, ".320" to outerZoneColor, ".315" to outerZoneColor, ".280" to outerZoneColor)
        )
        else -> listOf( // Default logic for BA/OPS
            listOf(".180" to outerZoneColor, ".195" to outerZoneColor, ".205" to outerZoneColor, ".198" to outerZoneColor, ".175" to outerZoneColor),
            listOf(".210" to outerZoneColor, ".285" to averageGreen, ".342" to eliteGreen, ".318" to averageGreen, ".225" to outerZoneColor),
            listOf(".225" to outerZoneColor, ".312" to averageGreen, ".395" to eliteGreen, ".358" to eliteGreen, ".245" to outerZoneColor),
            listOf(".195" to outerZoneColor, ".268" to belowAvgYellow, ".288" to averageGreen, ".255" to belowAvgYellow, ".205" to outerZoneColor),
            listOf(".165" to outerZoneColor, ".188" to outerZoneColor, ".195" to outerZoneColor, ".182" to outerZoneColor, ".158" to outerZoneColor)
        )
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f) // Maintains a perfect square regardless of screen width.
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(Dimensions.spacingSmall)
    ) {
        gridData.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.weight(1f)) {
                row.forEachIndexed { colIndex, cell ->
                    // Logic to visually distinguish the 3x3 strike zone from outer balls.
                    val isStrikeZone = rowIndex in 1..3 && colIndex in 1..3
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(cell.second)
                            .border(width = if (isStrikeZone) 1.dp else 0.dp, color = Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = cell.first, fontSize = Dimensions.gridTextSize)
                    }
                }
            }
        }
    }
}