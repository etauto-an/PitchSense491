package com.example.pitchsense.data.repository

import com.example.pitchsense.data.model.HeatLevel
import com.example.pitchsense.data.model.HeatMapCell
import com.example.pitchsense.data.model.PlayerOption
import com.example.pitchsense.data.model.PitchTypeStat
import com.example.pitchsense.data.model.SequenceRecommendation
import com.example.pitchsense.data.model.StatItem

/** Static/demo implementation used for local development and UI previews. */
class FakePitchSenseRepository : PitchSenseRepository {
    /** Returns the fixed batter list used by local development and screenshot-friendly demos. */
    override fun batterOptions(): List<PlayerOption> = listOf(
        PlayerOption("543807", "George Springer"),
        PlayerOption("680718", "Addison Barger"),
        PlayerOption("665489", "Vladimir Guerrero Jr."),
        PlayerOption("662139", "Daulton Varsho"),
        PlayerOption("672386", "Alejandro Kirk"),
        PlayerOption("672960", "Kazuma Okamoto"),
        PlayerOption("660821", "Jesús Sánchez"),
        PlayerOption("676391", "Ernie Clement"),
        PlayerOption("665926", "Andrés Giménez")
    )

    /** Returns the fixed pitcher list used by local development and screenshot-friendly demos. */
    override fun pitcherOptions(): List<PlayerOption> = listOf(
        PlayerOption("808967", "Yoshinobu Yamamoto"),
        PlayerOption("660271", "Shohei Ohtani"),
        PlayerOption("607192", "Tyler Glasnow"),
        PlayerOption("605483", "Blake Snell"),
        PlayerOption("808963", "Roki Sasaki"),
        PlayerOption("689017", "Landon Knack"),
        PlayerOption("676272", "Bobby Miller"),
        PlayerOption("689981", "River Ryan"),
        PlayerOption("694813", "Gavin Stone"),
        PlayerOption("686218", "Emmet Sheehan"),
        PlayerOption("680736", "Justin Wrobleski"),
        PlayerOption("656945", "Tanner Scott"),
        PlayerOption("595014", "Blake Treinen"),
        PlayerOption("681911", "Alex Vesia"),
        PlayerOption("660813", "Brusdar Graterol"),
        PlayerOption("592779", "Brock Stewart"),
        PlayerOption("676508", "Ben Casparius"),
        PlayerOption("676263", "Jack Dreyer"),
        PlayerOption("801434", "Paul Gervase"),
        PlayerOption("683618", "Edgardo Henriquez"),
        PlayerOption("669165", "Kyle Hurt"),
        PlayerOption("694361", "Will Klein"),
        PlayerOption("691947", "Ronan Kopp"),
        PlayerOption("621242", "Edwin Díaz")
    )

    /** Supplies stable overview cards so the dashboard remains usable without a backend. */
    override suspend fun overviewStats(batterId: String, pitcherId: String): Pair<List<StatItem>, List<StatItem>> {
        val general = listOf(
            StatItem(title = "BA (Batting Avg)", value = ".285", isPrimary = true),
            StatItem(title = "K% (Strikeout)", value = "24.3%", isPrimary = true),
            StatItem(title = "BB% (Walk)", value = "11.2%", isPrimary = true),
            StatItem(title = "OBP", value = ".362"),
            StatItem(title = "HR", value = "37")
        )
        val pitcherSpecific = if (pitcherId.isBlank()) emptyList() else listOf(
            StatItem(title = "BA (Batting Avg)", value = ".318", isPrimary = true),
            StatItem(title = "K% (Strikeout)", value = "19.2%", isPrimary = true),
            StatItem(title = "BB% (Walk)", value = "10.7%", isPrimary = true),
            StatItem(title = "OBP", value = ".385"),
            StatItem(title = "HR", value = "5")
        )
        return Pair(general, pitcherSpecific)
    }

    /** Supplies the fixed expected-outcome metrics shown on the advanced stats screen. */
    override suspend fun advancedSummaryStats(batterId: String): List<StatItem> = listOf(
        StatItem("xwOBA", ".372"),
        StatItem("xBA", ".291"),
        StatItem("xSLG", ".536"),
        StatItem("Hard Hit%", "45.2%"),
        StatItem("Barrel%", "12.8%")
    )

    /** Supplies the fixed swing-and-miss discipline metrics for offline mode. */
    override suspend fun advancedDisciplineStats(batterId: String): List<StatItem> = listOf(
        StatItem("Chase%", "27.1%"),
        StatItem("Zone Contact%", "84.6%"),
        StatItem("Whiff%", "22.9%"),
        StatItem("CSW%", "28.7%")
    )

    /** Supplies the fixed exit-velocity and batted-ball profile metrics for offline mode. */
    override suspend fun advancedBattedBallStats(batterId: String): List<StatItem> = listOf(
        StatItem("Avg EV", "91.8 mph"),
        StatItem("Max EV", "113.2 mph"),
        StatItem("Sweet Spot%", "34.4%")
    )

    /** Supplies placeholder situational splits until the backend exposes a real endpoint. */
    override suspend fun advancedSituationalStats(batterId: String): List<StatItem> = listOf(
        StatItem("RISP BA", ".307"),
        StatItem("2-Strike BA", ".211"),
        StatItem("Ahead in Count", ".356"),
        StatItem("High Leverage OPS", ".901")
    )

