package com.gaao.app.data.media

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState as AndroidPlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.gaao.app.data.model.MediaPlaybackInfo
import com.gaao.app.data.model.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaSessionRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MediaSessionRepository"

        @Volatile
        var instance: MediaSessionRepository? = null
            private set

        fun initialize(context: Context): MediaSessionRepository {
            return instance ?: synchronized(this) {
                instance ?: MediaSessionRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    private var activeController: MediaController? = null
    private var isListenerRegistered = false

    private val _playbackInfo = MutableStateFlow<MediaPlaybackInfo?>(null)
    val playbackInfo: StateFlow<MediaPlaybackInfo?> = _playbackInfo.asStateFlow()

    private val _statusMessage = MutableStateFlow("Waiting for notification listener...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: AndroidPlaybackState?) {
            Log.d(TAG, "Playback state changed: ${state?.state}")
            updatePlaybackInfo(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.d(TAG, "Metadata changed: ${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}")
            updatePlaybackInfo(activeController)
        }

        override fun onSessionDestroyed() {
            Log.d(TAG, "Media session destroyed")
            detachActiveController()
            refreshActiveSessions()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            Log.d(TAG, "Active sessions changed. Count: ${controllers?.size ?: 0}")
            evaluateSessions(controllers)
        }

    fun onNotificationListenerConnected(listener: MediaNotificationListener) {
        Log.d(TAG, "Notification listener connected")
        _statusMessage.value = "Notification listener connected"
        registerSessionsChangedListener()
        refreshActiveSessions()
    }

    fun onNotificationListenerDisconnected() {
        Log.d(TAG, "Notification listener disconnected")
        _statusMessage.value = "Notification listener disconnected"
        unregisterSessionsChangedListener()
        detachActiveController()
    }

    fun refreshActiveSessions() {
        if (!MediaNotificationListener.isPermissionGranted(context)) {
            _statusMessage.value = "Notification listener permission not granted"
            detachActiveController()
            return
        }

        val componentName = MediaNotificationListener.getComponentName(context)
        try {
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            evaluateSessions(controllers)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading active sessions", e)
            _statusMessage.value = "Permission required: grant Notification Access"
            detachActiveController()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying active sessions", e)
            _statusMessage.value = "Error reading media sessions: ${e.message}"
            detachActiveController()
        }
    }

    private fun registerSessionsChangedListener() {
        if (isListenerRegistered || mediaSessionManager == null) return
        val componentName = MediaNotificationListener.getComponentName(context)
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                componentName,
                mainHandler
            )
            isListenerRegistered = true
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot register sessions changed listener: permission missing", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register sessions listener", e)
        }
    }

    private fun unregisterSessionsChangedListener() {
        if (!isListenerRegistered || mediaSessionManager == null) return
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering sessions listener", e)
        } finally {
            isListenerRegistered = false
        }
    }

    private fun evaluateSessions(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            Log.d(TAG, "No active media sessions found")
            detachActiveController()
            _statusMessage.value = "No active media sessions found"
            return
        }

        // Prioritize:
        // 1. Supported apps that are currently PLAYING
        // 2. Supported apps that are PAUSED or BUFFERING
        // 3. Any session that is currently PLAYING
        // 4. Any other session
        val supportedPlaying = controllers.firstOrNull {
            SupportedMusicApps.isSupported(it.packageName) &&
                    it.playbackState?.state == AndroidPlaybackState.STATE_PLAYING
        }

        val supportedOther = controllers.firstOrNull {
            SupportedMusicApps.isSupported(it.packageName)
        }

        val anyPlaying = controllers.firstOrNull {
            it.playbackState?.state == AndroidPlaybackState.STATE_PLAYING
        }

        val chosen = supportedPlaying ?: supportedOther ?: anyPlaying ?: controllers.firstOrNull()

        if (chosen == null) {
            detachActiveController()
            _statusMessage.value = "No relevant media session found"
            return
        }

        if (activeController?.sessionToken != chosen.sessionToken) {
            Log.d(TAG, "Switching active controller to ${chosen.packageName}")
            attachController(chosen)
        } else {
            // Already attached, update playback info
            updatePlaybackInfo(activeController)
        }
    }

    private fun attachController(controller: MediaController) {
        detachActiveController()
        activeController = controller
        controller.registerCallback(controllerCallback, mainHandler)
        updatePlaybackInfo(controller)
    }

    private fun detachActiveController() {
        activeController?.let {
            try {
                it.unregisterCallback(controllerCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering controller callback", e)
            }
        }
        activeController = null
        _playbackInfo.value = null
    }

    private fun updatePlaybackInfo(controller: MediaController?) {
        if (controller == null) {
            _playbackInfo.value = null
            return
        }

        val metadata = controller.metadata
        val state = controller.playbackState
        val pkg = controller.packageName

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val playbackState = when (state?.state) {
            AndroidPlaybackState.STATE_PLAYING -> PlaybackState.PLAYING
            AndroidPlaybackState.STATE_PAUSED -> PlaybackState.PAUSED
            AndroidPlaybackState.STATE_STOPPED -> PlaybackState.STOPPED
            AndroidPlaybackState.STATE_BUFFERING,
            AndroidPlaybackState.STATE_CONNECTING -> PlaybackState.BUFFERING
            AndroidPlaybackState.STATE_NONE -> PlaybackState.NONE
            else -> PlaybackState.IDLE
        }

        val position = state?.position ?: 0L
        val speed = state?.playbackSpeed ?: 1.0f
        val lastUpdateTime = state?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime()

        val info = MediaPlaybackInfo(
            trackTitle = title,
            artist = artist,
            album = album,
            durationMs = if (duration > 0) duration else 0L,
            positionMs = if (position > 0) position else 0L,
            playbackSpeed = if (speed > 0f) speed else 1.0f,
            playbackState = playbackState,
            packageName = pkg,
            appDisplayName = SupportedMusicApps.getDisplayName(pkg),
            lastPositionUpdateTimeMs = lastUpdateTime
        )

        _playbackInfo.value = info
        _statusMessage.value = "Monitoring: ${info.appDisplayName}"
    }
}

