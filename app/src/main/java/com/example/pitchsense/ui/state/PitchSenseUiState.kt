package com.example.pitchsense.ui.state

import com.example.pitchsense.data.model.HeatMapCell
import com.example.pitchsense.data.model.PitchTypeStat
import com.example.pitchsense.data.model.SequenceRecommendation
import com.example.pitchsense.data.model.StatItem

/**
 * Single source of truth for all UI state across the app.
 *
 * Pitch sequence inputs come in two groups:
 * - Editable fields (e.g. `balls`, `strikes`) — updated immediately as the user adjusts controls.
 * - Generated/snapshot fields (e.g. `generatedBalls`, `generatedStrikes`) — copied from the
 *   editable fields when the user presses "Generate Updated Sequence", then used to fetch
 *   recommendations. This two-field pattern lets users freely edit inputs without triggering
 *   a reload until they explicitly commit.
 */
data class PitchSenseUiState(
    val selectedBatterId: String = "",
    val selectedBatter: String = "",
    val selectedPitcherId: String = "",
    val selectedPitcher: String = "",
    // Reserved for future multi-tab layout on the overview screen; currently unused.
    val overviewTabIndex: Int = 0,
    val selectedHeatMetric: String = "BA",

    // Editable pitch sequence inputs — reflect what the user has currently selected in the form.
    val pitchesToPredict: String = "3",
    val balls: String = "0",
    val strikes: String = "0",
    val outs: String = "0",
    val inning: String = "",
    val runnerOnFirst: Boolean = false,
    val runnerOnSecond: Boolean = false,
    val runnerOnThird: Boolean = false,

    // Snapshot of the last-submitted pitch sequence inputs, shown alongside the results.
    val generatedPitchesToPredict: String = "3",
    val generatedBalls: String = "0",
    val generatedStrikes: String = "0",
    val generatedOuts: String = "0",
    val generatedInning: String = "",
    val generatedRunnerOnFirst: Boolean = false,
    val generatedRunnerOnSecond: Boolean = false,
    val generatedRunnerOnThird: Boolean = false,

    // Loaded async by ViewModel — default to empty until first fetch completes.
    val overviewGeneral: List<StatItem> = emptyList(),
    val overviewPitcherSpecific: List<StatItem> = emptyList(),
    val advancedSummaryStats: List<StatItem> = emptyList(),
    val advancedDisciplineStats: List<StatItem> = emptyList(),
    val advancedBattedBallStats: List<StatItem> = emptyList(),
    val pitchTypeStats: List<PitchTypeStat> = emptyList(),
    val heatMap: List<List<HeatMapCell>> = emptyList(),
    val appliedScenario: String = "",
    val recommendedSequence: List<SequenceRecommendation> = emptyList(),

    // Set to true when a backend fetch fails so screens can show an error instead of empty content.
    val overviewError: Boolean = false,
    val advancedError: Boolean = false,
    val heatMapError: Boolean = false,

    // True when any screen's data is being served from the local fallback instead of the live API.
    val isOffline: Boolean = false,
)
