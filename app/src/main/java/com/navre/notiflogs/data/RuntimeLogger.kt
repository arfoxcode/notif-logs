package com.navre.notiflogs.data

import android.content.ContentValues
import android.content.Context
import android.util.Log

object RuntimeLogger {
    fun i(context: Context, tag: String, message: String) = write(context, "INFO", tag, message)
    fun w(context: Context, tag: String, message: String) = write(context, "WARN", tag, message)
    fun e(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        val full = if (throwable == null) message else "$message: ${throwable.message ?: throwable.javaClass.simpleName}"
        write(context, "ERROR", tag, full)
    }

    private fun write(context: Context, level: String, tag: String, message: String) {
        when (level) {
            "ERROR" -> Log.e(tag, message)
            "WARN" -> Log.w(tag, message)
            else -> Log.i(tag, message)
        }

        val prefs = AppPrefs(context)
        if (level == "INFO" && !prefs.debugLogging && !message.contains("aktif", ignoreCase = true) && !message.contains("active", ignoreCase = true) && !message.contains("tersambung", ignoreCase = true) && !message.contains("connected", ignoreCase = true)) {
            return
        }

        runCatching {
            val db = AppDb.get(context).writableDatabase
            val values = ContentValues().apply {
                put("time", System.currentTimeMillis())
                put("level", level)
                put("tag", tag.take(48))
                put("message", message.take(1000))
            }
            db.insert("runtime_logs", null, values)
            db.delete(
                "runtime_logs",
                "id NOT IN (SELECT id FROM runtime_logs ORDER BY time DESC LIMIT 500)",
                null
            )
        }
    }
}
