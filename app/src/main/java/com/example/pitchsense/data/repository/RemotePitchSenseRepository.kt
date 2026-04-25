package com.example.pitchsense.data.repository

import com.example.pitchsense.data.model.HeatLevel
import com.example.pitchsense.data.model.HeatMapCell
import com.example.pitchsense.data.model.PlayerOption
import com.example.pitchsense.data.model.PitchTypeStat
import com.example.pitchsense.data.model.SequenceRecommendation
import com.example.pitchsense.data.model.StatItem
import com.example.pitchsense.data.remote.api.PitchSenseApiService
import com.example.pitchsense.data.remote.model.AdvancedStatsResponseDto
import com.example.pitchsense.data.remote.model.GameSituationDto
import com.example.pitchsense.data.remote.model.HeatLevelDto
import com.example.pitchsense.data.remote.model.HeatMapResponseDto
import com.example.pitchsense.data.remote.model.PitchSequenceRequestDto
import java.util.Locale

/**
 * API-backed repository. Network or serialization failures propagate as exceptions
 * to the caller — fallback to demo data is handled by the ViewModel layer so the
 * offline state can be surfaced to the UI.
 *
 * Player rosters and situational-stat placeholders are sourced from
 * FakePitchSenseRepository because they are hardcoded client-side data, not API calls.
 */
