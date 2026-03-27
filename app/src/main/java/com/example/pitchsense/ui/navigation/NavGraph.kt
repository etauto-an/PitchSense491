package com.example.pitchsense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pitchsense.ui.screens.AdvancedStatsScreen
import com.example.pitchsense.ui.screens.HeatMapScreen
import com.example.pitchsense.ui.screens.LoginScreen
import com.example.pitchsense.ui.screens.OverviewScreen
import com.example.pitchsense.ui.screens.PitchSequenceScreen
import com.example.pitchsense.ui.viewmodel.PitchSenseViewModel

/** Root Compose navigation host for the PitchSense app. */
@Composable
fun PitchSenseApp() {
    // Single nav controller for the app graph.
    val navController = rememberNavController()
    // Shared view model across destinations in this host.
    val vm: PitchSenseViewModel = viewModel()
    // Observe UI state as Compose state for automatic recomposition.
    val state by vm.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // Entry screen — no auth required for MVP.
        composable("login") {
            LoginScreen(
                onEnter = {
                    // Remove login from the back stack so pressing "back" from Overview
                    // exits the app rather than returning to the login screen.
                    navController.navigate("overview") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Primary landing/dashboard screen.
        composable("overview") {
            OverviewScreen(
                selectedBatter = state.selectedBatter,
                selectedPitcher = state.selectedPitcher,
                batterOptions = vm.getBatterOptions(),
                pitcherOptions = vm.getPitcherOptions(),
                generalStats = state.overviewGeneral,
                pitcherSpecificStats = state.overviewPitcherSpecific,
                isError = state.overviewError,
                isOffline = state.isOffline,
                onBatterSelected = vm::onBatterSelected,
                onPitcherSelected = vm::onPitcherSelected,
                onNavigateToAdvancedStats = { navController.navigate("advanced_stats") },
                onNavigateToHeatMap = { navController.navigate("heat_map") },
                onNavigateToPitchSequence = { navController.navigate("pitch_sequence") }
            )
        }

        // Detailed advanced metrics destination.
        composable("advanced_stats") {
            AdvancedStatsScreen(
                batter = state.selectedBatter,
                summaryStats = state.advancedSummaryStats,
                disciplineStats = state.advancedDisciplineStats,
                battedBallStats = state.advancedBattedBallStats,
                pitchTypeStats = state.pitchTypeStats,
                isError = state.advancedError,
                isOffline = state.isOffline,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Strike-zone heat map analysis destination.
        composable("heat_map") {
            HeatMapScreen(
                batter = state.selectedBatter,
                selectedMetric = state.selectedHeatMetric,
                heatMap = state.heatMap,
                isError = state.heatMapError,
                isOffline = state.isOffline,
                onMetricSelected = vm::onHeatMetricSelected,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Pitch recommendation workflow destination.
        composable("pitch_sequence") {
            PitchSequenceScreen(
                batter = state.selectedBatter,
                pitcher = state.selectedPitcher,
                pitchesToPredict = state.pitchesToPredict,
                balls = state.balls,
                strikes = state.strikes,
                outs = state.outs,
                inning = state.inning,
                runnerOnFirst = state.runnerOnFirst,
                runnerOnSecond = state.runnerOnSecond,
                runnerOnThird = state.runnerOnThird,
                generatedBalls = state.generatedBalls,
                generatedStrikes = state.generatedStrikes,
                generatedOuts = state.generatedOuts,
                generatedInning = state.generatedInning,
                generatedRunnerOnFirst = state.generatedRunnerOnFirst,
                generatedRunnerOnSecond = state.generatedRunnerOnSecond,
                generatedRunnerOnThird = state.generatedRunnerOnThird,
                appliedScenario = state.appliedScenario,
                sequence = state.recommendedSequence,
                isOffline = state.isOffline,
                onPitchesToPredictChanged = vm::onPitchesToPredictChanged,
                onBallsChanged = vm::onBallsChanged,
                onStrikesChanged = vm::onStrikesChanged,
                onOutsChanged = vm::onOutsChanged,
                onInningChanged = vm::onInningChanged,
                onRunnerOnFirstChanged = vm::onRunnerOnFirstChanged,
                onRunnerOnSecondChanged = vm::onRunnerOnSecondChanged,
                onRunnerOnThirdChanged = vm::onRunnerOnThirdChanged,
                onGenerateUpdatedSequence = vm::onGenerateUpdatedSequence,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
