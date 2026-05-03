package com.navre.notiflogs.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.navre.notiflogs.data.AppPrefs
import com.navre.notiflogs.util.AppColors
import com.navre.notiflogs.util.L
import com.navre.notiflogs.util.bold
import com.navre.notiflogs.util.cardBackground
import com.navre.notiflogs.util.chip
import com.navre.notiflogs.util.dp
import com.navre.notiflogs.util.pillButton
import com.navre.notiflogs.util.roundedBg
import com.navre.notiflogs.util.titleRow

class AboutActivity : Activity() {
    private lateinit var prefs: AppPrefs
    private var lastDarkMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        lastDarkMode = prefs.darkMode
        AppColors.apply(lastDarkMode)
        buildUi()
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

        root.addView(
            titleRow(
                title = L.t("Tentang", "About"),
                infoTitle = L.t("Tentang aplikasi", "About app"),
                infoMessage = L.t(
                    "Halaman ini berisi informasi author, link sosial, dan tujuan aplikasi Notif Logs dibuat.",
                    "This page contains author information, social links, and the purpose of Notif Logs."
                ),
                textSize = 28f
            ),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        )

        root.addView(appCard(), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        root.addView(authorCard(), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        root.addView(purposeCard(), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        root.addView(linkCard(), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        root.addView(pillButton(L.t("Kembali", "Back"), AppColors.brand) { finish() }, LinearLayout.LayoutParams(-1, -2))
        setContentView(scroll)
    }

    private fun appCard(): LinearLayout {
        val card = baseCard()
        card.addView(TextView(this).apply {
            text = "Notif Logs"
            textSize = 24f
            setTextColor(AppColors.textPrimary)
            bold()
        })
        card.addView(TextView(this).apply {
            text = L.t(
                "Pencatat dan pengelola riwayat notifikasi lokal.",
                "Local notification history recorder and manager."
            )
            textSize = 14f
            setTextColor(AppColors.textSecondary)
            setPadding(0, dp(6), 0, dp(10))
        })
        val chipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chipRow.addView(chip("Kotlin", AppColors.chipGold, AppColors.goldText))
        chipRow.addView(chip("Offline-first", AppColors.chipBlue, AppColors.brandDark), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        chipRow.addView(chip(L.t("Privat", "Private"), AppColors.chipGreen, AppColors.green), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        card.addView(chipRow)
        return card
    }

    private fun authorCard(): LinearLayout {
        val card = baseCard()
        card.addView(titleRow(
            title = L.t("Author", "Author"),
            infoTitle = L.t("Author", "Author"),
            infoMessage = L.t(
                "Identitas pembuat aplikasi dan username sosial yang digunakan.",
                "App creator identity and social usernames."
            )
        ))
        card.addView(infoLine(L.t("Username", "Username"), "arfoxcode"))
        card.addView(infoLine("GitHub", "@arfoxcode"))
        card.addView(infoLine("Instagram", "@arfoxcode"))
        return card
    }

    private fun purposeCard(): LinearLayout {
        val card = baseCard()
        card.addView(titleRow(
            title = L.t("Tujuan aplikasi", "App purpose"),
            infoTitle = L.t("Tujuan aplikasi", "App purpose"),
            infoMessage = L.t(
                "Ringkasan alasan Notif Logs dibuat.",
                "Summary of why Notif Logs was created."
            )
        ))
        card.addView(paragraph(L.t(
            "Notif Logs dibuat untuk membantu pengguna menyimpan riwayat notifikasi secara lokal, mengelompokkan catatan berdasarkan judul dan nama aplikasi, serta memudahkan pencarian notifikasi penting yang mungkin terlewat.",
            "Notif Logs was created to help users store notification history locally, group records by title and app name, and make important missed notifications easier to find."
        )))
        card.addView(paragraph(L.t(
            "Aplikasi ini dirancang offline-first. Data notifikasi disimpan di perangkat dan tidak dikirim ke server luar. Karena rupanya privasi masih konsep yang layak dipertahankan, walau dunia digital sering berpura-pura lupa.",
            "The app is designed offline-first. Notification data is stored on the device and is not sent to external servers. Privacy, shockingly, is still worth defending."
        )))
        return card
    }

    private fun linkCard(): LinearLayout {
        val card = baseCard()
        card.addView(titleRow(
            title = L.t("Link author", "Author links"),
            infoTitle = L.t("Link author", "Author links"),
            infoMessage = L.t(
                "Tombol ini membuka profil author di aplikasi browser atau aplikasi terkait.",
                "These buttons open the author's profiles in a browser or related app."
            )
        ))
        card.addView(pillButton("GitHub: arfoxcode", AppColors.brand) {
            openUrl("https://github.com/arfoxcode")
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        card.addView(pillButton("Instagram: arfoxcode", AppColors.brandDark) {
            openUrl("https://instagram.com/arfoxcode")
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        return card
    }

    private fun baseCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            cardBackground(this@AboutActivity)
        }
    }

    private fun paragraph(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 14f
            setTextColor(AppColors.textSecondary)
            setPadding(0, dp(10), 0, 0)
        }
    }

    private fun infoLine(label: String, value: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            addView(TextView(context).apply {
                text = label
                textSize = 14f
                setTextColor(AppColors.textSecondary)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(context).apply {
                text = value
                textSize = 14f
                setTextColor(AppColors.textPrimary)
                bold()
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(
                    this,
                    L.t("Tidak ada aplikasi untuk membuka link.", "No app available to open the link."),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
