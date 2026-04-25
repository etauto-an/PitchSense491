package com.example.pitchsense.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pitchsense.data.model.SequenceRecommendation
import com.example.pitchsense.ui.components.ScreenScaffold
import com.example.pitchsense.ui.components.ToggleButton

/** Sequence recommendation screen driven by user-selected game context. */
@Composable
fun PitchSequenceScreen(
    batter: String,
    pitcher: String,
    pitchesToPredict: String,
    balls: String,
    strikes: String,
    outs: String,
    inning: String,
    runnerOnFirst: Boolean,
    runnerOnSecond: Boolean,
    runnerOnThird: Boolean,
    generatedBalls: String,
    generatedStrikes: String,
    generatedOuts: String,
    generatedInning: String,
    generatedRunnerOnFirst: Boolean,
    generatedRunnerOnSecond: Boolean,
    generatedRunnerOnThird: Boolean,
    appliedScenario: String,
    sequence: List<SequenceRecommendation>,
    isLoading: Boolean,
    isOffline: Boolean,
    onPitchesToPredictChanged: (String) -> Unit,
    onBallsChanged: (String) -> Unit,
    onStrikesChanged: (String) -> Unit,
    onOutsChanged: (String) -> Unit,
    onInningChanged: (String) -> Unit,
    timesThrough: Int?,
    onTimesThroughChanged: (Int?) -> Unit,
    onRunnerOnFirstChanged: (Boolean) -> Unit,
    onRunnerOnSecondChanged: (Boolean) -> Unit,
    onRunnerOnThirdChanged: (Boolean) -> Unit,
    onGenerateUpdatedSequence: () -> Unit,
    onBackClick: () -> Unit
) {
    // Generated values represent the most recently applied scenario.
    val generatedBallsValue = (generatedBalls.toIntOrNull() ?: 0).coerceIn(0, 3)
    val generatedStrikesValue = (generatedStrikes.toIntOrNull() ?: 0).coerceIn(0, 2)
    val generatedOutsValue = (generatedOuts.toIntOrNull() ?: 0).coerceIn(0, 2)
    val generatedInningValue = generatedInning.toIntOrNull() ?: 1
    val generatedRunnersOn = listOf(
        generatedRunnerOnFirst,
        generatedRunnerOnSecond,
        generatedRunnerOnThird
    ).count { it }
    val isHighLeverage =
        (generatedInningValue >= 7 && generatedRunnersOn >= 1) ||
            (generatedOutsValue == 2 && generatedRunnersOn >= 2)

    // Use the scenario label from the backend when available; derive client-side as fallback.
    val scenarioTag = if (appliedScenario.isNotBlank()) {
        appliedScenario
    } else {
        when {
            generatedStrikesValue == 2 -> "Two-Strike Putaway"
            generatedBallsValue == 3 -> "Hitter Count Damage Control"
            isHighLeverage -> "High Leverage"
            generatedRunnerOnThird && generatedOutsValue < 2 -> "Runner on 3rd, <2 Outs"
            else -> "Neutral Situation"
        }
    }

    val runnerSituation = buildString {
        if (generatedRunnerOnFirst) append("1B")
        if (generatedRunnerOnSecond) {
            if (isNotEmpty()) append(", ")
            append("2B")
        }
        if (generatedRunnerOnThird) {
            if (isNotEmpty()) append(", ")
            append("3B")
        }
        if (isEmpty()) append("Bases Empty")
    }
    val countLabel = "$generatedBallsValue-$generatedStrikesValue"
    val outsLabel = "$generatedOutsValue"

    // Editable control values are separately clamped for robust UI interaction.
    val inningValue = (inning.toIntOrNull() ?: 1).coerceIn(1, 9)
    val ballsValue = (balls.toIntOrNull() ?: 0).coerceIn(0, 3)
    val strikesValue = (strikes.toIntOrNull() ?: 0).coerceIn(0, 2)
    val pitchesToPredictValue = (pitchesToPredict.toIntOrNull() ?: 3).coerceIn(1, 3)
    val outsValue = (outs.toIntOrNull() ?: 0).coerceIn(0, 2)
    val canGenerateSequence = pitcher.isNotBlank()
    val hasSequenceResult = isLoading || sequence.isNotEmpty()

    ScreenScaffold(title = "Recommend Pitch Sequence", onBackClick = onBackClick, isOffline = isOffline) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text("Game Situation (Optional)", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            CountCircles(
                label = "Inning",
                selectedCount = inningValue,
                maxCount = 9,
                onCountChanged = { onInningChanged(it.toString()) },
                activeColor = Color(0xFF1E88E5),
                showStepNumbers = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            CountCircles(
                label = "Pitches to Predict",
                selectedCount = pitchesToPredictValue,
                maxCount = 3,
                onCountChanged = { onPitchesToPredictChanged(it.toString()) },
                activeColor = Color(0xFF6A1B9A),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountCircles(
                    label = "Balls",
                    selectedCount = ballsValue,
                    maxCount = 3,
                    onCountChanged = { onBallsChanged(it.toString()) },
                    activeColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )

                CountCircles(
                    label = "Strikes",
                    selectedCount = strikesValue,
                    maxCount = 2,
                    onCountChanged = { onStrikesChanged(it.toString()) },
                    activeColor = Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CountCircles(
                label = "Outs",
                selectedCount = outsValue,
                maxCount = 2,
                onCountChanged = { onOutsChanged(it.toString()) },
                activeColor = Color(0xFFEF6C00),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Times Through Order — tapping the active selection deselects it (null = unknown).
            Column {
                Text("Times Through Order", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    listOf(1, 2, 3).forEach { n ->
                        val isSelected = timesThrough == n
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { onTimesThroughChanged(if (isSelected) null else n) },
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF00796B) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF00796B))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    // The third bucket covers 3+ times through order.
                                    text = if (n == 3) "3+" else n.toString(),
                                    color = if (isSelected) Color.White else Color(0xFF00796B),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Runners on Base", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ToggleButton(
                    text = "1B",
                    isSelected = runnerOnFirst,
                    onClick = { onRunnerOnFirstChanged(!runnerOnFirst) }
                )
                ToggleButton(
                    text = "2B",
                    isSelected = runnerOnSecond,
                    onClick = { onRunnerOnSecondChanged(!runnerOnSecond) }
                )
                ToggleButton(
                    text = "3B",
                    isSelected = runnerOnThird,
                    onClick = { onRunnerOnThirdChanged(!runnerOnThird) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGenerateUpdatedSequence,
                enabled = canGenerateSequence && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Generate Updated Sequence")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (hasSequenceResult) {
                Text("Recommended Pitch Sequence vs. $batter", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8EEF9)
                ) {
                    Text(
                        text = "Applied Scenario: $scenarioTag",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color(0xFF233B90),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    // Centered spinner shown while the sequence API call is in flight.
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF233B90))
                    }
                } else {
                    sequence.forEach { step ->
                        SequenceStep(
                            num = step.step,
                            type = step.pitchType,
                            desc = step.description,
                            effectiveness = step.effectiveness
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFDE7), RoundedCornerShape(12.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        "Analysis: Based on $batter's weaknesses, $pitcher's arsenal strengths, count ($countLabel), outs ($outsLabel), and current base state ($runnerSituation).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = if (canGenerateSequence) {
                        "Generate a sequence to see recommendations."
                    } else {
                        "Select a pitcher to generate a recommendation."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

/** Renders tappable count indicators that behave like a bounded step selector. */
@Composable
private fun CountCircles(
    label: String,
    selectedCount: Int,
    maxCount: Int,
    onCountChanged: (Int) -> Unit,
    activeColor: Color,
    showStepNumbers: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(maxCount) { index ->
                val threshold = index + 1
                val isFilled = threshold <= selectedCount

                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clickable {
                            // Tapping selected threshold decrements by one; otherwise set directly.
                            val updated = if (selectedCount == threshold) index else threshold
                            onCountChanged(updated)
                        },
                    shape = CircleShape,
                    color = if (isFilled) activeColor else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(3.dp, activeColor)
                ) {
                    if (showStepNumbers) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = threshold.toString(),
                                color = if (isFilled) Color.White else activeColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Single row in the recommended sequence list. */
@Composable
fun SequenceStep(num: Int, type: String, desc: String, effectiveness: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF233B90),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$num", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(type, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Text(effectiveness, color = Color(0xFF233B90), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}
