package com.example.pitchsense.data.repository

import com.example.pitchsense.BuildConfig
import com.example.pitchsense.data.remote.api.ApiClient

/** Creates the active repository implementation based on build config flags. */
object RepositoryProvider {
    /** Chooses fake or remote data source at app startup using BuildConfig flags. */
    fun create(): PitchSenseRepository {
        // Default local mode for UI development without backend dependency.
        if (!BuildConfig.USE_REMOTE_API) {
            return FakePitchSenseRepository()
        }

        // Remote mode uses Retrofit client configured with BuildConfig.API_BASE_URL.
        return RemotePitchSenseRepository(
            api = ApiClient.create()
        )
    }
}
