package com.navre.notiflogs.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeText {
    private val dateTime = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
    private val shortTime = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    fun full(millis: Long): String = if (millis <= 0L) "-" else dateTime.format(Date(millis))
    fun short(millis: Long): String = if (millis <= 0L) "-" else shortTime.format(Date(millis))
}
