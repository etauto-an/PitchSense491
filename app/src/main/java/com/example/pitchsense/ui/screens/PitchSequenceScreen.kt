package com.example.pitchsense.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Screen providing a step-by-step pitching strategy.
 * - Wraps content in 'ScreenScaffold' to inherit the back button and standard layout shell.
 * - Uses 'verticalScroll' to ensure the sequence remains accessible on all device heights.
 */
@Composable
fun PitchSequenceScreen(onBackClick: () -> Unit) {
    ScreenScaffold(title = "Recommended Pitch Sequence", onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Optional Input Section: Allows users to refine the prediction parameters.
            Text("Game Situation (Optional)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = "3", onValueChange = {}, label = { Text("Pitches to Predict") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Inning") }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results Section: Displays the actual suggested pitches.
            Text("Recommended Pitch Sequence (3 Pitches)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            /**
             * Procedural List: We manually call 'SequenceStep' here.
             * In a production app, this would be generated from a List of objects
             * using a loop (e.g., sequenceData.forEachIndexed { ... }).
             */
            SequenceStep(1, "Slider", "Low and away, 87.5 mph - Target weak zone", "82%")
            SequenceStep(2, "Slider", "Down and in, 88.1 mph - Exploit low BA zone", "78%")
            SequenceStep(3, "Changeup", "Down and away, 84.5 mph - Induce weak contact", "75%")

            Spacer(modifier = Modifier.height(16.dp))

            // Insight Box: Uses a light yellow background to highlight 'Expert Analysis'
            // separate from the raw 'Data' in the list above.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFDE7), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Analysis: Based on Mike Trout's weaknesses and Liam Hendriks' arsenal strengths.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * A custom list item representing a single step in a sequence.
 * - @param num: The position in the sequence, displayed in a branded circular badge.
 * - @param type: The primary label (e.g., "Fastball").
 * - @param desc: Secondary details like location and speed.
 * - @param effectiveness: A highlight metric indicating the confidence level of this step.
 */
@Composable
fun SequenceStep(num: Int, type: String, desc: String, effectiveness: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step Indicator Badge: Uses a 'Surface' with a rounded shape to create a clean circle.
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF233B90),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$num", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Main content column. Uses weight(1f) to occupy the center space.
        Column(modifier = Modifier.weight(1f)) {
            Text(type, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }

        // Probability percentage displayed in the app's primary color.
        Text(effectiveness, color = Color(0xFF233B90), fontWeight = FontWeight.Bold)
    }
}