package com.navre.notiflogs.ui

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.util.AppColors
import com.navre.notiflogs.util.L
import com.navre.notiflogs.util.bold
import com.navre.notiflogs.util.cardBackground
import com.navre.notiflogs.util.chip
import com.navre.notiflogs.util.dp
import com.navre.notiflogs.util.infoButton
import com.navre.notiflogs.util.pillButton
import com.navre.notiflogs.util.titleRow

class SettingsActivity : Activity() {
    private lateinit var prefs: AppPrefs
    private lateinit var root: LinearLayout
    private var lastDarkMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        root.addView(
            titleRow(
                title = L.t("Settings", "Settings"),
                infoTitle = L.t("Settings", "Settings"),
                infoMessage = L.t(
                    "Atur perilaku capture, dedup, ketahanan layanan, tampilan, privasi, dan retensi data.",
                    "Configure capture behavior, dedup, service resilience, appearance, privacy, and data retention."
                ),
                textSize = 28f
            ),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        )

        section(L.t("Akurasi capture", "Capture accuracy"), L.t("Pengaturan untuk meningkatkan kualitas data notifikasi yang dicatat.", "Settings for improving recorded notification quality."))
        root.addView(settingCard("Abaikan group summary", "Ignore group summary", "Mengurangi notifikasi dobel dari aplikasi chat yang mengirim summary dan detail sekaligus.", "Reduces duplicate notifications from chat apps that send both summary and detail notifications.", prefs.ignoreGroupSummary) {
            prefs.ignoreGroupSummary = !prefs.ignoreGroupSummary; render()
        })
        root.addView(settingCard("Parse MessagingStyle", "Parse MessagingStyle", "Mengambil isi pesan dari format notifikasi chat modern seperti WhatsApp dan Telegram.", "Reads message content from modern chat notification formats such as WhatsApp and Telegram.", prefs.parseMessagingStyle) {
            prefs.parseMessagingStyle = !prefs.parseMessagingStyle; render()
        })
        root.addView(settingCard("Catat ongoing notification", "Capture ongoing notifications", "Untuk musik, download, VPN, upload, dan progress. Biasanya lebih ramai daripada notif biasa.", "For music, downloads, VPN, uploads, and progress notifications. Usually noisier than regular notifications.", prefs.captureOngoing) {
            prefs.captureOngoing = !prefs.captureOngoing; render()
        })

        section(L.t("Dedup", "Dedup"), L.t("Pengaturan untuk mencegah notifikasi kembar dicatat berulang kali.", "Settings for preventing duplicate notification records."))
        root.addView(modeCard())
        root.addView(windowCard())

        section(L.t("Ketahanan layanan", "Service resilience"), L.t("Pengaturan untuk menjaga watchdog dan listener tetap pulih saat perangkat agresif membunuh proses.", "Settings to help watchdog and listener recover when the device aggressively kills processes."))
        root.addView(settingCard("Keep-alive heartbeat", "Keep-alive heartbeat", "Menjadwalkan pemeriksaan berkala lewat AlarmManager agar watchdog punya kesempatan bangun lagi.", "Schedules periodic checks through AlarmManager so the watchdog gets a chance to wake again.", prefs.keepAliveEnabled) {
            prefs.keepAliveEnabled = !prefs.keepAliveEnabled; render()
        })
        root.addView(settingCard("Self-heal agresif", "Aggressive self-heal", "Saat listener terlihat stale, aplikasi akan mencoba requestRebind otomatis.", "When the listener looks stale, the app will try requestRebind automatically.", prefs.aggressiveSelfHeal) {
            prefs.aggressiveSelfHeal = !prefs.aggressiveSelfHeal; render()
        })

        section(L.t("Tampilan", "Appearance"), L.t("Atur tema warna aplikasi.", "Configure the app color theme."))
        root.addView(settingCard("Dark mode", "Dark mode", "Mengubah tampilan aplikasi menjadi tema gelap emas-hitam.", "Switches the app to a black-and-gold dark theme.", prefs.darkMode) {
            prefs.darkMode = !prefs.darkMode
            lastDarkMode = prefs.darkMode
            AppColors.apply(lastDarkMode)
            recreate()
        })

        section(L.t("Privasi & log", "Privacy & logs"), L.t("Atur visibilitas isi notifikasi dan seberapa banyak log internal disimpan.", "Configure notification preview visibility and internal logging."))
        root.addView(settingCard("Sembunyikan preview", "Hide preview", "Menyamarkan isi notifikasi di daftar utama. Detail thread tetap bisa dibuka.", "Hides notification content in the main list. Thread details can still be opened.", prefs.hidePreview) {
            prefs.hidePreview = !prefs.hidePreview; render()
        })
        root.addView(settingCard("Debug logging", "Debug logging", "Menyimpan log INFO lebih banyak. Berguna untuk troubleshooting, tetapi database log lebih cepat penuh.", "Stores more INFO logs. Useful for troubleshooting but fills log storage faster.", prefs.debugLogging) {
            prefs.debugLogging = !prefs.debugLogging; render()
        })

