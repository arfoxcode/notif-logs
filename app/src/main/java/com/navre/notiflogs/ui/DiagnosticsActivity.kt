package com.navre.notiflogs.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.data.DiagnosticInfo
import com.navre.notiflogs.data.NotificationRepository
import com.navre.notiflogs.service.WatchdogService
import com.navre.notiflogs.util.AppColors
import com.navre.notiflogs.util.L
import com.navre.notiflogs.util.TimeText
import com.navre.notiflogs.util.bold
import com.navre.notiflogs.util.cardBackground
import com.navre.notiflogs.util.chip
import com.navre.notiflogs.util.dp
import com.navre.notiflogs.util.pillButton
import com.navre.notiflogs.util.titleRow

class DiagnosticsActivity : Activity() {
    private lateinit var repo: NotificationRepository
    private lateinit var prefs: AppPrefs
    private lateinit var root: LinearLayout
    private var lastDarkMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = NotificationRepository(this)
        prefs = AppPrefs(this)
        lastDarkMode = prefs.darkMode
        AppColors.apply(lastDarkMode)
        buildUi()
        render()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.darkMode != lastDarkMode) recreate()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(AppColors.bg) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun render() {
        root.removeAllViews()
        val info = repo.getDiagnosticInfo(
            listenerEnabled = WatchdogService.isNotificationAccessEnabled(this),
            batteryExempt = isIgnoringBatteryOptimizations()
        )

        root.addView(titleRow(
            "Diagnostics",
            "Diagnostics",
            L.t("Memeriksa listener, battery, watchdog, heartbeat, riwayat capture, dan konfigurasi aktif.", "Checks listener, battery, watchdog, heartbeat, capture history, and active configuration."),
            28f
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        root.addView(statusCard(info), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        root.addView(titleRow(
            L.t("Riwayat capture", "Capture history"),
            L.t("Riwayat capture", "Capture history"),
            L.t("Waktu terakhir listener menangkap, menyimpan, atau melewati notifikasi serta event watchdog lain.", "Last time the listener captured, saved, skipped notifications, and other watchdog events.")
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        root.addView(detailCard(listOf(
            L.t("Capture terakhir", "Last capture") to TimeText.full(info.lastCaptureAt),
            L.t("Data tersimpan terakhir", "Last saved") to TimeText.full(info.lastSavedAt),
            L.t("Skip terakhir", "Last skip") to TimeText.full(info.lastSkipAt),
            L.t("Alasan skip terakhir", "Last skip reason") to info.lastSkipReason,
            "Listener connected" to TimeText.full(info.lastListenerConnectedAt),
            "Listener disconnected" to TimeText.full(info.lastListenerDisconnectedAt),
            "Watchdog start" to TimeText.full(info.lastWatchdogStartAt),
            "Watchdog tick" to TimeText.full(info.lastWatchdogTickAt),
            L.t("Heartbeat terakhir", "Last heartbeat") to TimeText.full(info.lastHeartbeatAt),
            L.t("Self-heal terakhir", "Last self-heal") to TimeText.full(info.lastSelfHealAt),
            L.t("Gagal start service", "Service start failure") to TimeText.full(info.lastServiceStartFailureAt),
            L.t("Alasan gagal start", "Failure reason") to info.lastServiceStartFailureReason
        )), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        root.addView(titleRow(
            L.t("Konfigurasi aktif", "Active configuration"),
            L.t("Konfigurasi aktif", "Active configuration"),
            L.t("Pengaturan yang sedang berjalan dan memengaruhi cara aplikasi menangkap notifikasi.", "Current settings that affect how the app records notifications.")
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        root.addView(detailCard(listOf(
            "Dedup mode" to info.dedupMode,
            "Dedup window" to L.t("${info.dedupWindowMs / 1000L} detik", "${info.dedupWindowMs / 1000L} seconds"),
            "Ignore group summary" to L.yesNo(info.ignoreGroupSummary),
            "Parse MessagingStyle" to L.yesNo(info.parseMessagingStyle),
            "Capture ongoing" to L.yesNo(info.captureOngoing),
            "Keep-alive heartbeat" to L.yesNo(info.keepAliveEnabled),
            L.t("Self-heal agresif", "Aggressive self-heal") to L.yesNo(info.aggressiveSelfHeal),
            "Debug logging" to L.yesNo(info.debugLogging),
            "Dark mode" to L.yesNo(prefs.darkMode),
            "Retention" to L.t("${info.retentionDays} hari", "${info.retentionDays} days")
        )), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(pillButton(L.t("Refresh", "Refresh"), AppColors.brand) { render() }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
        row1.addView(pillButton("Settings", AppColors.textSecondary) { startActivity(Intent(this, SettingsActivity::class.java)) }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        root.addView(row1, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(pillButton("Runtime logs", AppColors.textSecondary) { startActivity(Intent(this, LogsActivity::class.java)) }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
        row2.addView(pillButton(L.t("Kembali", "Back"), AppColors.brand) { finish() }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        root.addView(row2)
    }

    private fun statusCard(info: DiagnosticInfo): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            cardBackground(this@DiagnosticsActivity)
        }
        card.addView(titleRow(L.t("Status utama", "Main status"), L.t("Status utama", "Main status"), L.t("Ringkasan cepat kondisi listener, battery, dan total data.", "Quick summary of listener, battery, and total data.")), LinearLayout.LayoutParams(-1, -2))
        val chipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(10)) }
        chipRow.addView(chip(if (info.listenerEnabled) L.t("Listener aktif", "Listener on") else L.t("Listener mati", "Listener off"), if (info.listenerEnabled) AppColors.chipGreen else AppColors.chipRed, if (info.listenerEnabled) AppColors.green else AppColors.red))
        chipRow.addView(chip(if (info.batteryExempt) L.t("Battery aman", "Battery safe") else L.t("Battery belum aman", "Battery not safe"), if (info.batteryExempt) AppColors.chipBlue else AppColors.chipOrange, if (info.batteryExempt) AppColors.brandDark else AppColors.orange), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        card.addView(chipRow)
        card.addView(TextView(this).apply {
            text = L.t("Total: ${info.totalNotifications} notif · ${info.totalThreads} thread · ${info.totalApps} aplikasi", "Total: ${info.totalNotifications} notif · ${info.totalThreads} threads · ${info.totalApps} apps")
            textSize = 14f
            setTextColor(AppColors.textSecondary)
        })
        return card
    }

    private fun detailCard(items: List<Pair<String, String>>): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            cardBackground(this@DiagnosticsActivity)
        }
        items.forEach { (label, value) ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, dp(6)) }
            row.addView(TextView(this).apply {
                text = label
                textSize = 13f
                setTextColor(AppColors.textSecondary)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply {
                text = value
                textSize = 13f
                setTextColor(AppColors.textPrimary)
                bold()
            }, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(row)
        }
        return card
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}
