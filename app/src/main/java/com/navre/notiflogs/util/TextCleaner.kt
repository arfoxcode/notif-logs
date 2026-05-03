package com.navre.notiflogs.util

object TextCleaner {
    fun clean(value: CharSequence?, max: Int = 4000): String {
        if (value == null) return ""
        return value.toString()
            .replace('\u0000', ' ')
            .replace(Regex("[\\r\\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
            .take(max)
    }

    fun normalizeTitle(value: String): String {
        val cleaned = value.trim()
        return if (cleaned.isEmpty()) "(tanpa judul)" else cleaned
    }
}
