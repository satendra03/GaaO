package com.gaao.app.data.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaNotificationListener : NotificationListenerService() {

    companion object {
        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, MediaNotificationListener::class.java)
        }

        fun isPermissionGranted(context: Context): Boolean {
            return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }

        fun createSettingsIntent(): Intent {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
        MediaSessionRepository.instance?.onNotificationListenerConnected(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isListenerConnected.value = false
        MediaSessionRepository.instance?.onNotificationListenerDisconnected()
    }
}

