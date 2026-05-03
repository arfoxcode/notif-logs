package com.navre.notiflogs.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.data.RuntimeLogger

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action.orEmpty()
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_PRESENT,
            ACTION_RESTART_WATCHDOG,
            ACTION_HEALTH_CHECK,
            ACTION_QUICKBOOT -> {
                val prefs = AppPrefs(context)
                if (action == ACTION_HEALTH_CHECK) prefs.lastHeartbeatAt = System.currentTimeMillis()
                WatchdogService.scheduleHealthCheck(context)
                runCatching { WatchdogService.start(context) }
                    .onSuccess { RuntimeLogger.i(context, TAG, "Watchdog dimulai dari broadcast: $action") }
                    .onFailure { RuntimeLogger.e(context, TAG, "Gagal mulai watchdog dari broadcast: $action", it) }
            }
            else -> Unit
        }
    }

    companion object {
        const val ACTION_RESTART_WATCHDOG = "com.navre.notiflogs.RESTART_WATCHDOG"
        const val ACTION_HEALTH_CHECK = "com.navre.notiflogs.HEALTH_CHECK"
        const val ACTION_QUICKBOOT = "android.intent.action.QUICKBOOT_POWERON"
        private const val TAG = "BootReceiver"
    }
}
