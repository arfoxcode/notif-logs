package com.navre.notiflogs.ui

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.data.NotificationRepository
import com.navre.notiflogs.data.RuntimeLogEntry
import com.navre.notiflogs.util.AppColors
import com.navre.notiflogs.util.L
import com.navre.notiflogs.util.TimeText
import com.navre.notiflogs.util.cardBackground
import com.navre.notiflogs.util.chip
import com.navre.notiflogs.util.dp
import com.navre.notiflogs.util.emptyState
import com.navre.notiflogs.util.pillButton
import com.navre.notiflogs.util.roundedBg
import com.navre.notiflogs.util.titleRow

class LogsActivity : Activity() {
    private lateinit var repo: NotificationRepository
    private lateinit var prefs: AppPrefs
    private lateinit var list: LinearLayout
    private lateinit var summaryText: TextView
    private lateinit var filterRow: LinearLayout
    private lateinit var searchBox: EditText
    private var lastDarkMode: Boolean = false
    private var selectedLevel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = NotificationRepository(this)
        prefs = AppPrefs(this)
        lastDarkMode = prefs.darkMode
        AppColors.apply(lastDarkMode)
        buildUi()
        renderFilters()
        renderLogs()
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
            L.t("Runtime logs", "Runtime logs"),
            L.t("Runtime logs", "Runtime logs"),
            L.t(
                "Daftar log internal untuk memantau listener, watchdog, self-heal, dan error capture.",
                "Internal logs for monitoring listener, watchdog, self-heal, and capture errors."
            ),
            28f
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        val summaryCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(AppColors.surface, dp(20).toFloat(), AppColors.border, dp(1))
            elevation = dp(1).toFloat()
        }
        summaryCard.addView(titleRow(
            L.t("Ringkasan", "Summary"),
            L.t("Ringkasan log", "Log summary"),
            L.t("Menampilkan jumlah log yang cocok dengan filter aktif.", "Shows how many logs match the active filter.")
        ))
        summaryText = TextView(this).apply {
            textSize = 14f
            setTextColor(AppColors.textSecondary)
            setPadding(0, dp(6), 0, dp(10))
        }
        summaryCard.addView(summaryText)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(pillButton(L.t("Kembali", "Back"), Color.parseColor("#475569")) { finish() }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
        row.addView(pillButton(L.t("Bersihkan", "Clear"), AppColors.red) { confirmClear() }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        summaryCard.addView(row)
        root.addView(summaryCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })

        root.addView(titleRow(
            L.t("Filter logs", "Log filters"),
            L.t("Filter logs", "Log filters"),
            L.t(
                "Gunakan filter level dan pencarian agar tidak perlu menggali semua log seperti arkeolog logcat yang kurang tidur.",
                "Use level filters and search so you do not have to dig through every log like a sleep-deprived logcat archaeologist."
            )
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        searchBox = EditText(this).apply {
            hint = L.t("Cari tag atau pesan log", "Search tag or log message")
            textSize = 15f
            setSingleLine(true)
            setTextColor(AppColors.textPrimary)
            setHintTextColor(AppColors.textMuted)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBg(AppColors.surface, dp(16).toFloat(), AppColors.border, dp(1))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderLogs()
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        root.addView(searchBox, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        filterRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(filterRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })

        root.addView(titleRow(
            L.t("Daftar log", "Log list"),
            L.t("Daftar log", "Log list"),
            L.t("Log terbaru tampil di atas. Gunakan All, Info, Warn, atau Error untuk mempersempit hasil.", "Newest logs are shown first. Use All, Info, Warn, or Error to narrow the results.")
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(scroll)
    }

    private fun renderFilters() {
        if (!::filterRow.isInitialized) return
        filterRow.removeAllViews()
        filterRow.addView(filterButton(L.t("Semua", "All"), null), LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(4) })
        filterRow.addView(filterButton("Info", "INFO"), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })
        filterRow.addView(filterButton("Warn", "WARN"), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })
        filterRow.addView(filterButton("Error", "ERROR"), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(4) })
    }

    private fun filterButton(label: String, level: String?): TextView {
        val active = selectedLevel == level
        return TextView(this).apply {
            text = label
            gravity = android.view.Gravity.CENTER
            textSize = 13f
            setTextColor(if (active) AppColors.goldText else AppColors.textSecondary)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = roundedBg(if (active) AppColors.chipGold else AppColors.surface, dp(14).toFloat(), if (active) AppColors.goldSoft else AppColors.border, dp(1))
            setOnClickListener {
                selectedLevel = level
                renderFilters()
                renderLogs()
            }
        }
    }

    private fun renderLogs() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val query = searchBox.text?.toString().orEmpty()
        val logs = repo.getRuntimeLogs(level = selectedLevel, query = query)
        val filterName = selectedLevel ?: L.t("Semua", "All")
        summaryText.text = when {
            logs.isEmpty() -> L.t("Tidak ada log cocok · Filter: $filterName", "No matching logs · Filter: $filterName")
            query.isBlank() -> L.t("${logs.size} log cocok · Filter: $filterName", "${logs.size} matching logs · Filter: $filterName")
            else -> L.t("${logs.size} log cocok untuk \"$query\" · Filter: $filterName", "${logs.size} matching logs for \"$query\" · Filter: $filterName")
        }
        if (logs.isEmpty()) {
            list.addView(emptyState(L.t("Tidak ada log yang cocok dengan filter ini.", "No logs match this filter.")))
            return
        }
        logs.forEach { entry -> list.addView(logCard(entry), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }) }
    }

    private fun logCard(entry: RuntimeLogEntry): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            cardBackground(this@LogsActivity)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        top.addView(chip(entry.level, levelBg(entry.level), levelColor(entry.level)))
        top.addView(TextView(this).apply {
            text = entry.tag
            textSize = 13f
            setTextColor(AppColors.textSecondary)
            setPadding(dp(8), dp(5), 0, 0)
        })
        card.addView(top)
        card.addView(TextView(this).apply {
            text = TimeText.full(entry.time)
            textSize = 12f
            setTextColor(AppColors.textMuted)
            setPadding(0, dp(8), 0, dp(8))
        })
        card.addView(TextView(this).apply {
            text = entry.message
            textSize = 14f
            setTextColor(AppColors.textPrimary)
        })
        return card
    }

    private fun levelColor(level: String): Int = when (level) {
        "ERROR" -> AppColors.red
        "WARN" -> AppColors.orange
        else -> AppColors.brand
    }

    private fun levelBg(level: String): Int = when (level) {
        "ERROR" -> AppColors.chipRed
        "WARN" -> AppColors.chipOrange
        else -> AppColors.chipBlue
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle(L.t("Bersihkan runtime logs?", "Clear runtime logs?"))
            .setMessage(L.t("Ini hanya menghapus log internal, bukan notifikasi yang dicatat.", "This only clears internal logs, not recorded notifications."))
            .setPositiveButton(L.t("Bersihkan", "Clear")) { _, _ ->
                repo.clearRuntimeLogs()
                renderLogs()
            }
            .setNegativeButton(L.t("Batal", "Cancel"), null)
            .show()
    }
}
