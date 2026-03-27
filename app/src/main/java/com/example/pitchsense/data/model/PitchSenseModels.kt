package com.example.pitchsense.data.model

/** Selectable player option with stable API identifier and display label. */
data class PlayerOption(
    val id: String,
    val displayName: String
)

/**
 * Generic labeled stat value used across dashboard sections.
 * @param isPrimary Whether to render this stat with highlighted card styling (blue-tinted
 *                  background, no border) rather than the default plain white card.
 */
data class StatItem(
    val title: String,
    val value: String,
    val isPrimary: Boolean = false
)

/** Per-pitch-type performance snapshot for a batter. */
data class PitchTypeStat(
    val pitch: String,
    val whiff: String
)

/** One step in a suggested pitch sequence. */
data class SequenceRecommendation(
    val step: Int,
    val pitchType: String,
    val description: String,
    val effectiveness: String
)

/**
 * Relative quality buckets used to color heat map cells.
 * OUTER marks the eight border zones outside the central 3×3 strike zone in the 5×5 grid
 * — they're included for context but rendered with a muted color regardless of their value.
 */
enum class HeatLevel {
    OUTER,
    ELITE,
    GOOD,
    BELOW_AVG,
    WEAK
}

/** One heat-map zone containing a metric value and quality tier. */
data class HeatMapCell(
    val value: String,
    val level: HeatLevel
)
