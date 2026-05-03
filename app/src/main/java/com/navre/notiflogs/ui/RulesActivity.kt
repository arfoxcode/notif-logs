package com.navre.notiflogs.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.data.AppRule
import com.navre.notiflogs.data.NotificationRepository
import com.navre.notiflogs.util.AppColors
import com.navre.notiflogs.util.L
import com.navre.notiflogs.util.bold
import com.navre.notiflogs.util.cardBackground
import com.navre.notiflogs.util.chip
import com.navre.notiflogs.util.dp
import com.navre.notiflogs.util.emptyState
import com.navre.notiflogs.util.pillButton
import com.navre.notiflogs.util.roundedBg
import com.navre.notiflogs.util.titleRow

class RulesActivity : Activity() {
    private lateinit var repo: NotificationRepository
    private lateinit var prefs: AppPrefs
    private lateinit var modeCardText: TextView
    private lateinit var list: LinearLayout
    private var lastDarkMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = NotificationRepository(this)
        prefs = AppPrefs(this)
        lastDarkMode = prefs.darkMode
        AppColors.apply(lastDarkMode)
        buildUi()
        renderRules()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.darkMode != lastDarkMode) recreate()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(AppColors.bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }
        scroll.addView(root)

        root.addView(titleRow(
            L.t("Rules aplikasi", "App rules"),
            L.t("Rules aplikasi", "App rules"),
            L.t("Atur aplikasi mana yang dicatat. Blacklist mencatat semua kecuali yang diblok. Whitelist hanya mencatat aplikasi Allow.", "Choose which apps are recorded. Blacklist records everything except blocked apps. Whitelist records only apps marked Allow."),
            28f
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        val modeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(AppColors.surface, dp(20).toFloat(), AppColors.border, dp(1))
            elevation = dp(1).toFloat()
        }
        modeCard.addView(titleRow(
            L.t("Mode filter", "Filter mode"),
            L.t("Mode filter", "Filter mode"),
            L.t("Toggle mode mengubah antara whitelist dan blacklist.", "Toggle mode switches between whitelist and blacklist.")
        ))
        modeCardText = TextView(this).apply {
            textSize = 14f
            setTextColor(AppColors.textSecondary)
            setPadding(0, dp(6), 0, dp(10))
        }
        modeCard.addView(modeCardText)
        val modeActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        modeActions.addView(pillButton(L.t("Toggle mode", "Toggle mode"), AppColors.brand) {
            prefs.whitelistMode = !prefs.whitelistMode
            renderRules()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
        modeActions.addView(pillButton(L.t("Kembali", "Back"), Color.parseColor("#475569")) { finish() }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        modeCard.addView(modeActions)
        root.addView(modeCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })

        root.addView(titleRow(
            L.t("Daftar aplikasi", "App list"),
            L.t("Daftar aplikasi", "App list"),
            L.t("Pilih status Normal, Allow, atau Block untuk tiap aplikasi.", "Choose Normal, Allow, or Block for each app.")
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(scroll)
    }

    private fun renderRules() {
        modeCardText.text = if (prefs.whitelistMode) L.t("Whitelist aktif", "Whitelist active") else L.t("Blacklist aktif", "Blacklist active")
        if (!::list.isInitialized) return
        list.removeAllViews()
        val apps = repo.getObservedApps()
        if (apps.isEmpty()) {
            list.addView(emptyState(L.t("Belum ada aplikasi teramati.", "No observed apps yet.")))
            return
        }
        apps.forEach { app -> list.addView(ruleCard(app), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }) }
    }

    private fun ruleCard(app: AppRule): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            cardBackground(this@RulesActivity)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val textWrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textWrap.addView(TextView(this).apply {
            text = app.appName.ifBlank { app.packageName }
            textSize = 17f
            setTextColor(AppColors.textPrimary)
            bold()
        })
        textWrap.addView(TextView(this).apply {
            text = app.packageName
            textSize = 12f
            setTextColor(AppColors.textSecondary)
            setPadding(0, dp(2), 0, 0)
        })
        top.addView(textWrap, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(chip(modeText(app.mode), modeColorBg(app.mode), modeColorText(app.mode)))
        card.addView(top)
        card.addView(TextView(this).apply {
            text = L.t("Total notif: ${app.count}", "Total notif: ${app.count}")
            textSize = 13f
            setTextColor(AppColors.textSecondary)
            setPadding(0, dp(8), 0, dp(10))
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(pillButton("Normal", Color.parseColor("#64748B")) {
            repo.setRule(app.packageName, app.appName, AppRule.MODE_NORMAL); renderRules()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(4) })
        row.addView(pillButton("Allow", AppColors.green) {
            repo.setRule(app.packageName, app.appName, AppRule.MODE_ALLOW); renderRules()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })
        row.addView(pillButton("Block", AppColors.red) {
            repo.setRule(app.packageName, app.appName, AppRule.MODE_BLOCK); renderRules()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(4) })
        card.addView(row)
        return card
    }

    private fun modeText(mode: Int): String = when (mode) {
        AppRule.MODE_ALLOW -> "ALLOW"
        AppRule.MODE_BLOCK -> "BLOCK"
        else -> "NORMAL"
    }

    private fun modeColorBg(mode: Int): Int = when (mode) {
        AppRule.MODE_ALLOW -> AppColors.chipGreen
        AppRule.MODE_BLOCK -> AppColors.chipRed
        else -> AppColors.chipGray
    }

    private fun modeColorText(mode: Int): Int = when (mode) {
        AppRule.MODE_ALLOW -> AppColors.green
        AppRule.MODE_BLOCK -> AppColors.red
        else -> AppColors.textSecondary
    }
}
