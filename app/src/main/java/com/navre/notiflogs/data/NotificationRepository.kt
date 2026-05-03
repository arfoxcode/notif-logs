package com.navre.notiflogs.data

import android.app.Notification
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.navre.notiflogs.util.Hashing
import com.navre.notiflogs.util.L
import com.navre.notiflogs.util.TextCleaner
import org.json.JSONObject

class NotificationRepository(private val context: Context) {
    private val db get() = AppDb.get(context).writableDatabase
    private val prefs by lazy { AppPrefs(context) }

    fun saveFromStatusBarNotification(sbn: StatusBarNotification): Boolean {
        prefs.markCaptured()

        if (sbn.packageName == context.packageName) {
            prefs.markSkipped(L.t("Skip notifikasi dari aplikasi sendiri", "Skipped notification from this app"))
            return false
        }

        val appName = getAppName(sbn.packageName)
        val rule = getRuleMode(sbn.packageName)
        if (!isAllowedByRules(rule)) {
            prefs.markSkipped(L.t("Skip ${appName}: diblokir rules atau belum Allow pada mode whitelist", "Skip ${appName}: blocked by rules or not allowed in whitelist mode"))
            return false
        }

        val notification = sbn.notification ?: run {
            prefs.markSkipped(L.t("Skip ${appName}: notification null", "Skip ${appName}: notification is null"))
            return false
        }

        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary && prefs.ignoreGroupSummary) {
            prefs.markSkipped(L.t("Skip ${appName}: group summary diabaikan", "Skip ${appName}: group summary ignored"))
            return false
        }

        val ongoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (ongoing && !prefs.captureOngoing) {
            prefs.markSkipped(L.t("Skip ${appName}: ongoing notification tidak dicatat", "Skip ${appName}: ongoing notification not recorded"))
            return false
        }

        val extras = notification.extras
        val title = TextCleaner.clean(extras.getCharSequence(Notification.EXTRA_TITLE), 700)
        val normalText = TextCleaner.clean(extras.getCharSequence(Notification.EXTRA_TEXT), 2500)
        val bigText = TextCleaner.clean(extras.getCharSequence(Notification.EXTRA_BIG_TEXT), 4000)
        val subText = TextCleaner.clean(extras.getCharSequence(Notification.EXTRA_SUB_TEXT), 700)
        val infoText = TextCleaner.clean(extras.getCharSequence(Notification.EXTRA_INFO_TEXT), 700)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n") { TextCleaner.clean(it, 700) }
            ?: ""
        val messagingText = if (prefs.parseMessagingStyle) parseMessagingText(extras) else ""

        val category = notification.category ?: ""
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notification.channelId ?: "" else ""
        val groupKey = sbn.groupKey ?: ""
        val postTime = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
        val notificationWhen = notification.`when`.takeIf { it > 0L } ?: postTime
        val capturedAt = System.currentTimeMillis()
        val normalizedTitle = TextCleaner.normalizeTitle(title)
        val storedText = when {
            messagingText.isNotBlank() -> messagingText
            normalText.isNotBlank() -> normalText
            textLines.isNotBlank() -> textLines
            else -> ""
        }
        val sbnKey = sbn.key ?: ""

        if (normalizedTitle.isBlank() && storedText.isBlank() && bigText.isBlank() && subText.isBlank() && infoText.isBlank()) {
            prefs.markSkipped(L.t("Skip ${appName}: tidak ada judul atau teks yang bisa dicatat", "Skip ${appName}: no title or text to record"))
            return false
        }

        val raw = JSONObject().apply {
            put("package", sbn.packageName)
            put("app", appName)
            put("title", title)
            put("text", normalText)
            put("bigText", bigText)
            put("textLines", textLines)
            put("messagingText", messagingText)
            put("subText", subText)
            put("infoText", infoText)
            put("category", category)
            put("channelId", channelId)
            put("groupKey", groupKey)
            put("postTime", postTime)
            put("notificationWhen", notificationWhen)
            put("capturedAt", capturedAt)
            put("isOngoing", ongoing)
            put("isClearable", sbn.isClearable)
            put("isGroupSummary", isGroupSummary)
            put("sbnKey", sbnKey)
        }.toString()

