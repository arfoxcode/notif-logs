package com.navre.notiflogs.service

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.data.NotificationRepository
import com.navre.notiflogs.data.RuntimeLogger
import com.navre.notiflogs.util.L

class NotificationCaptureService : NotificationListenerService() {
    private val repo by lazy { NotificationRepository(this) }
    private val prefs by lazy { AppPrefs(this) }

    override fun onCreate() {
        super.onCreate()
        RuntimeLogger.i(this, TAG, L.t("NotificationCaptureService dibuat", "NotificationCaptureService created"))
        WatchdogService.scheduleHealthCheck(this)
        runCatching { WatchdogService.start(this) }
            .onFailure { RuntimeLogger.e(this, TAG, "Gagal mulai watchdog dari listener", it) }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        prefs.lastListenerConnectedAt = System.currentTimeMillis()
        WatchdogService.scheduleHealthCheck(this)
        RuntimeLogger.i(this, TAG, L.t("Notification listener tersambung", "Notification listener connected"))
        runCatching {
            var inserted = 0
            var scanned = 0
            activeNotifications?.forEach { sbn ->
                scanned++
                if (repo.saveFromStatusBarNotification(sbn)) inserted++
            }
            RuntimeLogger.i(this, TAG, "Sync active notifications selesai: $inserted/$scanned tersimpan")
        }.onFailure { RuntimeLogger.e(this, TAG, "Gagal sync active notifications", it) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        prefs.lastListenerDisconnectedAt = System.currentTimeMillis()
        WatchdogService.scheduleHealthCheck(this, 60_000L)
        RuntimeLogger.w(this, TAG, L.t("Notification listener terputus; coba rebind saat sistem mengizinkan", "Notification listener disconnected; will rebind when system allows"))
        NotificationListenerService.requestRebind(android.content.ComponentName(this, NotificationCaptureService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatching {
            val inserted = repo.saveFromStatusBarNotification(sbn)
            if (inserted) RuntimeLogger.i(this, TAG, L.t("Notif dicatat dari ${sbn.packageName}", "Notification recorded from ${sbn.packageName}"))
        }.onFailure { RuntimeLogger.e(this, TAG, "Gagal mencatat notification dari ${sbn.packageName}", it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (prefs.debugLogging) RuntimeLogger.i(this, TAG, L.t("Notif dihapus dari ${sbn.packageName}", "Notification removed from ${sbn.packageName}"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WatchdogService.scheduleHealthCheck(this)
        return START_STICKY
    }

    companion object {
        private const val TAG = "NotifCapture"
    }
}
