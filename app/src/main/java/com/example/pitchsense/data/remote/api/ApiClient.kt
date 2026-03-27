package com.example.pitchsense.data.remote.api

import com.example.pitchsense.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Factory for creating the Retrofit API service used by the remote repository. */
object ApiClient {
    /**
     * Builds a Retrofit client from BuildConfig defaults unless a custom base URL is supplied.
     * `baseUrl` must include a trailing slash to satisfy Retrofit.
     */
    fun create(baseUrl: String = BuildConfig.API_BASE_URL): PitchSenseApiService {
        // BASIC level logs method/path/status without full payload bodies.
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Short timeouts ensure offline fallback is reached quickly rather than blocking
        // for OkHttp's default 10-second window before the catch path fires.
        val client = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        // KotlinJsonAdapterFactory is required for Moshi to reflectively serialize
        // Kotlin data classes. Without it, adapter creation throws before any HTTP
        // request is made and every call silently falls back to fake data.
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PitchSenseApiService::class.java)
    }
}
