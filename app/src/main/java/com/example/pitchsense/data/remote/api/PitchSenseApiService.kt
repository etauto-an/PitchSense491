package com.example.pitchsense.data.remote.api

import com.example.pitchsense.data.remote.model.AdvancedStatsResponseDto
import com.example.pitchsense.data.remote.model.HeatMapResponseDto
import com.example.pitchsense.data.remote.model.OverviewStatsResponseDto
import com.example.pitchsense.data.remote.model.PitchSequenceRequestDto
import com.example.pitchsense.data.remote.model.PitchSequenceResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Retrofit definition of all MVP backend endpoints consumed by the app. */
interface PitchSenseApiService {
    /** Overview cards for selected batter and optional pitcher split. */
    @GET("overview/stats")
    suspend fun overviewStats(
        @Query("batterId") batterId: String,
        @Query("pitcherId") pitcherId: String?,
        @Query("season") season: Int
    ): OverviewStatsResponseDto

    /** Advanced direct metrics and whiff-by-pitch-type table. */
    @GET("advanced/stats")
    suspend fun advancedStats(
        @Query("batterId") batterId: String,
        @Query("pitcherId") pitcherId: String?,
        @Query("season") season: Int
    ): AdvancedStatsResponseDto

    /** 5x5 batter heat map grid plus natural-language analysis text. */
    @GET("heatmap")
    suspend fun heatMap(
        @Query("batterId") batterId: String,
        @Query("metric") metric: String,
        @Query("season") season: Int
    ): HeatMapResponseDto

    /** Pitch sequence recommendation for the current game situation context. */
    @POST("pitch-sequence/recommend")
    suspend fun recommendSequence(@Body request: PitchSequenceRequestDto): PitchSequenceResponseDto
}