        section(L.t("Retensi data", "Data retention"), L.t("Menentukan berapa lama data notifikasi disimpan sebelum dipangkas otomatis.", "Defines how long notification data is kept before automatic pruning."))
        root.addView(retentionCard())

        section(L.t("Tentang", "About"), L.t("Informasi author, link sosial, dan tujuan aplikasi.", "Author info, social links, and app purpose."))
        root.addView(pillButton(L.t("Buka halaman Tentang", "Open About page"), AppColors.brandDark) {
            startActivity(Intent(this, AboutActivity::class.java))
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        root.addView(pillButton(L.t("Kembali", "Back"), AppColors.brand) { finish() }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
    }

    private fun section(title: String, info: String) {
        root.addView(titleRow(title, title, info), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(8) })
    }

    private fun settingCard(titleId: String, titleEn: String, infoId: String, infoEn: String, checked: Boolean, click: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            cardBackground(this@SettingsActivity)
            setOnClickListener { click() }
            addView(headerRow(L.t(titleId, titleEn), L.t(infoId, infoEn), L.active(checked)))
        }.also { it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) } }
    }

    private fun headerRow(title: String, info: String, value: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val left = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = title
                    textSize = 16f
                    setTextColor(AppColors.textPrimary)
                    bold()
                }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(infoButton(title, info), LinearLayout.LayoutParams(dp(24), dp(24)).apply { leftMargin = dp(8) })
            }
            addView(left, LinearLayout.LayoutParams(0, -2, 1f))
            val active = value == L.active(true) || value == prefs.dedupModeLabel()
            addView(chip(value, if (active) AppColors.chipBlue else AppColors.chipGray, if (active) AppColors.brandDark else AppColors.textSecondary))
        }
    }

    private fun modeCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            cardBackground(this@SettingsActivity)
        }
        card.addView(headerRow(L.t("Mode dedup", "Dedup mode"), L.t("Safe paling aman, Normal seimbang, Aggressive lebih berani membersihkan notifikasi yang mirip.", "Safe is safest, Normal is balanced, Aggressive is more willing to clean similar notifications."), prefs.dedupModeLabel()))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(pillButton("Safe", if (prefs.dedupMode == AppPrefs.DEDUP_SAFE) AppColors.brand else AppColors.textSecondary) {
            prefs.dedupMode = AppPrefs.DEDUP_SAFE; render()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(4); topMargin = dp(10) })
        row.addView(pillButton("Normal", if (prefs.dedupMode == AppPrefs.DEDUP_NORMAL) AppColors.brand else AppColors.textSecondary) {
            prefs.dedupMode = AppPrefs.DEDUP_NORMAL; render()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(4); rightMargin = dp(4); topMargin = dp(10) })
        row.addView(pillButton("Aggressive", if (prefs.dedupMode == AppPrefs.DEDUP_AGGRESSIVE) AppColors.brand else AppColors.textSecondary) {
            prefs.dedupMode = AppPrefs.DEDUP_AGGRESSIVE; render()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(4); topMargin = dp(10) })
        card.addView(row)
        return card.also { it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) } }
    }

    private fun windowCard(): LinearLayout {
        val sec = prefs.dedupWindowMs / 1000L
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            cardBackground(this@SettingsActivity)
        }
        card.addView(headerRow(L.t("Window dedup", "Dedup window"), L.t("Rentang waktu untuk mendeteksi notifikasi kembar yang datang berdekatan.", "Time window for detecting duplicate notifications that arrive close together."), "${sec}s"))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(5L, 30L, 60L, 120L).forEachIndexed { index, value ->
            row.addView(pillButton("${value}s", if (sec == value) AppColors.brand else AppColors.textSecondary) {
                prefs.dedupWindowMs = value * 1000L; render()
            }, LinearLayout.LayoutParams(0, -2, 1f).apply {
                topMargin = dp(10)
                if (index > 0) leftMargin = dp(4)
                if (index < 3) rightMargin = dp(4)
            })
        }
        card.addView(row)
        return card.also { it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) } }
    }

    private fun retentionCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            cardBackground(this@SettingsActivity)
        }
        card.addView(headerRow(L.t("Simpan data", "Keep data"), L.t("Data lama dipangkas otomatis oleh watchdog sesuai jumlah hari yang dipilih.", "Old data is automatically pruned by the watchdog based on the selected number of days."), L.t("${prefs.retentionDays} hari", "${prefs.retentionDays} days")))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(7, 30, 90, 365).forEachIndexed { index, value ->
            row.addView(pillButton(L.t("${value}h", "${value}d"), if (prefs.retentionDays == value) AppColors.brand else AppColors.textSecondary) {
                prefs.retentionDays = value; render()
            }, LinearLayout.LayoutParams(0, -2, 1f).apply {
                topMargin = dp(10)
                if (index > 0) leftMargin = dp(4)
                if (index < 3) rightMargin = dp(4)
            })
        }
        card.addView(row)
        return card.also { it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) } }
    }
}