    /** Supplies a small fixed whiff table so the pitch-type section can render offline. */
    override suspend fun pitchTypeStats(batterId: String): List<PitchTypeStat> = listOf(
        PitchTypeStat("4-Seam Fastball", "18.5%"),
        PitchTypeStat("Slider", "35.8%"),
        PitchTypeStat("Changeup", "28.3%")
    )

    /** Supplies deterministic 5x5 heat-map data for each supported metric in offline mode. */
    override suspend fun heatMap(metric: String, batterId: String): List<List<HeatMapCell>> {
        // Local aliases keep grid declarations compact and readable.
        val outer = HeatLevel.OUTER
        val elite = HeatLevel.ELITE
        val good = HeatLevel.GOOD
        val belowAvg = HeatLevel.BELOW_AVG
        val weak = HeatLevel.WEAK

        // Provide a dedicated SLG map; all other metrics use the default table.
        return when (metric) {
            "SLG" -> listOf(
                listOf(HeatMapCell(".292", outer), HeatMapCell(".301", outer), HeatMapCell(".309", outer), HeatMapCell(".304", outer), HeatMapCell(".296", outer)),
                listOf(HeatMapCell(".315", outer), HeatMapCell(".334", good), HeatMapCell(".368", elite), HeatMapCell(".342", good), HeatMapCell(".307", outer)),
                listOf(HeatMapCell(".318", outer), HeatMapCell(".338", good), HeatMapCell(".381", elite), HeatMapCell(".356", elite), HeatMapCell(".311", outer)),
                listOf(HeatMapCell(".303", outer), HeatMapCell(".262", belowAvg), HeatMapCell(".329", good), HeatMapCell(".227", weak), HeatMapCell(".298", outer)),
                listOf(HeatMapCell(".287", outer), HeatMapCell(".294", outer), HeatMapCell(".302", outer), HeatMapCell(".297", outer), HeatMapCell(".289", outer))
            )
            else -> listOf(
                listOf(HeatMapCell(".288", outer), HeatMapCell(".295", outer), HeatMapCell(".304", outer), HeatMapCell(".299", outer), HeatMapCell(".287", outer)),
                listOf(HeatMapCell(".309", outer), HeatMapCell(".332", good), HeatMapCell(".351", elite), HeatMapCell(".339", good), HeatMapCell(".303", outer)),
                listOf(HeatMapCell(".313", outer), HeatMapCell(".346", good), HeatMapCell(".372", elite), HeatMapCell(".357", elite), HeatMapCell(".316", outer)),
                listOf(HeatMapCell(".298", outer), HeatMapCell(".268", belowAvg), HeatMapCell(".328", good), HeatMapCell(".252", belowAvg), HeatMapCell(".291", outer)),
                listOf(HeatMapCell(".286", outer), HeatMapCell(".292", outer), HeatMapCell(".297", outer), HeatMapCell(".289", outer), HeatMapCell(".284", outer))
            )
        }
    }

    /** Builds a canned recommendation sequence from the current count and base-state context. */
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
        // Derive leverage signals from inning/outs/base-runner state.
        val inningNumber = inning.toIntOrNull() ?: 1
        val runnersOn = listOf(runnerOnFirst, runnerOnSecond, runnerOnThird).count { it }
        val isHighLeverage = (inningNumber >= 7 && runnersOn >= 1) || (outs == 2 && runnersOn >= 2)

        // Choose a baseline 3-pitch script based on the most important current context.
        val (scenario, fullSequence) = when {
            strikes == 2 -> "Two-Strike Putaway" to listOf(
                SequenceRecommendation(1, "Slider", "Back-foot, lower third — chase for strike three", "84%"),
                SequenceRecommendation(2, "Splitter", "Bottom edge — late drop below zone", "80%"),
                SequenceRecommendation(3, "4-Seam Fastball", "Top rail, glove side — finish above bat path", "76%")
            )
            balls >= 3 -> "Hitter Count Damage Control" to listOf(
                SequenceRecommendation(1, "4-Seam Fastball", "Outer third — must-strike location", "81%"),
                SequenceRecommendation(2, "Cutter", "Glove-side edge — weak-contact lane", "77%"),
                SequenceRecommendation(3, "Changeup", "Bottom edge — disrupt timing", "73%")
            )
            isHighLeverage -> "High Leverage" to listOf(
                SequenceRecommendation(1, "Sinker", "In on hands — ground-ball intent", "83%"),
                SequenceRecommendation(2, "Slider", "Front-door, middle third — freeze or soft roll-over", "79%"),
                SequenceRecommendation(3, "Changeup", "Below zone, arm side — avoid lift with runners on", "74%")
            )
            runnerOnThird && outs < 2 -> "Runner on 3rd, <2 Outs" to listOf(
                SequenceRecommendation(1, "4-Seam Fastball", "Up and in — prevent sac fly contact", "80%"),
                SequenceRecommendation(2, "Slider", "Back-foot, lower third — whiff or weak infield contact", "78%"),
                SequenceRecommendation(3, "Curveball", "Bottom rail — collapse launch angle", "72%")
            )
            else -> "Neutral Situation" to listOf(
                SequenceRecommendation(1, "Slider", "Low and away — target weak zone", "82%"),
                SequenceRecommendation(2, "Slider", "Down and in — exploit low BA zone", "78%"),
                SequenceRecommendation(3, "Changeup", "Down and away — induce weak contact", "75%")
            )
        }

        val count = pitchesToPredict.coerceIn(1, fullSequence.size)
        return Pair(scenario, fullSequence.take(count))
    }
}
