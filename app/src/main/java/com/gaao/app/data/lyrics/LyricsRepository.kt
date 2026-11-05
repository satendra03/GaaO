package com.gaao.app.data.lyrics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.gaao.app.data.model.LyricsResult
import java.io.IOException
import java.net.UnknownHostException

class LyricsRepository(
    private val context: Context,
    private val apiClient: LyricsApiClient = LyricsApiClient()
) {
    companion object {
        private const val TAG = "LyricsRepository"

        @Volatile
        var instance: LyricsRepository? = null
            private set

        fun initialize(context: Context): LyricsRepository {
            return instance ?: synchronized(this) {
                instance ?: LyricsRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    // In-memory cache for current track to avoid redundant network requests
    private var cachedTrackKey: String? = null
    private var cachedResult: LyricsResult? = null

    suspend fun getLyrics(
        trackTitle: String?,
        artist: String?,
        durationMs: Long = 0L,
        album: String? = null
    ): LyricsResult {
        if (trackTitle.isNullOrBlank() || artist.isNullOrBlank()) {
            return LyricsResult.Idle
        }

        // Cache key based on title, artist, and duration (approximate)
        val durationSec = if (durationMs > 0) durationMs / 1000L else 0L
        val trackKey = "$trackTitle|$artist|$durationSec"

        if (trackKey == cachedTrackKey && cachedResult != null) {
            return cachedResult!!
        }

        if (!isNetworkAvailable()) {
            Log.d(TAG, "Network unavailable, cannot fetch lyrics")
            return LyricsResult.Offline
        }

        return try {
            val response = apiClient.getLyrics(
                trackName = trackTitle,
                artistName = artist,
                durationSeconds = if (durationSec > 0) durationSec else null,
                albumName = album
            )

            val result: LyricsResult = if (response == null || response.instrumental) {
                LyricsResult.Unavailable
            } else if (!response.syncedLyrics.isNullOrBlank()) {
                val parsedLines = LrcParser.parse(response.syncedLyrics)
                if (parsedLines.isNotEmpty()) {
                    LyricsResult.Success(
                        lines = parsedLines,
                        isSynced = true,
                        trackName = response.trackName ?: trackTitle,
                        artistName = response.artistName ?: artist
                    )
                } else {
                    LyricsResult.Unavailable
                }
            } else {
                // Unsynchronized lyrics only — per SRS FR-03 & TDD 2.2, reject unsynchronized lyrics for sync engine
                LyricsResult.Unavailable
            }

            cachedTrackKey = trackKey
            cachedResult = result
            result
        } catch (e: UnknownHostException) {
            Log.w(TAG, "Unknown host (device offline)", e)
            LyricsResult.Offline
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching lyrics", e)
            LyricsResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching lyrics", e)
            LyricsResult.Error("Unexpected error: ${e.message}")
        }
    }

    fun clearCache() {
        cachedTrackKey = null
        cachedResult = null
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