        val hash = Hashing.sha256(
            listOf(
                sbn.packageName,
                normalizedTitle,
                storedText,
                bigText,
                subText,
                infoText,
                channelId,
                sbnKey,
                postTime.toString(),
                notificationWhen.toString()
            ).joinToString("|")
        )

        val values = ContentValues().apply {
            put("package_name", sbn.packageName)
            put("app_name", appName)
            put("title", normalizedTitle)
            put("text", storedText)
            put("big_text", bigText)
            put("sub_text", subText)
            put("info_text", infoText)
            put("category", category)
            put("channel_id", channelId)
            put("sbn_key", sbnKey)
            put("group_key", groupKey)
            put("post_time", postTime)
            put("notif_when", notificationWhen)
            put("captured_at", capturedAt)
            put("is_ongoing", if (ongoing) 1 else 0)
            put("is_clearable", if (sbn.isClearable) 1 else 0)
            put("priority", notification.priority)
            put("hash", hash)
            put("raw", raw)
        }

        val database = db
        var inserted = false
        database.beginTransaction()
        try {
            val duplicate = isDuplicateCandidate(
                database = database,
                packageName = sbn.packageName,
                title = normalizedTitle,
                text = storedText,
                bigText = bigText,
                subText = subText,
                infoText = infoText,
                channelId = channelId,
                sbnKey = sbnKey,
                postTime = postTime,
                notificationWhen = notificationWhen,
                capturedAt = capturedAt
            )
            if (duplicate) {
                prefs.markSkipped(L.t("Skip ${appName}: duplikat terdeteksi (${prefs.dedupModeLabel()})", "Skip ${appName}: duplicate detected (${prefs.dedupModeLabel()})"))
            } else {
                val rowId = database.insertWithOnConflict("notifications", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                inserted = rowId != -1L
                if (inserted) prefs.markSaved() else prefs.markSkipped(L.t("Skip ${appName}: hash sudah ada di database", "Skip ${appName}: hash already exists in database"))
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        return inserted
    }

    fun getThreads(query: String = "", limit: Int = 200): List<ThreadSummary> {
        val result = mutableListOf<ThreadSummary>()
        val args = mutableListOf<String>()
        val where = if (query.isBlank()) {
            ""
        } else {
            args += "%$query%"
            args += "%$query%"
            args += "%$query%"
            args += "%$query%"
            "WHERE title LIKE ? OR app_name LIKE ? OR text LIKE ? OR big_text LIKE ?"
        }
        val sql = """
            SELECT
                MAX(package_name) AS package_name,
                app_name,
                title,
                COUNT(*) AS count_items,
                MAX(post_time) AS last_time
            FROM notifications
            $where
            GROUP BY title, app_name
            ORDER BY last_time DESC
            LIMIT $limit
        """.trimIndent()
        db.rawQuery(sql, args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                val pkg = cursor.s("package_name")
                val app = cursor.s("app_name")
                val title = cursor.s("title")
                result += ThreadSummary(
                    packageName = pkg,
                    appName = app,
                    title = title,
                    count = cursor.i("count_items"),
                    lastTime = cursor.l("last_time"),
                    lastText = getLastText(app, title)
                )
            }
        }
        return result
    }

    fun getMessages(appName: String, title: String, search: String = "", limit: Int = 600): List<NotificationRecord> {
        val result = mutableListOf<NotificationRecord>()
        val args = mutableListOf(appName, title)
        val searchSql = if (search.isBlank()) "" else {
            args += "%$search%"
            args += "%$search%"
            "AND (text LIKE ? OR big_text LIKE ?)"
        }
        val sql = """
            SELECT * FROM notifications
            WHERE app_name = ? AND title = ? $searchSql
            ORDER BY post_time ASC, id ASC
            LIMIT $limit
        """.trimIndent()
        db.rawQuery(sql, args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) result += cursor.toRecord()
        }
        return result
    }

