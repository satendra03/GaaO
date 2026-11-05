package com.gaao.app

import com.gaao.app.data.media.SupportedMusicApps
import com.gaao.app.data.model.MediaPlaybackInfo
import com.gaao.app.data.model.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaDetectionTest {

    @Test
    fun testSupportedMusicAppsIdentification() {
        assertTrue(SupportedMusicApps.isSupported(SupportedMusicApps.PACKAGE_YOUTUBE_MUSIC))
        assertTrue(SupportedMusicApps.isSupported(SupportedMusicApps.PACKAGE_SPOTIFY))
        assertFalse(SupportedMusicApps.isSupported("com.unknown.player"))
        assertFalse(SupportedMusicApps.isSupported(null))

        assertEquals("YouTube Music", SupportedMusicApps.getDisplayName(SupportedMusicApps.PACKAGE_YOUTUBE_MUSIC))
        assertEquals("Spotify", SupportedMusicApps.getDisplayName(SupportedMusicApps.PACKAGE_SPOTIFY))
        assertEquals("com.random.app", SupportedMusicApps.getDisplayName("com.random.app"))
        assertEquals("Unknown", SupportedMusicApps.getDisplayName(null))
    }

    @Test
    fun testMediaPlaybackInfoDefaults() {
        val info = MediaPlaybackInfo()
        assertEquals(PlaybackState.NONE, info.playbackState)
        assertEquals(0L, info.positionMs)
        assertEquals(0L, info.durationMs)
        assertEquals(1.0f, info.playbackSpeed, 0.001f)
    }
}

