package com.gaao.app.data.lyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class LyricsApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "LyricsApiClient"
        private const val BASE_URL = "https://lrclib.net/api/get"
        private const val USER_AGENT = "GaaO-Android-Companion/1.0 (https://github.com/sattu/GaaO)"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Fetches track metadata and lyrics from LRCLIB.
     * Returns [LrclibResponse] on success, null if not found (404),
     * or throws [IOException] on network failure.
     */
    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        durationSeconds: Long? = null,
        albumName: String? = null
    ): LrclibResponse? = withContext(Dispatchers.IO) {
        val urlBuilder = BASE_URL.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalStateException("Invalid base URL: $BASE_URL")

        urlBuilder.addQueryParameter("track_name", trackName)
        urlBuilder.addQueryParameter("artist_name", artistName)

        if (durationSeconds != null && durationSeconds > 0) {
            urlBuilder.addQueryParameter("duration", durationSeconds.toString())
        }

        if (!albumName.isNullOrBlank()) {
            urlBuilder.addQueryParameter("album_name", albumName)
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val bodyString = response.body?.string()
                        if (bodyString.isNullOrBlank()) {
                            null
                        } else {
                            json.decodeFromString<LrclibResponse>(bodyString)
                        }
                    }
                    404 -> {
                        Log.d(TAG, "Lyrics not found (404) for: $trackName by $artistName")
                        null
                    }
                    429 -> {
                        Log.w(TAG, "Rate limited by LRCLIB (429)")
                        throw IOException("Rate limited by LRCLIB API (429)")
                    }
                    else -> {
                        Log.w(TAG, "Unexpected LRCLIB HTTP response: ${response.code}")
                        throw IOException("HTTP error from LRCLIB: ${response.code}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error fetching lyrics for $trackName by $artistName", e)
            throw e
        }
    }
}
