package com.navre.notiflogs.data

data class NotificationRecord(
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val bigText: String,
    val subText: String,
    val infoText: String,
    val category: String,
    val channelId: String,
    val sbnKey: String,
    val groupKey: String,
    val postTime: Long,
    val notificationWhen: Long,
    val capturedAt: Long,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val priority: Int,
    val hash: String,
    val raw: String
)

data class ThreadSummary(
    val packageName: String,
    val appName: String,
    val title: String,
    val count: Int,
    val lastTime: Long,
    val lastText: String
)

data class AppRule(
    val packageName: String,
    val appName: String,
    val mode: Int,
    val count: Int = 0
) {
    companion object {
        const val MODE_NORMAL = 0
        const val MODE_ALLOW = 1
        const val MODE_BLOCK = 2
    }
}

data class RuntimeLogEntry(
    val id: Long,
    val time: Long,
    val level: String,
    val tag: String,
    val message: String
)


data class DashboardStats(
    val totalNotifications: Int,
    val totalThreads: Int,
    val totalApps: Int
)


data class DiagnosticInfo(
    val listenerEnabled: Boolean,
    val batteryExempt: Boolean,
    val totalNotifications: Int,
    val totalThreads: Int,
    val totalApps: Int,
    val lastCaptureAt: Long,
    val lastSavedAt: Long,
    val lastSkipAt: Long,
    val lastSkipReason: String,
    val lastListenerConnectedAt: Long,
    val lastListenerDisconnectedAt: Long,
    val lastWatchdogTickAt: Long,
    val lastWatchdogStartAt: Long,
    val lastHeartbeatAt: Long,
    val lastSelfHealAt: Long,
    val lastServiceStartFailureAt: Long,
    val lastServiceStartFailureReason: String,
    val dedupMode: String,
    val dedupWindowMs: Long,
    val ignoreGroupSummary: Boolean,
    val parseMessagingStyle: Boolean,
    val captureOngoing: Boolean,
    val keepAliveEnabled: Boolean,
    val aggressiveSelfHeal: Boolean,
    val debugLogging: Boolean,
    val retentionDays: Int
)
