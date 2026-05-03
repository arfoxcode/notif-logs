package com.navre.notiflogs.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.navre.notiflogs.R
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.data.NotificationRepository
import com.navre.notiflogs.data.RuntimeLogger
import com.navre.notiflogs.ui.MainActivity
import com.navre.notiflogs.util.L

class WatchdogService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            runHealthCheck(reason = "tick")
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val prefs = AppPrefs(this)
        prefs.lastWatchdogStartAt = System.currentTimeMillis()
        startForeground(NOTIF_ID, buildNotification())
        scheduleHealthCheck(this)
        RuntimeLogger.i(this, TAG, L.t("WatchdogService aktif dan heartbeat dijadwalkan", "WatchdogService active and heartbeat scheduled"))
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action.orEmpty()
        if (action == BootReceiver.ACTION_HEALTH_CHECK || action == ACTION_FORCE_HEALTH_CHECK) {
            runHealthCheck(reason = action.ifBlank { "manual" })
        }
        scheduleHealthCheck(this)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scheduleHealthCheck(this)
        RuntimeLogger.w(this, TAG, L.t("WatchdogService dihentikan; heartbeat ulang dijadwalkan", "WatchdogService stopped; heartbeat rescheduled"))
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        scheduleSoftRestart(this, 5_000L)
        scheduleHealthCheck(this, 60_000L)
        RuntimeLogger.w(this, TAG, L.t("Task dihapus; soft restart dan heartbeat dijadwalkan", "Task removed; soft restart and heartbeat scheduled"))
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun runHealthCheck(reason: String) {
        runCatching {
            val prefs = AppPrefs(this)
            val now = System.currentTimeMillis()
            prefs.lastWatchdogTickAt = now
            prefs.lastHeartbeatAt = now

            val enabled = isNotificationAccessEnabled(this)
            if (!enabled) {
                RuntimeLogger.w(this, TAG, L.t("Akses notification listener belum aktif", "Notification listener access is not enabled"))
            } else if (prefs.keepAliveEnabled && prefs.aggressiveSelfHeal) {
                val staleListener = prefs.lastListenerConnectedAt == 0L || now - prefs.lastListenerConnectedAt > LISTENER_STALE_MS
                if (staleListener) {
                    requestListenerRebind(this)
                    prefs.lastSelfHealAt = now
                    RuntimeLogger.w(this, TAG, L.t("Self-heal: requestRebind listener karena koneksi listener terlihat stale ($reason)", "Self-heal: requestRebind because listener connection looks stale ($reason)"))
                }
            }

            NotificationRepository(this).pruneOldIfNeeded()
            refreshForegroundNotification(enabled)
            scheduleHealthCheck(this)
        }.onFailure { RuntimeLogger.e(this, TAG, L.t("Watchdog health check gagal", "Watchdog health check failed"), it) }
    }

    private fun refreshForegroundNotification(listenerEnabled: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(listenerEnabled))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.watchdog_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.watchdog_channel_desc) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(listenerEnabled: Boolean = isNotificationAccessEnabled(this)): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(this, 10, intent, flags)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
        }
        val status = if (listenerEnabled) L.t("Listener aktif • heartbeat menjaga layanan", "Listener active • heartbeat keeps service alive") else L.t("Listener belum aktif • buka aplikasi untuk diagnosis", "Listener not enabled • open app for diagnosis")
        return builder
            .setSmallIcon(R.drawable.ic_stat_notif_logs)
            .setContentTitle(L.t("Notif Logs berjaga", "Notif Logs watching"))
            .setContentText(status)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(android.app.Notification.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "Watchdog"
        private const val CHANNEL_ID = "notif_logs_guard"
        private const val NOTIF_ID = 4207
        private const val TICK_MS = 30_000L
        private const val HEARTBEAT_MS = 10L * 60L * 1000L
        private const val LISTENER_STALE_MS = 6L * 60L * 1000L
        const val ACTION_FORCE_HEALTH_CHECK = "com.navre.notiflogs.FORCE_HEALTH_CHECK"

        fun start(context: Context) {
            val appContext = context.applicationContext
            scheduleHealthCheck(appContext)
            runCatching {
                val intent = Intent(appContext, WatchdogService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            }.onFailure {
                AppPrefs(appContext).markServiceStartFailure(it.javaClass.simpleName + ": " + (it.message ?: "tanpa pesan"))
                RuntimeLogger.e(appContext, TAG, "Gagal start WatchdogService", it)
            }
        }

        fun forceHealthCheck(context: Context) {
            val appContext = context.applicationContext
            scheduleHealthCheck(appContext, 30_000L)
            runCatching {
                val intent = Intent(appContext, WatchdogService::class.java).apply { action = ACTION_FORCE_HEALTH_CHECK }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appContext.startForegroundService(intent) else appContext.startService(intent)
            }.onFailure {
                AppPrefs(appContext).markServiceStartFailure(it.javaClass.simpleName + ": " + (it.message ?: "tanpa pesan"))
                RuntimeLogger.e(appContext, TAG, "Gagal force health check", it)
            }
        }

        fun scheduleHealthCheck(context: Context, delayMs: Long = HEARTBEAT_MS) {
            val appContext = context.applicationContext
            if (!AppPrefs(appContext).keepAliveEnabled) return
            runCatching {
                val intent = Intent(appContext, BootReceiver::class.java).apply { action = BootReceiver.ACTION_HEALTH_CHECK }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    98,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarm = appContext.getSystemService(AlarmManager::class.java)
                val triggerAt = System.currentTimeMillis() + delayMs
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            }.onFailure { RuntimeLogger.e(appContext, TAG, "Gagal jadwalkan heartbeat", it) }
        }

        fun scheduleSoftRestart(context: Context, delayMs: Long = 5_000L) {
            val appContext = context.applicationContext
            runCatching {
                val intent = Intent(appContext, BootReceiver::class.java).apply { action = BootReceiver.ACTION_RESTART_WATCHDOG }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    99,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarm = appContext.getSystemService(AlarmManager::class.java)
                alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, pendingIntent)
            }.onFailure { RuntimeLogger.e(appContext, TAG, "Gagal jadwalkan soft restart", it) }
        }

        fun requestListenerRebind(context: Context) {
            runCatching {
                NotificationListenerServiceAccessor.requestRebind(context)
            }.onFailure { RuntimeLogger.e(context, TAG, "Gagal requestRebind listener", it) }
        }

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            return flat.split(":").any { it.contains(context.packageName, ignoreCase = true) }
        }

        fun batteryOptimizationIntent(context: Context): Intent {
            return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }
}

private object NotificationListenerServiceAccessor {
    fun requestRebind(context: Context) {
        android.service.notification.NotificationListenerService.requestRebind(
            ComponentName(context, NotificationCaptureService::class.java)
        )
    }
}