    fun cleanupDuplicateNotifications(mode: Int = prefs.dedupMode): Int {
        var deleted = 0
        val database = db
        database.beginTransaction()
        try {
            // Safe cleanup: exact duplicate rows only.
            deleted += database.delete(
                "notifications",
                """
                id NOT IN (
                    SELECT MIN(id)
                    FROM notifications
                    GROUP BY package_name, title, text, big_text, sub_text, info_text, channel_id, post_time, notif_when
                )
                """.trimIndent(),
                null
            )

            // Normal cleanup: same rendered content and same active notification key.
            if (mode >= AppPrefs.DEDUP_NORMAL) {
                deleted += database.delete(
                    "notifications",
                    """
                    sbn_key != '' AND id NOT IN (
                        SELECT MIN(id)
                        FROM notifications
                        WHERE sbn_key != ''
                        GROUP BY package_name, title, text, big_text, sub_text, info_text, channel_id, sbn_key
                    )
                    """.trimIndent(),
                    null
                )
            }

            // Aggressive cleanup: same rendered content inside a small time bucket.
            if (mode >= AppPrefs.DEDUP_AGGRESSIVE) {
                val bucket = (prefs.dedupWindowMs / 1000L).coerceAtLeast(5L)
                deleted += database.delete(
                    "notifications",
                    """
                    id NOT IN (
                        SELECT MIN(id)
                        FROM notifications
                        GROUP BY package_name, title, text, big_text, sub_text, info_text, channel_id, (post_time / ($bucket * 1000))
                    )
                    """.trimIndent(),
                    null
                )
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        return deleted
    }

    fun getDashboardStats(): DashboardStats {
        val totalNotifications = db.compileStatement("SELECT COUNT(*) FROM notifications").simpleQueryForLong().toInt()
        val totalThreads = db.compileStatement("SELECT COUNT(*) FROM (SELECT 1 FROM notifications GROUP BY title, app_name) AS t").simpleQueryForLong().toInt()
        val totalApps = db.compileStatement("SELECT COUNT(*) FROM (SELECT 1 FROM notifications GROUP BY package_name) AS t").simpleQueryForLong().toInt()
        return DashboardStats(totalNotifications, totalThreads, totalApps)
    }

    fun getDiagnosticInfo(listenerEnabled: Boolean, batteryExempt: Boolean): DiagnosticInfo {
        val stats = getDashboardStats()
        return DiagnosticInfo(
            listenerEnabled = listenerEnabled,
            batteryExempt = batteryExempt,
            totalNotifications = stats.totalNotifications,
            totalThreads = stats.totalThreads,
            totalApps = stats.totalApps,
            lastCaptureAt = prefs.lastCaptureAt,
            lastSavedAt = prefs.lastSavedAt,
            lastSkipAt = prefs.lastSkipAt,
            lastSkipReason = prefs.lastSkipReason,
            lastListenerConnectedAt = prefs.lastListenerConnectedAt,
            lastListenerDisconnectedAt = prefs.lastListenerDisconnectedAt,
            lastWatchdogTickAt = prefs.lastWatchdogTickAt,
            lastWatchdogStartAt = prefs.lastWatchdogStartAt,
            lastHeartbeatAt = prefs.lastHeartbeatAt,
            lastSelfHealAt = prefs.lastSelfHealAt,
            lastServiceStartFailureAt = prefs.lastServiceStartFailureAt,
            lastServiceStartFailureReason = prefs.lastServiceStartFailureReason,
            dedupMode = prefs.dedupModeLabel(),
            dedupWindowMs = prefs.dedupWindowMs,
            ignoreGroupSummary = prefs.ignoreGroupSummary,
            parseMessagingStyle = prefs.parseMessagingStyle,
            captureOngoing = prefs.captureOngoing,
            keepAliveEnabled = prefs.keepAliveEnabled,
            aggressiveSelfHeal = prefs.aggressiveSelfHeal,
            debugLogging = prefs.debugLogging,
            retentionDays = prefs.retentionDays
        )
    }

    fun getObservedApps(): List<AppRule> {
        val apps = linkedMapOf<String, AppRule>()
        db.rawQuery(
            """
            SELECT package_name, MAX(app_name) AS app_name, COUNT(*) AS count_items
            FROM notifications
            GROUP BY package_name
            ORDER BY app_name COLLATE NOCASE ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val pkg = cursor.s("package_name")
                apps[pkg] = AppRule(
                    packageName = pkg,
                    appName = cursor.s("app_name"),
                    mode = getRuleMode(pkg),
                    count = cursor.i("count_items")
                )
            }
        }
        db.rawQuery("SELECT package_name, app_name, mode FROM app_rules ORDER BY app_name COLLATE NOCASE ASC", null).use { cursor ->
            while (cursor.moveToNext()) {
                val pkg = cursor.s("package_name")
                if (!apps.containsKey(pkg)) apps[pkg] = AppRule(pkg, cursor.s("app_name"), cursor.i("mode"), 0)
            }
        }
        return apps.values.toList()
    }

    fun setRule(packageName: String, appName: String, mode: Int) {
        val values = ContentValues().apply {
            put("package_name", packageName)
            put("app_name", appName.ifBlank { getAppName(packageName) })
            put("mode", mode)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("app_rules", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getRuleMode(packageName: String): Int {
        db.rawQuery("SELECT mode FROM app_rules WHERE package_name = ? LIMIT 1", arrayOf(packageName)).use { cursor ->
            return if (cursor.moveToFirst()) cursor.i("mode") else AppRule.MODE_NORMAL
        }
    }

    fun isAllowedByRules(mode: Int): Boolean {
        return if (prefs.whitelistMode) mode == AppRule.MODE_ALLOW else mode != AppRule.MODE_BLOCK
    }

    fun getRuntimeLogs(level: String? = null, query: String = "", limit: Int = 250): List<RuntimeLogEntry> {
        val logs = mutableListOf<RuntimeLogEntry>()
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        val cleanLevel = level?.uppercase()?.takeIf { it in setOf("INFO", "WARN", "ERROR") }
        if (cleanLevel != null) {
            where += "level = ?"
            args += cleanLevel
        }
        val cleanQuery = query.trim()
        if (cleanQuery.isNotEmpty()) {
            where += "(tag LIKE ? OR message LIKE ? OR level LIKE ?)"
            val like = "%$cleanQuery%"
            args += like
            args += like
            args += like
        }
        val sql = buildString {
            append("SELECT * FROM runtime_logs ")
            if (where.isNotEmpty()) append("WHERE ").append(where.joinToString(" AND ")).append(' ')
            append("ORDER BY time DESC LIMIT ?")
        }
        args += limit.coerceIn(50, 1000).toString()
        db.rawQuery(sql, args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                logs += RuntimeLogEntry(cursor.l("id"), cursor.l("time"), cursor.s("level"), cursor.s("tag"), cursor.s("message"))
            }
        }
        return logs
    }

    fun deleteThread(appName: String, title: String): Int {
        return db.delete("notifications", "app_name = ? AND title = ?", arrayOf(appName, title))
    }

    fun clearRuntimeLogs() {
        db.delete("runtime_logs", null, null)
    }

    fun pruneOldIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - prefs.lastPruneAt < 12L * 60L * 60L * 1000L) return
        pruneOld()
        prefs.lastPruneAt = now
    }

    fun pruneOld(): Int {
        val cutoff = System.currentTimeMillis() - prefs.retentionDays * 24L * 60L * 60L * 1000L
        return db.delete("notifications", "post_time < ?", arrayOf(cutoff.toString()))
    }

    private fun isDuplicateCandidate(
        database: SQLiteDatabase,
        packageName: String,
        title: String,
        text: String,
        bigText: String,
        subText: String,
        infoText: String,
        channelId: String,
        sbnKey: String,
        postTime: Long,
        notificationWhen: Long,
        capturedAt: Long
    ): Boolean {
        val args = mutableListOf(packageName, title, text, bigText, subText, infoText, channelId)
        val duplicateChecks = mutableListOf<String>()

        when (prefs.dedupMode) {
            AppPrefs.DEDUP_SAFE -> {
                if (sbnKey.isNotBlank()) {
                    duplicateChecks += "sbn_key = ?"
                    args += sbnKey
                }
                duplicateChecks += "(post_time = ? AND notif_when = ?)"
                args += postTime.toString()
                args += notificationWhen.toString()
            }
            AppPrefs.DEDUP_AGGRESSIVE -> {
                if (sbnKey.isNotBlank()) {
                    duplicateChecks += "sbn_key = ?"
                    args += sbnKey
                }
                duplicateChecks += "post_time = ?"
                args += postTime.toString()
                duplicateChecks += "notif_when = ?"
                args += notificationWhen.toString()
                duplicateChecks += "captured_at >= ?"
                args += (capturedAt - prefs.dedupWindowMs).toString()
            }
            else -> {
                if (sbnKey.isNotBlank()) {
                    duplicateChecks += "sbn_key = ?"
                    args += sbnKey
                }
                duplicateChecks += "post_time = ?"
                args += postTime.toString()
                duplicateChecks += "notif_when = ?"
                args += notificationWhen.toString()
                duplicateChecks += "captured_at >= ?"
                args += (capturedAt - prefs.dedupWindowMs).toString()
            }
        }

        if (duplicateChecks.isEmpty()) return false
        val sql = """
            SELECT id FROM notifications
            WHERE package_name = ?
              AND title = ?
              AND text = ?
              AND big_text = ?
              AND sub_text = ?
              AND info_text = ?
              AND channel_id = ?
              AND (${duplicateChecks.joinToString(" OR ")})
            LIMIT 1
        """.trimIndent()
        database.rawQuery(sql, args.toTypedArray()).use { cursor -> return cursor.moveToFirst() }
    }

    @Suppress("DEPRECATION")
    private fun parseMessagingText(extras: Bundle): String {
        val items = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return ""
        val lines = mutableListOf<String>()
        for (item in items) {
            val bundle = item as? Bundle ?: continue
            val text = TextCleaner.clean(bundle.getCharSequence("text"), 1800)
            if (text.isBlank()) continue
            val senderObj = runCatching { bundle.get("sender") }.getOrNull()
            val sender = if (senderObj is CharSequence) TextCleaner.clean(senderObj, 300) else ""
            lines += if (sender.isNotBlank()) "$sender: $text" else text
        }
        return lines.joinToString("\n").take(4000)
    }

    private fun getLastText(appName: String, title: String): String {
        db.rawQuery(
            "SELECT text, big_text FROM notifications WHERE app_name = ? AND title = ? ORDER BY post_time DESC, id DESC LIMIT 1",
            arrayOf(appName, title)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return ""
            return cursor.s("text").ifBlank { cursor.s("big_text") }
        }
    }

    private fun getAppName(packageName: String): String {
        return runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    private fun Cursor.toRecord(): NotificationRecord {
        return NotificationRecord(
            id = l("id"),
            packageName = s("package_name"),
            appName = s("app_name"),
            title = s("title"),
            text = s("text"),
            bigText = s("big_text"),
            subText = s("sub_text"),
            infoText = s("info_text"),
            category = s("category"),
            channelId = s("channel_id"),
            sbnKey = s("sbn_key"),
            groupKey = s("group_key"),
            postTime = l("post_time"),
            notificationWhen = l("notif_when"),
            capturedAt = l("captured_at"),
            isOngoing = i("is_ongoing") == 1,
            isClearable = i("is_clearable") == 1,
            priority = i("priority"),
            hash = s("hash"),
            raw = s("raw")
        )
    }

    private fun Cursor.s(column: String): String = getString(getColumnIndexOrThrow(column)) ?: ""
    private fun Cursor.i(column: String): Int = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.l(column: String): Long = getLong(getColumnIndexOrThrow(column))
}
