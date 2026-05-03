package com.navre.notiflogs.data

import android.content.Context
import com.navre.notiflogs.util.L

class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("notif_logs_prefs", Context.MODE_PRIVATE)

    var whitelistMode: Boolean
        get() = prefs.getBoolean(KEY_WHITELIST_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_WHITELIST_MODE, value).apply()

    var captureOngoing: Boolean
        get() = prefs.getBoolean(KEY_CAPTURE_ONGOING, false)
        set(value) = prefs.edit().putBoolean(KEY_CAPTURE_ONGOING, value).apply()

    var ignoreGroupSummary: Boolean
        get() = prefs.getBoolean(KEY_IGNORE_GROUP_SUMMARY, true)
        set(value) = prefs.edit().putBoolean(KEY_IGNORE_GROUP_SUMMARY, value).apply()

    var parseMessagingStyle: Boolean
        get() = prefs.getBoolean(KEY_PARSE_MESSAGING_STYLE, true)
        set(value) = prefs.edit().putBoolean(KEY_PARSE_MESSAGING_STYLE, value).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var hidePreview: Boolean
        get() = prefs.getBoolean(KEY_HIDE_PREVIEW, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_PREVIEW, value).apply()

    var debugLogging: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_LOGGING, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_LOGGING, value).apply()

    var keepAliveEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEEP_ALIVE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_ALIVE_ENABLED, value).apply()

    var aggressiveSelfHeal: Boolean
        get() = prefs.getBoolean(KEY_AGGRESSIVE_SELF_HEAL, true)
        set(value) = prefs.edit().putBoolean(KEY_AGGRESSIVE_SELF_HEAL, value).apply()

    var dedupMode: Int
        get() = prefs.getInt(KEY_DEDUP_MODE, DEDUP_NORMAL)
        set(value) = prefs.edit().putInt(KEY_DEDUP_MODE, value.coerceIn(DEDUP_SAFE, DEDUP_AGGRESSIVE)).apply()

    var dedupWindowMs: Long
        get() = prefs.getLong(KEY_DEDUP_WINDOW_MS, 30_000L)
        set(value) = prefs.edit().putLong(KEY_DEDUP_WINDOW_MS, value.coerceIn(5_000L, 5L * 60L * 1000L)).apply()

    var retentionDays: Int
        get() = prefs.getInt(KEY_RETENTION_DAYS, 30)
        set(value) = prefs.edit().putInt(KEY_RETENTION_DAYS, value.coerceIn(1, 3650)).apply()

    var lastPruneAt: Long
        get() = prefs.getLong(KEY_LAST_PRUNE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PRUNE, value).apply()

    var lastCaptureAt: Long
        get() = prefs.getLong(KEY_LAST_CAPTURE_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CAPTURE_AT, value).apply()

    var lastSavedAt: Long
        get() = prefs.getLong(KEY_LAST_SAVED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SAVED_AT, value).apply()

    var lastSkipAt: Long
        get() = prefs.getLong(KEY_LAST_SKIP_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SKIP_AT, value).apply()

    var lastSkipReason: String
        get() = prefs.getString(KEY_LAST_SKIP_REASON, null) ?: L.t("Belum ada skip tercatat", "No skipped notification recorded yet")
        set(value) = prefs.edit().putString(KEY_LAST_SKIP_REASON, value.take(500)).apply()

    var lastListenerConnectedAt: Long
        get() = prefs.getLong(KEY_LAST_LISTENER_CONNECTED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_LISTENER_CONNECTED_AT, value).apply()

    var lastListenerDisconnectedAt: Long
        get() = prefs.getLong(KEY_LAST_LISTENER_DISCONNECTED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_LISTENER_DISCONNECTED_AT, value).apply()

    var lastWatchdogTickAt: Long
        get() = prefs.getLong(KEY_LAST_WATCHDOG_TICK_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_WATCHDOG_TICK_AT, value).apply()

    var lastWatchdogStartAt: Long
        get() = prefs.getLong(KEY_LAST_WATCHDOG_START_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_WATCHDOG_START_AT, value).apply()

    var lastHeartbeatAt: Long
        get() = prefs.getLong(KEY_LAST_HEARTBEAT_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_HEARTBEAT_AT, value).apply()

    var lastSelfHealAt: Long
        get() = prefs.getLong(KEY_LAST_SELF_HEAL_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SELF_HEAL_AT, value).apply()

    var lastServiceStartFailureAt: Long
        get() = prefs.getLong(KEY_LAST_SERVICE_START_FAILURE_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SERVICE_START_FAILURE_AT, value).apply()

    var lastServiceStartFailureReason: String
        get() = prefs.getString(KEY_LAST_SERVICE_START_FAILURE_REASON, null) ?: L.t("Belum ada gagal start", "No service start failure yet")
        set(value) = prefs.edit().putString(KEY_LAST_SERVICE_START_FAILURE_REASON, value.take(500)).apply()

    fun markCaptured() {
        lastCaptureAt = System.currentTimeMillis()
    }

    fun markSaved() {
        val now = System.currentTimeMillis()
        lastCaptureAt = now
        lastSavedAt = now
    }

    fun markSkipped(reason: String) {
        val now = System.currentTimeMillis()
        lastCaptureAt = now
        lastSkipAt = now
        lastSkipReason = reason
    }

    fun markServiceStartFailure(reason: String) {
        lastServiceStartFailureAt = System.currentTimeMillis()
        lastServiceStartFailureReason = reason
    }

    fun dedupModeLabel(): String = when (dedupMode) {
        DEDUP_SAFE -> "SAFE"
        DEDUP_AGGRESSIVE -> "AGGRESSIVE"
        else -> "NORMAL"
    }

    companion object {
        const val DEDUP_SAFE = 0
        const val DEDUP_NORMAL = 1
        const val DEDUP_AGGRESSIVE = 2

        private const val KEY_WHITELIST_MODE = "whitelist_mode"
        private const val KEY_CAPTURE_ONGOING = "capture_ongoing"
        private const val KEY_IGNORE_GROUP_SUMMARY = "ignore_group_summary"
        private const val KEY_PARSE_MESSAGING_STYLE = "parse_messaging_style"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_HIDE_PREVIEW = "hide_preview"
        private const val KEY_DEBUG_LOGGING = "debug_logging"
        private const val KEY_KEEP_ALIVE_ENABLED = "keep_alive_enabled"
        private const val KEY_AGGRESSIVE_SELF_HEAL = "aggressive_self_heal"
        private const val KEY_DEDUP_MODE = "dedup_mode"
        private const val KEY_DEDUP_WINDOW_MS = "dedup_window_ms"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_LAST_PRUNE = "last_prune_at"
        private const val KEY_LAST_CAPTURE_AT = "last_capture_at"
        private const val KEY_LAST_SAVED_AT = "last_saved_at"
        private const val KEY_LAST_SKIP_AT = "last_skip_at"
        private const val KEY_LAST_SKIP_REASON = "last_skip_reason"
        private const val KEY_LAST_LISTENER_CONNECTED_AT = "last_listener_connected_at"
        private const val KEY_LAST_LISTENER_DISCONNECTED_AT = "last_listener_disconnected_at"
        private const val KEY_LAST_WATCHDOG_TICK_AT = "last_watchdog_tick_at"
        private const val KEY_LAST_WATCHDOG_START_AT = "last_watchdog_start_at"
        private const val KEY_LAST_HEARTBEAT_AT = "last_heartbeat_at"
        private const val KEY_LAST_SELF_HEAL_AT = "last_self_heal_at"
        private const val KEY_LAST_SERVICE_START_FAILURE_AT = "last_service_start_failure_at"
        private const val KEY_LAST_SERVICE_START_FAILURE_REASON = "last_service_start_failure_reason"
    }
}
