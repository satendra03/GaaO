package com.gaao.app

import android.app.Application
import com.gaao.app.data.lyrics.LyricsRepository
import com.gaao.app.data.media.MediaSessionRepository

class GaaOApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MediaSessionRepository.initialize(this)
        LyricsRepository.initialize(this)
    }
}

