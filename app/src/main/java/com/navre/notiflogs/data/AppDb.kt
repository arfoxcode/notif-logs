package com.navre.notiflogs.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDb private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                package_name TEXT NOT NULL,
                app_name TEXT NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                text TEXT NOT NULL DEFAULT '',
                big_text TEXT NOT NULL DEFAULT '',
                sub_text TEXT NOT NULL DEFAULT '',
                info_text TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL DEFAULT '',
                channel_id TEXT NOT NULL DEFAULT '',
                sbn_key TEXT NOT NULL DEFAULT '',
                group_key TEXT NOT NULL DEFAULT '',
                post_time INTEGER NOT NULL,
                notif_when INTEGER NOT NULL DEFAULT 0,
                captured_at INTEGER NOT NULL,
                is_ongoing INTEGER NOT NULL DEFAULT 0,
                is_clearable INTEGER NOT NULL DEFAULT 1,
                priority INTEGER NOT NULL DEFAULT 0,
                hash TEXT NOT NULL UNIQUE,
                raw TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notifications_thread ON notifications(title, app_name, post_time DESC)")
        db.execSQL("CREATE INDEX idx_notifications_search ON notifications(package_name, app_name, title, text, big_text)")
        db.execSQL("CREATE INDEX idx_notifications_time ON notifications(post_time DESC)")
        db.execSQL("CREATE INDEX idx_notifications_sbn ON notifications(sbn_key)")
        db.execSQL("CREATE INDEX idx_notifications_dedup ON notifications(package_name, title, channel_id, post_time, notif_when)")

        db.execSQL(
            """
            CREATE TABLE app_rules (
                package_name TEXT PRIMARY KEY,
                app_name TEXT NOT NULL DEFAULT '',
                mode INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE runtime_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                time INTEGER NOT NULL,
                level TEXT NOT NULL,
                tag TEXT NOT NULL,
                message TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_runtime_logs_time ON runtime_logs(time DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            runCatching { db.execSQL("ALTER TABLE notifications ADD COLUMN info_text TEXT NOT NULL DEFAULT ''") }
            runCatching { db.execSQL("ALTER TABLE notifications ADD COLUMN raw TEXT NOT NULL DEFAULT ''") }
        }
        if (oldVersion < 3) {
            runCatching { db.execSQL("CREATE INDEX IF NOT EXISTS idx_notifications_sbn ON notifications(sbn_key)") }
            runCatching { db.execSQL("CREATE INDEX IF NOT EXISTS idx_notifications_dedup ON notifications(package_name, title, channel_id, post_time, notif_when)") }
        }
        if (oldVersion < 4) {
            runCatching { db.execSQL("DROP INDEX IF EXISTS idx_notifications_thread") }
            runCatching { db.execSQL("CREATE INDEX IF NOT EXISTS idx_notifications_thread ON notifications(title, app_name, post_time DESC)") }
        }
    }

    companion object {
        private const val DB_NAME = "notif_logs.db"
        private const val DB_VERSION = 4

        @Volatile private var INSTANCE: AppDb? = null

        fun get(context: Context): AppDb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDb(context).also { INSTANCE = it }
            }
        }
    }
}
