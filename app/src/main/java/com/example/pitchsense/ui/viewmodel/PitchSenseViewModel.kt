package com.example.pitchsense.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pitchsense.data.model.PlayerOption
import com.example.pitchsense.data.repository.FakePitchSenseRepository
import com.example.pitchsense.data.repository.PitchSenseRepository
import com.example.pitchsense.data.repository.RepositoryProvider
import com.example.pitchsense.ui.state.PitchSenseUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** ViewModel coordinating UI state mutations and repository reads for PitchSense. */
class PitchSenseViewModel(
    private val repository: PitchSenseRepository = RepositoryProvider.create()
) : ViewModel() {

    companion object {
        private const val TAG = "PitchSenseViewModel"
    }

    // Fallback data source used when the live API is unreachable. Always available locally.
    private val fallback = FakePitchSenseRepository()

    // Cache static option lists once for stable dropdown ordering.
    private val batterOptions: List<PlayerOption> = repository.batterOptions()
    private val pitcherOptions: List<PlayerOption> = repository.pitcherOptions()
    private val batterByDisplayName = batterOptions.associateBy { it.displayName }
    private val pitcherByDisplayName = pitcherOptions.associateBy { it.displayName }
    private var hasLoadedOverview = false
    private var hasLoadedAdvanced = false
    private var hasLoadedHeatMap = false

    // Initialize defaults from available options so UI starts with valid selections.
    private val _uiState = MutableStateFlow(
        PitchSenseUiState(
            selectedBatterId = batterOptions.firstOrNull()?.id.orEmpty(),
            selectedBatter = batterOptions.firstOrNull()?.displayName.orEmpty(),
            selectedPitcher = ""
        )
    )
    /** Read-only state stream observed by Compose. */
    val uiState: StateFlow<PitchSenseUiState> = _uiState.asStateFlow()

    /** Returns cached batter options for selector controls. */
    fun getBatterOptions(): List<String> = batterOptions.map { it.displayName }
    /** Returns cached pitcher options for selector controls. */
    fun getPitcherOptions(): List<String> = pitcherOptions.map { it.displayName }

    /** Loads overview data the first time the overview destination becomes visible. */
    fun onOverviewVisible() {
        if (!hasLoadedOverview) {
            loadOverview()
        }
    }

    /** Loads advanced stats the first time the advanced destination becomes visible. */
    fun onAdvancedVisible() {
        if (!hasLoadedAdvanced) {
            loadAdvanced()
        }
    }

    /** Loads the selected heat-map metric the first time the heat-map destination becomes visible. */
    fun onHeatMapVisible() {
        if (!hasLoadedHeatMap) {
            loadHeatMap()
        }
    }

    /** Applies a new batter selection and refreshes every batter-dependent screen section. */
    fun onBatterSelected(value: String) {
        val option = batterByDisplayName[value]
        _uiState.update {
            it.copy(
                selectedBatterId = option?.id.orEmpty(),
                selectedBatter = value
            )
        }
        loadOverview()
        if (hasLoadedAdvanced) loadAdvanced()
        if (hasLoadedHeatMap) loadHeatMap()
        _uiState.update { it.copy(appliedScenario = "", recommendedSequence = emptyList()) }
    }

    /** Applies a pitcher filter and reloads only the overview data that depends on that matchup. */
    fun onPitcherSelected(value: String) {
        val option = pitcherByDisplayName[value]
        _uiState.update {
            it.copy(
                selectedPitcherId = option?.id.orEmpty(),
                selectedPitcher = value
            )
        }
        loadOverview()
        _uiState.update { it.copy(appliedScenario = "", recommendedSequence = emptyList()) }
    }

    /** Stores the selected overview tab so the dashboard can preserve the active section. */
    fun onOverviewTabSelected(index: Int) {
        _uiState.update { it.copy(overviewTabIndex = index) }
    }

    /** Switches the active heat-map metric and fetches the corresponding zone grid. */
    fun onHeatMetricSelected(metric: String) {
        _uiState.update { it.copy(selectedHeatMetric = metric) }
        if (hasLoadedHeatMap) loadHeatMap()
    }

    /** Updates the editable pitch-count length before the user applies a new sequence scenario. */
    fun onPitchesToPredictChanged(value: String) {
        _uiState.update { it.copy(pitchesToPredict = value) }
    }

    /** Updates the editable ball count without triggering a sequence refresh yet. */
    fun onBallsChanged(value: String) {
        _uiState.update { it.copy(balls = value) }
    }

    /** Updates the editable strike count without triggering a sequence refresh yet. */
    fun onStrikesChanged(value: String) {
        _uiState.update { it.copy(strikes = value) }
    }

    /** Updates the editable out count used by the sequence recommendation form. */
    fun onOutsChanged(value: String) {
        _uiState.update { it.copy(outs = value) }
    }

    /** Updates the editable inning value used by the sequence recommendation form. */
    fun onInningChanged(value: String) {
        _uiState.update { it.copy(inning = value) }
    }

    /** Toggles whether the applied scenario should include a runner on first base. */
    fun onRunnerOnFirstChanged(value: Boolean) {
        _uiState.update { it.copy(runnerOnFirst = value) }
    }

    /** Toggles whether the applied scenario should include a runner on second base. */
    fun onRunnerOnSecondChanged(value: Boolean) {
        _uiState.update { it.copy(runnerOnSecond = value) }
    }

    /** Toggles whether the applied scenario should include a runner on third base. */
    fun onRunnerOnThirdChanged(value: Boolean) {
        _uiState.update { it.copy(runnerOnThird = value) }
    }

    /**
     * Sets how many times the pitcher has faced this batting order (1, 2, or 3).
     * Passing null clears the selection so the model uses its default bucket.
     */
    fun onTimesThroughChanged(value: Int?) {
        _uiState.update { it.copy(timesThrough = value) }
    }

    /**
     * Commits the current editable pitch-sequence inputs as the applied scenario
     * and triggers a fresh sequence load for those inputs.
     */
    fun onGenerateUpdatedSequence() {
        if (_uiState.value.selectedPitcherId.isBlank()) {
            _uiState.update { it.copy(appliedScenario = "", recommendedSequence = emptyList(), isPitchSequenceLoading = false) }
            return
        }
        _uiState.update {
            it.copy(
                generatedPitchesToPredict = it.pitchesToPredict,
                generatedBalls = it.balls,
                generatedStrikes = it.strikes,
                generatedOuts = it.outs,
                generatedInning = it.inning,
                generatedRunnerOnFirst = it.runnerOnFirst,
                generatedRunnerOnSecond = it.runnerOnSecond,
                generatedRunnerOnThird = it.runnerOnThird,
                generatedTimesThrough = it.timesThrough
            )
        }
        loadPitchSequence()
    }

    /** Loads overview cards for the current batter and optional pitcher matchup. */
    private fun loadOverview() {
        hasLoadedOverview = true
        val batterId = _uiState.value.selectedBatterId
        val pitcherId = _uiState.value.selectedPitcherId
        viewModelScope.launch {
            // Show demo data immediately so the screen renders without waiting for the network.
            val (fakeGeneral, fakePitcherSpecific) = fallback.overviewStats(batterId, pitcherId)
            _uiState.update {
                it.copy(overviewGeneral = fakeGeneral, overviewPitcherSpecific = fakePitcherSpecific, overviewError = false)
            }
            // Replace with live data if the API responds; mark offline only on failure.
            try {
                val (general, pitcherSpecific) = repository.overviewStats(batterId, pitcherId)
                _uiState.update {
                    it.copy(overviewGeneral = general, overviewPitcherSpecific = pitcherSpecific, overviewError = false, isOffline = false)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Overview fetch failed, using fallback data", e)
                _uiState.update { it.copy(isOffline = true) }
            }
        }
    }

    /** Loads all advanced-stat sections together so the advanced screen stays in sync. */
    private fun loadAdvanced() {
        hasLoadedAdvanced = true
        val batterId = _uiState.value.selectedBatterId
        viewModelScope.launch {
            // Show demo data immediately so the screen renders without waiting for the network.
            _uiState.update {
                it.copy(
                    advancedSummaryStats = fallback.advancedSummaryStats(batterId),
                    advancedDisciplineStats = fallback.advancedDisciplineStats(batterId),
                    advancedBattedBallStats = fallback.advancedBattedBallStats(batterId),
                    pitchTypeStats = fallback.pitchTypeStats(batterId),
                    advancedError = false
                )
            }
            // Replace with live data if the API responds; mark offline only on failure.
            try {
                val summary = repository.advancedSummaryStats(batterId)
                val discipline = repository.advancedDisciplineStats(batterId)
                val battedBall = repository.advancedBattedBallStats(batterId)
                val pitchType = repository.pitchTypeStats(batterId)
                _uiState.update {
                    it.copy(
                        advancedSummaryStats = summary,
                        advancedDisciplineStats = discipline,
                        advancedBattedBallStats = battedBall,
                        pitchTypeStats = pitchType,
                        advancedError = false,
                        isOffline = false
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Advanced stats fetch failed, using fallback data", e)
                _uiState.update { it.copy(isOffline = true) }
            }
        }
    }

    /** Loads the current metric's 5x5 heat-map grid for the selected batter. */
    private fun loadHeatMap() {
        hasLoadedHeatMap = true
        val batterId = _uiState.value.selectedBatterId
        val metric = _uiState.value.selectedHeatMetric
        viewModelScope.launch {
            // Show demo data immediately so the screen renders without waiting for the network.
            _uiState.update { it.copy(heatMap = fallback.heatMap(metric, batterId), heatMapError = false) }
            // Replace with live data if the API responds; mark offline only on failure.
            try {
                val grid = repository.heatMap(metric, batterId)
                _uiState.update { it.copy(heatMap = grid, heatMapError = false, isOffline = false) }
            } catch (e: Exception) {
                Log.w(TAG, "Heat map fetch failed, using fallback data", e)
                _uiState.update { it.copy(isOffline = true) }
            }
        }
    }

    /** Loads recommendations using the last applied sequence scenario snapshot. */
    private fun loadPitchSequence() {
        val state = _uiState.value
        val pitchesToPredict = state.generatedPitchesToPredict.toIntOrNull() ?: 3
        val balls = (state.generatedBalls.toIntOrNull() ?: 0).coerceIn(0, 3)
        val strikes = (state.generatedStrikes.toIntOrNull() ?: 0).coerceIn(0, 2)
        val outs = (state.generatedOuts.toIntOrNull() ?: 0).coerceIn(0, 2)
        viewModelScope.launch {
            // Clear the previous sequence and show a loading indicator while the new
            // recommendation loads. Unlike other screens, fake data here would be visible
            // and misleading since the sequence is the primary output the user is waiting for.
            _uiState.update { it.copy(appliedScenario = "", recommendedSequence = emptyList(), isPitchSequenceLoading = true) }
            try {
                val (scenario, sequence) = repository.recommendedSequence(
                    batterId = state.selectedBatterId,
                    pitcherId = state.selectedPitcherId,
                    pitchesToPredict = pitchesToPredict,
                    balls = balls,
                    strikes = strikes,
                    outs = outs,
                    inning = state.generatedInning,
                    runnerOnFirst = state.generatedRunnerOnFirst,
                    runnerOnSecond = state.generatedRunnerOnSecond,
                    runnerOnThird = state.generatedRunnerOnThird,
                    timesThrough = state.generatedTimesThrough
                )
                _uiState.update { it.copy(appliedScenario = scenario, recommendedSequence = sequence, isOffline = false, isPitchSequenceLoading = false) }
            } catch (e: Exception) {
                Log.w(TAG, "Pitch sequence fetch failed, using fallback data", e)
                _uiState.update { it.copy(isOffline = true, isPitchSequenceLoading = false) }
            }
        }
    }
}
