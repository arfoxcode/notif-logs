package com.navre.notiflogs.util

import java.util.Locale

object L {
    fun isIndonesian(): Boolean {
        val language = Locale.getDefault().language.lowercase(Locale.ROOT)
        return language == "id" || language == "in"
    }

    fun t(id: String, en: String): String = if (isIndonesian()) id else en

    fun active(value: Boolean): String = if (value) t("AKTIF", "ON") else t("MATI", "OFF")
    fun yesNo(value: Boolean): String = if (value) t("Aktif", "On") else t("Mati", "Off")
    fun none(): String = t("Belum ada", "None yet")
    fun untitled(): String = t("(Tanpa judul)", "(No title)")
    fun noText(): String = t("(Tanpa isi teks)", "(No text)")
    fun hiddenPreview(): String = t("Preview disembunyikan", "Preview hidden")
}
