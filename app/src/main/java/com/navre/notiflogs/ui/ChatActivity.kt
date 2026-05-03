package com.navre.notiflogs.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.data.NotificationRecord
import com.navre.notiflogs.data.NotificationRepository
import com.navre.notiflogs.util.AppColors
import com.navre.notiflogs.util.L
import com.navre.notiflogs.util.TimeText
import com.navre.notiflogs.util.bodyText
import com.navre.notiflogs.util.bold
import com.navre.notiflogs.util.chip
import com.navre.notiflogs.util.dp
import com.navre.notiflogs.util.emptyState
import com.navre.notiflogs.util.pillButton
import com.navre.notiflogs.util.roundedBg
import com.navre.notiflogs.util.sectionTitle

class ChatActivity : Activity() {
    private lateinit var repo: NotificationRepository
    private lateinit var prefs: AppPrefs
    private lateinit var packageNameArg: String
    private lateinit var titleArg: String
    private lateinit var appNameArg: String
    private lateinit var container: LinearLayout
    private lateinit var searchBox: EditText
    private lateinit var scroll: ScrollView
    private lateinit var infoText: TextView
    private var lastDarkMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = NotificationRepository(this)
        prefs = AppPrefs(this)
        lastDarkMode = prefs.darkMode
        AppColors.apply(lastDarkMode)
        packageNameArg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        titleArg = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        appNameArg = intent.getStringExtra(EXTRA_APP).orEmpty()
        buildUi()
        renderMessages()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.darkMode != lastDarkMode) recreate()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(16), dp(14), dp(12))
            setBackgroundColor(AppColors.bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(AppColors.surface, dp(20).toFloat(), AppColors.border, dp(1))
            elevation = dp(1).toFloat()
        }
        header.addView(TextView(this).apply {
            text = titleArg.ifBlank { L.untitled() }
            textSize = 24f
            setTextColor(AppColors.textPrimary)
            bold()
        })
        header.addView(TextView(this).apply {
            text = appNameArg.ifBlank { packageNameArg }
            textSize = 14f
            setTextColor(AppColors.textSecondary)
            setPadding(0, dp(2), 0, dp(10))
        })
        infoText = TextView(this).apply {
            textSize = 13f
            setTextColor(AppColors.textSecondary)
            setPadding(0, 0, 0, dp(10))
        }
        header.addView(infoText)
        val buttonRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttonRow.addView(pillButton(L.t("Kembali", "Back"), AppColors.brand) { finish() }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
        buttonRow.addView(pillButton(L.t("Hapus thread", "Delete thread"), AppColors.red) { confirmDelete() }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        header.addView(buttonRow)
        root.addView(header, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        root.addView(sectionTitle(L.t("Cari di dalam thread", "Search in thread")))
        searchBox = EditText(this).apply {
            hint = L.t("Cari teks notifikasi", "Search notification text")
            setSingleLine(true)
            textSize = 15f
            setTextColor(AppColors.textPrimary)
            setHintTextColor(AppColors.textMuted)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBg(AppColors.surface, dp(16).toFloat(), AppColors.border, dp(1))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderMessages()
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        root.addView(searchBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(12) })

        scroll = ScrollView(this)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        scroll.addView(container)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
    }

    private fun renderMessages() {
        if (!::container.isInitialized) return
        container.removeAllViews()
        val messages = repo.getMessages(appNameArg.ifBlank { packageNameArg }, titleArg, searchBox.text?.toString().orEmpty())
        infoText.text = if (messages.isEmpty()) {
            L.t("Belum ada pesan yang cocok.", "No matching messages.")
        } else {
            L.t("Menampilkan ${messages.size} pesan.", "Showing ${messages.size} messages.")
        }
        if (messages.isEmpty()) {
            container.addView(emptyState(L.t("Tidak ada pesan yang cocok.", "No matching messages.")))
            return
        }
        messages.forEachIndexed { index, item -> container.addView(messageBubble(item, index)) }
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun messageBubble(item: NotificationRecord, index: Int): LinearLayout {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBg(AppColors.surface, dp(18).toFloat(), AppColors.border, dp(1))
            elevation = dp(1).toFloat()
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(chip(TimeText.short(item.postTime), AppColors.chipGray, AppColors.textSecondary))
        if (item.channelId.isNotBlank()) {
            head.addView(chip(item.channelId, AppColors.chipBlue, AppColors.brandDark), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        }
        if (item.isOngoing) {
            head.addView(chip("ongoing", AppColors.chipOrange, AppColors.orange), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        }
        bubble.addView(head)
        bubble.addView(TextView(this).apply {
            text = item.text.ifBlank { item.bigText.ifBlank { L.noText() } }
            textSize = 15f
            setTextColor(AppColors.textPrimary)
            setPadding(0, dp(10), 0, 0)
        })
        if (item.subText.isNotBlank()) {
            bubble.addView(TextView(this).apply {
                text = L.t("Subteks: ${item.subText}", "Subtext: ${item.subText}")
                textSize = 13f
                setTextColor(AppColors.textSecondary)
                setPadding(0, dp(8), 0, 0)
            })
        }
        if (item.infoText.isNotBlank()) {
            bubble.addView(TextView(this).apply {
                text = L.t("Info: ${item.infoText}", "Info: ${item.infoText}")
                textSize = 13f
                setTextColor(AppColors.textSecondary)
                setPadding(0, dp(4), 0, 0)
            })
        }
        bubble.addView(TextView(this).apply {
            text = L.t("Tertangkap: ${TimeText.full(item.capturedAt)}", "Captured: ${TimeText.full(item.capturedAt)}")
            textSize = 11f
            setTextColor(AppColors.textMuted)
            setPadding(0, dp(10), 0, 0)
        })
        wrapper.addView(bubble, LinearLayout.LayoutParams((resources.displayMetrics.widthPixels * 0.82f).toInt(), -2).apply {
            bottomMargin = dp(8)
        })
        return wrapper
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(L.t("Hapus thread?", "Delete thread?"))
            .setMessage(L.t("Semua notifikasi untuk judul ini akan dihapus dari database lokal.", "All notifications for this title will be deleted from the local database."))
            .setPositiveButton(L.t("Hapus", "Delete")) { _, _ ->
                repo.deleteThread(appNameArg.ifBlank { packageNameArg }, titleArg)
                finish()
            }
            .setNegativeButton(L.t("Batal", "Cancel"), null)
            .show()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_APP = "extra_app"
    }
}
