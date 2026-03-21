package com.example.pitchsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview. Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pitchsense.ui.components.HeaderBar
import com.example.pitchsense.ui.components.StatCard
import com.example.pitchsense.ui.theme.Dimensions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment



/**
 * The main analytical dashboard for PitchSense.
 * - This screen coordinates user inputs (batter/pitcher selection) with visual
 * statistical summaries.
 * - It uses a 'TabRow' to toggle between global batter data and matchup-specific data.
 */
@Preview(showBackground = true)
@Composable
fun OverviewScreenPreview() {
    OverviewScreen(
        onNavigateToAdvancedStats = {},
        onNavigateToHeatMap = {},
        onNavigateToPitchSequence = {}
    )
}
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

        Column(modifier = Modifier.padding(Dimensions.spacingMedium)) {
            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))
            //screen for the game score
            GameScoreScreen()
            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            // Date Range Selection: Uses 'weight(1f)' to evenly split the horizontal space.
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)) {
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Start Date", fontSize = Dimensions.labelFontSize) }, placeholder = { Text("mm/dd/yyyy", fontSize = Dimensions.labelFontSize)}, modifier = Modifier.weight(1f), textStyle = TextStyle(fontSize = Dimensions.bodyFontSize))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("End Date", fontSize = Dimensions.labelFontSize) }, placeholder = { Text("mm/dd/yyyy", fontSize = Dimensions.labelFontSize)}, modifier = Modifier.weight(1f),  textStyle = TextStyle(fontSize = Dimensions.bodyFontSize))
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingSmall))

            // Primary filter inputs for the scouting report.
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)) {
                OutlinedTextField(value = "Mike Trout", onValueChange = {}, label = { Text("Choose Batter (Required)", fontSize = Dimensions.labelFontSize) }, modifier = Modifier.weight(1f), textStyle = TextStyle(fontSize = Dimensions.bodyFontSize))
                OutlinedTextField(value = "Liam Hendriks", onValueChange = {}, label = { Text("Choose Pitcher (Optional)", fontSize = Dimensions.labelFontSize) }, modifier = Modifier.weight(1f), textStyle = TextStyle(fontSize = Dimensions.bodyFontSize))
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            // Navigational Tabs: Switches the context of the statistical display below.
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("General", fontSize = Dimensions.labelFontSize) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Pitcher Specific", fontSize = Dimensions.labelFontSize) }
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
                Button(onClick = onNavigateToAdvancedStats, modifier = Modifier.weight(1f).height(Dimensions.buttonHeight)) {
                    Text("Advanced Stats", fontSize = Dimensions.buttonFontSize)
                }
                Button(onClick = onNavigateToHeatMap, modifier = Modifier.weight(1f).height(Dimensions.buttonHeight)) {
                    Text("Batter Heat Maps", fontSize = Dimensions.buttonFontSize)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToPitchSequence, modifier = Modifier.fillMaxWidth().height(Dimensions.buttonHeight)) {
                Text("Recommended Pitch Sequence", fontSize = Dimensions.buttonFontSize)
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
        Text("Batter Overview",  fontSize = Dimensions.titleFontSize)
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
        Text("Batter Overview vs. Liam Hendriks", fontSize = Dimensions.titleFontSize)
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
// Screen with the teams scores.
@Composable
fun GameScoreScreen() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF233B90))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingSmall),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Home", fontSize = Dimensions.bodyFontSize, fontWeight = FontWeight.Bold, color = Color.White)
                Text("12:26", fontSize = Dimensions.titleFontSize, fontWeight = FontWeight.Bold, color = Color.Yellow)
                Text("Guest", fontSize = Dimensions.bodyFontSize, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("02", fontSize = Dimensions.titleFontSize, fontWeight = FontWeight.Bold, color = Color.Red)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("INNING", fontSize = Dimensions.labelFontSize, color = Color.White)
                    Text("6", fontSize = Dimensions.titleFontSize, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("06", fontSize = Dimensions.titleFontSize, fontWeight = FontWeight.Bold, color = Color.Red)
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BALL", fontSize = Dimensions.labelFontSize, color = Color.White)
                    Row { Text("X", color = Color.Red); Text("X", color = Color.Red) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STRIKE", fontSize = Dimensions.labelFontSize, color = Color.White)
                    Row { Text("X", color = Color.Red); Text("X", color = Color.Red) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OUT", fontSize = Dimensions.labelFontSize, color = Color.White)
                    Text("X", color = Color.Red)
                }
            }
        }
    }
}