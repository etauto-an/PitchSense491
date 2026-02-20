package com.example.pitchsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pitchsense.ui.components.HeaderBar
import com.example.pitchsense.ui.components.StatCard

/**
 * The main analytical dashboard for PitchSense.
 * - This screen coordinates user inputs (batter/pitcher selection) with visual
 * statistical summaries.
 * - It uses a 'TabRow' to toggle between global batter data and matchup-specific data.
 */
@Composable
fun OverviewScreen(
    onNavigateToAdvancedStats: () -> Unit,
    onNavigateToHeatMap: () -> Unit,
    onNavigateToPitchSequence: () -> Unit
) {
    // Tracks the current active tab (0 = General, 1 = Pitcher Specific).
    // Using 'remember' ensures the selection persists during UI updates.
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        // Shared branding component from the components package.
        HeaderBar(title = "PitchSense: Analyze Batter")

        Column(modifier = Modifier.padding(16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            // Date Range Selection: Uses 'weight(1f)' to evenly split the horizontal space.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Start Date") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("End Date") }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary filter inputs for the scouting report.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = "Mike Trout", onValueChange = {}, label = { Text("Choose Batter (Required)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = "Liam Hendriks", onValueChange = {}, label = { Text("Choose Pitcher (Optional)") }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigational Tabs: Switches the context of the statistical display below.
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("General") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Pitcher Specific") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            /**
             * Conditional Content Rendering:
             * Only the Composable matching the 'selectedTabIndex' is composed and drawn.
             * This keeps the UI responsive and focused on the user's current intent.
             */
            when (selectedTabIndex) {
                0 -> GeneralStatsContent()
                1 -> PitcherSpecificStatsContent()
            }

            // Occupies all remaining vertical space to force the navigation buttons to the bottom.
            Spacer(modifier = Modifier.weight(1f))

            // High-level navigation actions leading to detailed reports.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onNavigateToAdvancedStats, modifier = Modifier.weight(1f)) {
                    Text("Advanced Stats")
                }
                Button(onClick = onNavigateToHeatMap, modifier = Modifier.weight(1f)) {
                    Text("Batter Heat Maps")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToPitchSequence, modifier = Modifier.fillMaxWidth()) {
                Text("Recommended Pitch Sequence")
            }
        }
    }
}

/**
 * Displays the batter's overall seasonal performance.
 * - Uses 'StatCard' components to highlight core metrics like BA and K%.
 */
@Composable
fun GeneralStatsContent() {
    Column {
        Text("Batter Overview", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // isPrimary = true uses a distinct background for high-priority metrics.
            StatCard(title = "BA (Batting Avg)", value = ".285", isPrimary = true, modifier = Modifier.weight(1f))
            StatCard(title = "K% (Strikeout)", value = "24.3%", isPrimary = true, modifier = Modifier.weight(1f))
            StatCard(title = "BB% (Walk)", value = "11.2%", isPrimary = true, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(title = "OBP", value = ".362", isPrimary = false, modifier = Modifier.weight(1f))
            StatCard(title = "HRs", value = "37", isPrimary = false, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Displays stats filtered specifically against the selected pitcher (Liam Hendriks).
 * - This content is swapped in when the second tab is selected.
 */
@Composable
fun PitcherSpecificStatsContent() {
    Column {
        Text("Batter Overview vs. Liam Hendriks", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(title = "BA (Batting Avg)", value = ".318", isPrimary = true, modifier = Modifier.weight(1f))
            StatCard(title = "K% (Strikeout)", value = "19.2%", isPrimary = true, modifier = Modifier.weight(1f))
            StatCard(title = "BB% (Walk)", value = "10.7%", isPrimary = true, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(title = "OBP", value = ".385", isPrimary = false, modifier = Modifier.weight(1f))
            StatCard(title = "HRs", value = "5", isPrimary = false, modifier = Modifier.weight(1f))
        }
    }
}