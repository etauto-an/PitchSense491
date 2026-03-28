package com.example.pitchsense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.tooling.preview. Preview
import androidx.compose.ui.unit.dp
import com.example.pitchsense.data.model.PitchTypeStat
import com.example.pitchsense.data.model.StatItem
import com.example.pitchsense.ui.components.ScreenScaffold
import com.example.pitchsense.ui.theme.Dimensions


/** Advanced metrics screen combining direct Statcast metrics and whiff breakdowns. */

//@Preview(showBackground = true)
//@Composable
//fun AdvancedStatsScreenPreview() {

//}
@Composable
fun AdvancedStatsScreen(
    batter: String,
    summaryStats: List<StatItem>,
    disciplineStats: List<StatItem>,
    battedBallStats: List<StatItem>,
    pitchTypeStats: List<PitchTypeStat>,
    isError: Boolean,
    isOffline: Boolean,
    onBackClick: () -> Unit
) {
    // Merge source sections, then project into a fixed display order for stable UI.
    val directMetricMap = (summaryStats + disciplineStats + battedBallStats).associateBy { it.title }
    val directMetricsOrder = listOf(
        "xwOBA",
        "xBA",
        "xSLG",
        "Hard Hit%",
        "Barrel%",
        "Avg EV",
        "Max EV",
        "Sweet Spot%",
        "Chase%",
        "Whiff%",
        "Zone Contact%",
        "CSW%"
    )
    val directMetrics = directMetricsOrder.mapNotNull { directMetricMap[it] }

    ScreenScaffold(title = "Advanced Batter Statistics", onBackClick = onBackClick, isOffline = isOffline) {
        if (isError) {
            Text(
                text = "Unable to load stats. Check that the backend is running.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
            return@ScreenScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F6FF))
            ) {
                Column(modifier = Modifier.padding(Dimensions.cardPadding)) {
                    Text("Direct Statcast Metrics", fontSize = Dimensions.titleFontSize)
                    Text(
                        batter,
                        fontSize = Dimensions.bodyFontSize,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

                    // Render as a compact 3-column grid.
                    directMetrics.chunked(3).forEach { rowStats ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowStats.forEach { stat ->
                                CompactMetricCard(
                                    label = stat.title,
                                    value = stat.value,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimensions.spacingSmall),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Whiff by Pitch Type", fontSize = Dimensions.labelFontSize, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(Dimensions.spacingSmall))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pitch", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("Whiff", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = Color(0xFFD7DEE8))

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        pitchTypeStats.forEach { stat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stat.pitch,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stat.whiff,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider(color = Color(0xFFE6ECF3))
                        }
                    }
                }
            }
        }
    }
}

/** Displays one value cell in the direct-metrics grid without extra card chrome. */
@Composable
private fun CompactMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    // Minimal card used by the direct metrics grid.
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = Dimensions.bodyFontSize, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = Dimensions.titleFontSize, fontWeight = FontWeight.Bold)
        }
    }
}
