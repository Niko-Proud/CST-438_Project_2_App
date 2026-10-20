package com.example.recallapp.data
import com.example.recallapp.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    /**
     * Server address from build.gradle.kts (API_BASE_URL). Debug builds use 10.0.2.2 by default,
     * which is the computer running the emulator. Change API_BASE_URL in local.properties to use another server.
     */
    private val BASE_URL = BuildConfig.API_BASE_URL.trimEnd('/') + "/api/v1/"

    val instance: BrainwormApiService by lazy { create(BASE_URL) }

    /** Builds the API client for any base URL. Tests use this with a MockWebServer address. */
    fun create(baseUrl: String): BrainwormApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(BrainwormApiService::class.java)
    }
}
