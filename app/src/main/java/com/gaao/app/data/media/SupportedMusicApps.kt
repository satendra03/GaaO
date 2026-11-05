package com.gaao.app.data.media

object SupportedMusicApps {
    const val PACKAGE_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"
    const val PACKAGE_SPOTIFY = "com.spotify.music"

    private val SUPPORTED_PACKAGES = mapOf(
        PACKAGE_YOUTUBE_MUSIC to "YouTube Music",
        PACKAGE_SPOTIFY to "Spotify"
    )

    fun isSupported(packageName: String?): Boolean {
        return packageName != null && SUPPORTED_PACKAGES.containsKey(packageName)
    }

    fun getDisplayName(packageName: String?): String {
        if (packageName == null) return "Unknown"
        return SUPPORTED_PACKAGES[packageName] ?: packageName
    }
}

