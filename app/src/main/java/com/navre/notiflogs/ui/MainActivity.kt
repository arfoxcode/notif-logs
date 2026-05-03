package com.navre.notiflogs.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.data.NotificationRepository
import com.navre.notiflogs.service.WatchdogService
import com.navre.notiflogs.util.AppColors
import com.navre.notiflogs.util.L
import com.navre.notiflogs.util.TimeText
import com.navre.notiflogs.util.bold
import com.navre.notiflogs.util.cardBackground
import com.navre.notiflogs.util.chip
import com.navre.notiflogs.util.dp
import com.navre.notiflogs.util.emptyState
import com.navre.notiflogs.util.primaryButton
import com.navre.notiflogs.util.roundedBg
import com.navre.notiflogs.util.secondaryButton
import com.navre.notiflogs.util.statCard
import com.navre.notiflogs.util.titleRow

class MainActivity : Activity() {
    private lateinit var repo: NotificationRepository
    private lateinit var prefs: AppPrefs
    private lateinit var statusText: TextView
    private lateinit var permissionChipWrap: LinearLayout
    private lateinit var statsWrap: LinearLayout
    private lateinit var listContainer: LinearLayout
    private lateinit var searchBox: EditText
    private var lastDarkMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = NotificationRepository(this)
        prefs = AppPrefs(this)
        lastDarkMode = prefs.darkMode
        AppColors.apply(lastDarkMode)
        WatchdogService.start(this)
        requestPostNotificationPermission()
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.darkMode != lastDarkMode) {
            recreate()
            return
        }
        WatchdogService.scheduleHealthCheck(this)
        refreshStatus()
        renderThreads()
    }

    private fun buildUi() {
        val frame = FrameLayout(this).apply { setBackgroundColor(AppColors.bg) }
        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(AppColors.bg)
            clipToPadding = false
            setPadding(0, 0, 0, dp(104))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }
        rootScroll.addView(root)
        frame.addView(rootScroll, FrameLayout.LayoutParams(-1, -1))

        root.addView(
            titleRow(
                title = "Notif Logs",
                infoTitle = L.t("Tentang halaman utama", "About Home"),
                infoMessage = L.t(
                    "Halaman ini untuk melihat status listener, jumlah data, aksi cepat, dan daftar thread notifikasi.",
                    "This page shows listener status, data totals, quick actions, and notification threads."
                ),
                textSize = 30f
            ),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        )

        val heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(AppColors.surface, dp(22).toFloat(), AppColors.goldSoft, dp(1))
            elevation = dp(2).toFloat()
        }
        heroCard.addView(
            titleRow(
                title = L.t("Status layanan", "Service status"),
                infoTitle = L.t("Status layanan", "Service status"),
                infoMessage = L.t(
                    "Menunjukkan apakah akses listener aktif, baterai aman, dan heartbeat watchdog masih segar.",
                    "Shows whether listener access is enabled, battery restriction is safe, and the watchdog heartbeat is fresh."
                )
            )
        )
        statusText = TextView(this).apply {
            textSize = 14f
            setTextColor(AppColors.textSecondary)
            setPadding(0, dp(6), 0, dp(10))
        }
        heroCard.addView(statusText)
        permissionChipWrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        heroCard.addView(permissionChipWrap)
        root.addView(heroCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        statsWrap = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(statsWrap, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        root.addView(
            titleRow(
                title = L.t("Aksi cepat", "Quick actions"),
                infoTitle = L.t("Aksi cepat", "Quick actions"),
                infoMessage = L.t(
                    "Izin listener membuka pengaturan akses notifikasi. Battery meminta pengecualian optimasi baterai. Self-heal memaksa pemeriksaan layanan. Bersihkan duplikat menghapus data kembar sesuai mode dedup.",
                    "Listener opens notification access settings. Battery asks for battery optimization exemption. Self-heal forces a health check. Cleanup duplicates removes duplicate data using the active dedup mode."
                )
            )
        )
        val actionRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actionRow1.addView(primaryButton(L.t("Izin listener", "Listener permission")) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
        actionRow1.addView(secondaryButton(L.t("Battery", "Battery")) {
            requestBatteryExemption()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        root.addView(actionRow1, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        val actionRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actionRow2.addView(secondaryButton(L.t("Self-heal sekarang", "Self-heal now")) {
            WatchdogService.forceHealthCheck(this)
            refreshStatus()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
        actionRow2.addView(secondaryButton(L.t("Bersihkan duplikat", "Clean duplicates")) {
            confirmCleanupDuplicates()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        root.addView(actionRow2, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(16) })

        root.addView(
            titleRow(
                title = L.t("Cari notifikasi", "Search notifications"),
                infoTitle = L.t("Cari notifikasi", "Search notifications"),
                infoMessage = L.t(
                    "Pencarian memfilter thread berdasarkan judul, nama aplikasi, atau isi notifikasi.",
                    "Search filters threads by title, app name, or notification content."
                )
            )
        )
        searchBox = EditText(this).apply {
            hint = L.t("Cari judul, aplikasi, atau isi notifikasi", "Search title, app, or notification text")
            textSize = 15f
            setTextColor(AppColors.textPrimary)
            setHintTextColor(AppColors.textMuted)
            setSingleLine(true)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBg(AppColors.surface, dp(16).toFloat(), AppColors.border, dp(1))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderThreads()
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        root.addView(searchBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(16) })

        root.addView(
            titleRow(
                title = L.t("Percakapan terbaru", "Recent threads"),
                infoTitle = L.t("Daftar thread", "Thread list"),
                infoMessage = L.t(
                    "Setiap kartu mewakili grup notifikasi berdasarkan aplikasi dan judul. Ketuk kartu untuk membuka detail.",
                    "Each card represents a notification group by app and title. Tap a card to open details."
                )
            ),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        )
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listContainer)

        frame.addView(floatingMenu(), FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            leftMargin = dp(12)
            rightMargin = dp(12)
            bottomMargin = dp(12)
        })

        setContentView(frame)
    }

    private fun floatingMenu(): LinearLayout {
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedBg(AppColors.navBg, dp(24).toFloat(), AppColors.goldSoft, dp(1))
            elevation = dp(8).toFloat()
        }
        menu.addView(navItem(L.t("Home", "Home")) { refreshStatus(); renderThreads() }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { rightMargin = dp(3) })
        menu.addView(navItem(L.t("Rules", "Rules")) { startActivity(Intent(this, RulesActivity::class.java)) }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        menu.addView(navItem(L.t("Set", "Set")) { startActivity(Intent(this, SettingsActivity::class.java)) }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        menu.addView(navItem(L.t("Info", "About")) { startActivity(Intent(this, AboutActivity::class.java)) }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        menu.addView(navItem(L.t("Diag", "Diag")) { startActivity(Intent(this, DiagnosticsActivity::class.java)) }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        menu.addView(navItem(L.t("Logs", "Logs")) { startActivity(Intent(this, LogsActivity::class.java)) }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(3) })
        return menu
    }

    private fun navItem(label: String, click: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 11f
            bold()
            setTextColor(AppColors.goldText)
            background = roundedBg(AppColors.navItemBg, dp(18).toFloat())
            setOnClickListener { click() }
        }
    }

    private fun refreshStatus() {
        val nls = WatchdogService.isNotificationAccessEnabled(this)
        val battery = isIgnoringBatteryOptimizations()
        val now = System.currentTimeMillis()
        val heartbeatFresh = prefs.lastHeartbeatAt > 0L && now - prefs.lastHeartbeatAt < 15L * 60L * 1000L
        statusText.text = when {
            nls && battery && heartbeatFresh -> L.t("Siap · Listener aktif · Battery aman · Heartbeat OK", "Ready · Listener on · Battery safe · Heartbeat OK")
            nls && battery -> L.t("Listener aktif · Battery aman · Heartbeat perlu dicek", "Listener on · Battery safe · Heartbeat needs checking")
            nls -> L.t("Listener aktif · Battery masih rawan", "Listener on · Battery still risky")
            else -> L.t("Listener belum aktif", "Listener is not enabled")
        }
        permissionChipWrap.removeAllViews()
        permissionChipWrap.addView(chip(if (nls) L.t("Listener aktif", "Listener on") else L.t("Listener mati", "Listener off"), if (nls) AppColors.chipGreen else AppColors.chipRed, if (nls) AppColors.green else AppColors.red))
        permissionChipWrap.addView(chip(if (battery) L.t("Battery aman", "Battery safe") else L.t("Battery rawan", "Battery risky"), if (battery) AppColors.chipBlue else AppColors.chipOrange, if (battery) AppColors.brandDark else AppColors.orange), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        permissionChipWrap.addView(chip(if (heartbeatFresh) "Heartbeat OK" else "Heartbeat stale", if (heartbeatFresh) AppColors.chipGold else AppColors.chipGray, if (heartbeatFresh) AppColors.goldText else AppColors.textSecondary), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })

        val stats = repo.getDashboardStats()
        statsWrap.removeAllViews()
        statsWrap.addView(statCard(stats.totalNotifications.toString(), L.t("Total notif", "Total notif")), LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(5) })
        statsWrap.addView(statCard(stats.totalThreads.toString(), L.t("Thread", "Threads")), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(5); rightMargin = dp(5) })
        statsWrap.addView(statCard(stats.totalApps.toString(), L.t("Aplikasi", "Apps")), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(5) })
    }

    private fun renderThreads() {
        if (!::listContainer.isInitialized) return
        listContainer.removeAllViews()
        val threads = repo.getThreads(searchBox.text?.toString().orEmpty())
        if (threads.isEmpty()) {
            listContainer.addView(emptyState(L.t("Belum ada notifikasi tercatat.", "No notifications recorded yet.")))
            return
        }
        threads.forEach { thread ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                cardBackground(this@MainActivity)
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, ChatActivity::class.java).apply {
                        putExtra(ChatActivity.EXTRA_PACKAGE, thread.packageName)
                        putExtra(ChatActivity.EXTRA_TITLE, thread.title)
                        putExtra(ChatActivity.EXTRA_APP, thread.appName)
                    })
                }
            }
            val avatar = TextView(this).apply {
                text = thread.appName.take(1).ifBlank { "?" }.uppercase()
                gravity = Gravity.CENTER
                textSize = 18f
                bold()
                setTextColor(AppColors.goldText)
                background = roundedBg(AppColors.chipGold, dp(16).toFloat())
            }
            card.addView(avatar, LinearLayout.LayoutParams(dp(48), dp(48)).apply { rightMargin = dp(12) })

            val center = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            center.addView(TextView(this).apply {
                text = thread.title.ifBlank { L.untitled() }
                textSize = 16f
                setTextColor(AppColors.textPrimary)
                bold()
            })
            center.addView(TextView(this).apply {
                text = thread.appName.ifBlank { thread.packageName }
                textSize = 14f
                setTextColor(AppColors.textSecondary)
                setPadding(0, dp(2), 0, dp(4))
            })
            center.addView(TextView(this).apply {
                text = if (prefs.hidePreview) L.hiddenPreview() else thread.lastText.ifBlank { L.noText() }
                textSize = 13f
                maxLines = 2
                setTextColor(AppColors.textMuted)
            })
            card.addView(center, LinearLayout.LayoutParams(0, -2, 1f))

            val right = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }
            right.addView(chip(thread.count.toString(), AppColors.chipGold, AppColors.goldText))
            right.addView(TextView(this).apply {
                text = TimeText.short(thread.lastTime)
                textSize = 11f
                setTextColor(AppColors.textMuted)
                gravity = Gravity.END
                setPadding(0, dp(8), 0, 0)
            })
            card.addView(right)
            listContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
    }

    private fun confirmCleanupDuplicates() {
        AlertDialog.Builder(this)
            .setTitle(L.t("Bersihkan duplikat?", "Clean duplicates?"))
            .setMessage(L.t("Aplikasi akan membersihkan duplikat sesuai mode dedup aktif: ${prefs.dedupModeLabel()}.", "The app will clean duplicates using the active dedup mode: ${prefs.dedupModeLabel()}."))
            .setPositiveButton(L.t("Bersihkan", "Clean")) { _, _ ->
                val deleted = repo.cleanupDuplicateNotifications()
                refreshStatus()
                renderThreads()
                AlertDialog.Builder(this)
                    .setTitle(L.t("Selesai", "Done"))
                    .setMessage(L.t("$deleted data duplikat dihapus.", "$deleted duplicate records deleted."))
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton(L.t("Batal", "Cancel"), null)
            .show()
    }

    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 27)
        }
    }

    private fun requestBatteryExemption() {
        if (isIgnoringBatteryOptimizations()) return
        runCatching { startActivity(WatchdogService.batteryOptimizationIntent(this)) }
            .onFailure {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}