class RemotePitchSenseRepository(
    private val api: PitchSenseApiService,
    // Supplies hardcoded roster lists and situational-stat placeholders (not a network fallback).
    private val staticData: FakePitchSenseRepository = FakePitchSenseRepository(),
    // Season pin keeps responses deterministic across app sessions/environments.
    private val season: Int = 2025
) : PitchSenseRepository {

    // Per-batter cache for the full advanced stats response — all four advanced methods
    // share this so one batter load triggers at most one API call.
    private var advancedCache: Pair<String, AdvancedStatsResponseDto>? = null

    // Per-(metric, batter) cache for the heatmap grid response.
    private var heatMapCache: Triple<String, String, HeatMapResponseDto>? = null

    override fun batterOptions(): List<PlayerOption> = staticData.batterOptions()

    override fun pitcherOptions(): List<PlayerOption> = staticData.pitcherOptions()

    override suspend fun overviewStats(batterId: String, pitcherId: String): Pair<List<StatItem>, List<StatItem>> {
        // API treats null pitcherId as "general-only" view.
        val pitcherParam = pitcherId.takeIf { it.isNotBlank() }
        val body = api.overviewStats(batterId = batterId, pitcherId = pitcherParam, season = season)
        return Pair(
            body.general.map { it.toStatItem() },
            body.pitcherSpecific.map { it.toStatItem() }
        )
    }

    override suspend fun advancedSummaryStats(batterId: String): List<StatItem> =
        fetchAdvancedMetrics(batterId).filter {
            it.title in setOf("xwOBA", "xBA", "xSLG", "Hard Hit%", "Barrel%")
        }

    override suspend fun advancedDisciplineStats(batterId: String): List<StatItem> =
        fetchAdvancedMetrics(batterId).filter {
            it.title in setOf("Chase%", "Zone Contact%", "Whiff%", "CSW%")
        }

    override suspend fun advancedBattedBallStats(batterId: String): List<StatItem> =
        fetchAdvancedMetrics(batterId).filter {
            it.title in setOf("Avg EV", "Max EV", "Sweet Spot%")
        }

    // Situational stats are hardcoded MVP placeholders — not backed by the API.
    override suspend fun advancedSituationalStats(batterId: String): List<StatItem> =
        staticData.advancedSituationalStats(batterId)

    override suspend fun pitchTypeStats(batterId: String): List<PitchTypeStat> {
        val body = fetchAdvancedResponse(batterId)
        return body.whiffByPitchType.map {
            PitchTypeStat(pitch = it.pitchType, whiff = formatPercent(it.whiffPct))
        }
    }

    override suspend fun heatMap(metric: String, batterId: String): List<List<HeatMapCell>> {
        val body = fetchHeatMapResponse(metric, batterId)
        return body.grid.map { row ->
            row.map { cell -> HeatMapCell(value = formatRate(cell.value), level = cell.level.toDomain()) }
        }
    }

    override suspend fun recommendedSequence(
        batterId: String,
        pitcherId: String,
        pitchesToPredict: Int,
        balls: Int,
        strikes: Int,
        outs: Int,
        inning: String,
        runnerOnFirst: Boolean,
        runnerOnSecond: Boolean,
        runnerOnThird: Boolean,
        timesThrough: Int?
    ): Pair<String, List<SequenceRecommendation>> {
        val body = api.recommendSequence(
            PitchSequenceRequestDto(
                batterId = batterId,
                pitcherId = pitcherId,
                pitchesToPredict = pitchesToPredict,
                gameSituation = GameSituationDto(
                    inning = inning.toIntOrNull() ?: 1,
                    balls = balls,
                    strikes = strikes,
                    outs = outs,
                    runnerOnFirst = runnerOnFirst,
                    runnerOnSecond = runnerOnSecond,
                    runnerOnThird = runnerOnThird
                ),
                timesThrough = timesThrough
            )
        )
        return Pair(
            body.appliedScenario,
            body.recommendations.map {
                SequenceRecommendation(
                    step = it.step,
                    pitchType = it.pitchType,
                    description = it.description,
                    effectiveness = "${it.effectivenessPct}%"
                )
            }
        )
    }

    /** Fetches and memoizes the advanced-stats payload so all advanced sections share one API call. */
    private suspend fun fetchAdvancedResponse(batterId: String): AdvancedStatsResponseDto {
        advancedCache?.let { (cachedBatter, response) ->
            if (cachedBatter == batterId) return response
        }
        return api.advancedStats(batterId = batterId, pitcherId = null, season = season)
            .also { advancedCache = Pair(batterId, it) }
    }

    /** Normalizes direct advanced metrics into the UI's shared stat-card model and formatting rules. */
    private suspend fun fetchAdvancedMetrics(batterId: String): List<StatItem> {
        val body = fetchAdvancedResponse(batterId)
        return body.directMetrics.map {
            val formattedValue = when {
                it.label.endsWith("%") -> formatPercent(it.value)
                it.label.contains("EV") -> "${formatOneDecimal(it.value)} mph"
                else -> formatRate(it.value)
            }
            StatItem(title = it.label, value = formattedValue)
        }
    }

    /** Fetches and validates a 5x5 heat-map response before it reaches the UI layer. */
    private suspend fun fetchHeatMapResponse(metric: String, batterId: String): HeatMapResponseDto {
        heatMapCache?.let { (cachedMetric, cachedBatter, response) ->
            if (cachedMetric == metric && cachedBatter == batterId) return response
        }
        val response = api.heatMap(batterId = batterId, metric = metric, season = season)
        // Grid must be exactly 5×5 — any other size indicates a backend contract violation.
        if (response.grid.size != 5 || response.grid.any { it.size != 5 }) {
            throw IllegalStateException("Heat map response is not a 5×5 grid")
        }
        return response.also { heatMapCache = Triple(metric, batterId, it) }
    }

    /** Formats batting-rate values with three decimals using baseball-style leading-zero suppression. */
    private fun formatRate(value: Double): String {
        val text = String.format(Locale.US, "%.3f", value)
        // Baseball rates are displayed like ".285" rather than "0.285".
        return if (value in 0.0..1.0) text.removePrefix("0") else text
    }

    /** Formats percentage-style metrics with one decimal place for consistent card display. */
    private fun formatPercent(value: Double): String = "${formatOneDecimal(value)}%"

    /** Formats generic numeric stats to one decimal place using a fixed US locale. */
    private fun formatOneDecimal(value: Double): String =
        String.format(Locale.US, "%.1f", value)

    /** Converts overview API stat rows into domain cards while preserving display-specific labels. */
    private fun com.example.pitchsense.data.remote.model.StatValueDto.toStatItem(): StatItem {
        val formattedValue = when {
            label.endsWith("%") -> formatPercent(value)
            label in setOf("BA", "OBP", "xwOBA", "xBA", "xSLG") -> formatRate(value)
            key == "hr" -> value.toInt().toString()
            else -> formatOneDecimal(value)
        }
        // "BA" is the only abbreviated label that gets a parenthetical expansion because
        // the overview UI displays it as "BA (Batting Avg)" for readability. Other stats
        // (OBP, xwOBA, etc.) are already self-explanatory abbreviations on the dashboard.
        return StatItem(
            title = "$label${if (label == "BA") " (Batting Avg)" else ""}",
            value = formattedValue,
            isPrimary = isPrimary
        )
    }

    /** Maps backend heat-level enums onto the UI color-bucket enum used by the heat map. */
    private fun HeatLevelDto.toDomain(): HeatLevel = when (this) {
        HeatLevelDto.OUTER -> HeatLevel.OUTER
        HeatLevelDto.ELITE -> HeatLevel.ELITE
        HeatLevelDto.GOOD -> HeatLevel.GOOD
        HeatLevelDto.BELOW_AVG -> HeatLevel.BELOW_AVG
        HeatLevelDto.WEAK -> HeatLevel.WEAK
    }
}
