package com.example.pitchsense.data.repository

import com.example.pitchsense.data.model.HeatMapCell
import com.example.pitchsense.data.model.PlayerOption
import com.example.pitchsense.data.model.PitchTypeStat
import com.example.pitchsense.data.model.SequenceRecommendation
import com.example.pitchsense.data.model.StatItem

/** Contract for data used by PitchSense screens. */
interface PitchSenseRepository {
    /** Returns selectable batters for the UI dropdown (stable ID + display name). */
    fun batterOptions(): List<PlayerOption>
    /** Returns selectable pitchers for the UI dropdown (stable ID + display name). */
    fun pitcherOptions(): List<PlayerOption>

    /**
     * Returns general and pitcher-specific overview stats in a single fetch.
     * When pitcher is blank, pitcherSpecific will be empty.
     */
    suspend fun overviewStats(batterId: String, pitcherId: String): Pair<List<StatItem>, List<StatItem>>

    /** Returns expected-outcome summary metrics (quality of contact/profile). */
    suspend fun advancedSummaryStats(batterId: String): List<StatItem>
    /** Returns swing decision and contact quality discipline metrics. */
    suspend fun advancedDisciplineStats(batterId: String): List<StatItem>
    /** Returns exit velocity and batted-ball profile metrics. */
    suspend fun advancedBattedBallStats(batterId: String): List<StatItem>
    /** Returns context-specific performance splits. */
    suspend fun advancedSituationalStats(batterId: String): List<StatItem>
    /** Returns batter performance broken down by pitch type. */
    suspend fun pitchTypeStats(batterId: String): List<PitchTypeStat>

    /** Returns a zone matrix for the selected metric (for strike-zone heat map rendering). */
    suspend fun heatMap(metric: String, batterId: String): List<List<HeatMapCell>>

    /**
     * Returns the applied scenario label and recommendations for the current game state.
     * First element is the scenario name, second is the ordered recommendation list.
     */
    suspend fun recommendedSequence(
        batterId: String,
        pitcherId: String,
        pitchesToPredict: Int,
        balls: Int,
        strikes: Int,
        outs: Int,
        inning: String,
        runnerOnFirst: Boolean,
        runnerOnSecond: Boolean,
        runnerOnThird: Boolean
    ): Pair<String, List<SequenceRecommendation>>
}
