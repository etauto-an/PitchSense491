package com.example.pitchsense.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview. Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchsense.data.model.HeatLevel
import com.example.pitchsense.data.model.HeatMapCell
import com.example.pitchsense.ui.components.LegendItem
import com.example.pitchsense.ui.components.ScreenScaffold
import com.example.pitchsense.ui.components.ToggleButton
import com.example.pitchsense.ui.theme.Dimensions

/** Heat-map screen with metric toggles, zone grid, and performance legend. */
@Preview(showBackground = true)
@Composable
fun HeatMapScreenPreview() {
    HeatMapScreen(onBackClick = {})
}

@Composable
fun HeatMapScreen(
    batter: String,
    selectedMetric: String,
    heatMap: List<List<HeatMapCell>>,
    isError: Boolean,
    isOffline: Boolean,
    onMetricSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    ScreenScaffold(title = "Batter Heat Maps", onBackClick = onBackClick, isOffline = isOffline) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(Dimensions.spacingMedium)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)
            ) {
                Text(
                    text = "Batter Performance Heat Map — $batter",
                    fontSize = Dimensions.labelFontSize,
                    fontWeight = FontWeight.Bold
                )

                // Stateless tabs that delegate selection to screen state.
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)) {
                    ToggleButton("BA", selectedMetric == "BA") { onMetricSelected("BA") }
                    ToggleButton("SLG", selectedMetric == "SLG") { onMetricSelected("SLG") }
                    ToggleButton("OPS", selectedMetric == "OPS") { onMetricSelected("OPS") }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            if (isError) {
                Text(
                    text = "Unable to load heat map. Check that the backend is running.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 460.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Square strike-zone grid with 5x5 buckets.
                        HeatMapGrid(heatMap = heatMap)
                        Spacer(modifier = Modifier.height(spacingSmall))
                        Text(
                            text = "Strike Zone View (Catcher's Perspective)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacingMedium))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(spacingMedium))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(
                        text = "Performance Legend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(spacingSmall))
                    // Threshold labels vary by metric — use MLB-standard benchmarks for each.
                    val (elite, excellent, belowAvg, weak) = when (selectedMetric) {
                        "SLG" -> listOf(".600+ (Elite)", ".500-.599 (Excellent)", ".350-.399 (Below Avg)", "<.350 (Weak)")
                        "OPS" -> listOf(".950+ (Elite)", ".850-.949 (Excellent)", ".650-.749 (Below Avg)", "<.650 (Weak)")
                        else  -> listOf(".350+ (Elite)", ".320-.349 (Excellent)", ".240-.269 (Below Avg)", "<.240 (Weak)")
                    }
                    LegendItem(color = Color(0xFF00897B), label = elite)
                    LegendItem(color = Color(0xFF26A69A), label = excellent)
                    LegendItem(color = Color(0xFFFFCA28), label = belowAvg)
                    LegendItem(color = Color(0xFFFFB74D), label = weak)
                }
            }
        }
    }
}

/** Renders the 5x5 zone matrix and outlines the central 3x3 strike-zone area. */
@Composable
fun HeatMapGrid(heatMap: List<List<HeatMapCell>>) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp))
            .padding(Dimensions.spacingSmall)
    ) {
        heatMap.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.weight(1f)) {
                row.forEachIndexed { colIndex, cell ->
                    // Highlight the 3x3 core strike zone inside the 5x5 context grid.
                    val isStrikeZone = rowIndex in 1..3 && colIndex in 1..3
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colorForHeatLevel(cell.level))
                            .border(width = if (isStrikeZone) 1.dp else 0.dp, color = Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = cell.value, fontSize = Dimensions.gridTextSize)
                    }
                }
            }
        }
    }
}

/** Maps semantic heat level categories to UI colors. */
private fun colorForHeatLevel(level: HeatLevel): Color = when (level) {
    HeatLevel.OUTER -> Color(0xFFE8F5E9)
    HeatLevel.ELITE -> Color(0xFF00897B)
    HeatLevel.GOOD -> Color(0xFF80CBC4)
    HeatLevel.BELOW_AVG -> Color(0xFFFFCA28)
    HeatLevel.WEAK -> Color(0xFFFFB74D)
}
