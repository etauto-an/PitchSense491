package com.example.pitchsense.data.remote.model

/**
 * Moshi-deserialized DTOs that mirror the PitchSense backend API contract.
 *
 * These types are used exclusively for JSON deserialization and are distinct from the
 * domain models in PitchSenseModels.kt. RemotePitchSenseRepository is responsible for
 * converting DTOs into domain models before they reach the UI layer.
 *
 * Each class corresponds to a request or response shape defined in openapi.yaml /
 * docs/backend-api-contract.md.
 */

/** One stat row from the overview endpoint. `isPrimary` controls card emphasis after mapping. */
data class StatValueDto(
    val key: String,
    val label: String,
    val value: Double,
    val isPrimary: Boolean = false
)

/** Full response from GET /overview/stats — general season stats plus optional pitcher split. */
data class OverviewStatsResponseDto(
    val general: List<StatValueDto>,
    val pitcherSpecific: List<StatValueDto>
)

/** One Statcast metric row returned by the advanced stats endpoint (e.g. xwOBA, Barrel%). */
data class DirectMetricDto(
    val key: String,
    val label: String,
    val value: Double
)

/** Whiff rate for a single pitch type in the advanced stats response. */
data class WhiffByPitchTypeDto(
    val pitchType: String,
    val whiffPct: Double
)

/** Full response from GET /advanced/stats — direct Statcast metrics plus pitch-type breakdown. */
data class AdvancedStatsResponseDto(
    val directMetrics: List<DirectMetricDto>,
    val whiffByPitchType: List<WhiffByPitchTypeDto>
)

/**
 * Performance bucket for one heat map cell as returned by the backend.
 * Mapped to the domain HeatLevel enum before UI rendering.
 */
enum class HeatLevelDto {
    OUTER,
    ELITE,
    GOOD,
    BELOW_AVG,
    WEAK
}

/** One cell in the 5×5 zone grid, with a raw numeric value and its performance tier. */
data class HeatMapCellDto(
    val value: Double,
    val level: HeatLevelDto
)

/**
 * Full response from GET /heatmap.
 * The grid is always 5 rows × 5 columns — any other size is treated as a contract violation.
 */
data class HeatMapResponseDto(
    val metric: String,
    val grid: List<List<HeatMapCellDto>>
)

/** Game situation context sent as part of a pitch sequence recommendation request. */
data class GameSituationDto(
    val inning: Int,
    val balls: Int,
    val strikes: Int,
    val outs: Int,
    val runnerOnFirst: Boolean,
    val runnerOnSecond: Boolean,
    val runnerOnThird: Boolean
)

/** Request body for POST /pitch-sequence/recommend. */
data class PitchSequenceRequestDto(
    val batterId: String,
    val pitcherId: String,
    val pitchesToPredict: Int,
    val gameSituation: GameSituationDto
)

/** One recommended pitch step in the sequence response (e.g. step 1: Slider, low and away). */
data class RecommendationDto(
    val step: Int,
    val pitchType: String,
    val description: String,
    val effectivenessPct: Int
)

/** Full response from POST /pitch-sequence/recommend — scenario label plus ordered pitch steps. */
data class PitchSequenceResponseDto(
    val appliedScenario: String,
    val recommendations: List<RecommendationDto>
)
