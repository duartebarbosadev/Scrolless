/*
 * Copyright (C) 2025 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scrolless.app.core.data.database.dao.SessionSegmentDao
import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.data.database.model.SessionSegmentEntity
import com.scrolless.app.core.data.database.model.UserSettingsEntity

/**
 * The [RoomDatabase]
 */
@Database(
    entities = [
        UserSettingsEntity::class,
        SessionSegmentEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(LocalDateTypeConverters::class, BlockableAppTypeConverters::class, LocalDateTimeTypeConverters::class)
abstract class ScrollessDatabase : RoomDatabase() {
    abstract fun userSettingsDao(): UserSettingsDao

    abstract fun sessionSegmentDao(): SessionSegmentDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN reels_daily_usage INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN shorts_daily_usage INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN tiktok_daily_usage INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN first_launch_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN has_seen_review_prompt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN review_prompt_attempt_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN review_prompt_last_attempt_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE user_settings
                    SET first_launch_at = CAST(strftime('%s','now') AS INTEGER) * 1000
                    WHERE first_launch_at = 0
                    """.trimIndent(),
                )
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS session_segments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        app TEXT NOT NULL,
                        durationMillis INTEGER NOT NULL,
                        startDateTime TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_session_segments_startDateTime 
                    ON session_segments (startDateTime)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_session_segments_app_startDateTime 
                    ON session_segments (app, startDateTime)
                    """.trimIndent(),
                )
            }
        }
        // Remove legacy usage columns.
        // SQLite on Android does not support dropping columns directly, so rebuild user_settings.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Preserve per-app daily counters as migrated session rows.
                db.execSQL(
                    """
                        INSERT INTO session_segments (app, durationMillis, startDateTime)
                        SELECT 'REELS', reels_daily_usage, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
                        FROM user_settings
                        WHERE reels_daily_usage > 0
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                        INSERT INTO session_segments (app, durationMillis, startDateTime)
                        SELECT 'SHORTS', shorts_daily_usage, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
                        FROM user_settings
                        WHERE shorts_daily_usage > 0
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                        INSERT INTO session_segments (app, durationMillis, startDateTime)
                        SELECT 'TIKTOK', tiktok_daily_usage, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
                        FROM user_settings
                        WHERE tiktok_daily_usage > 0
                    """.trimIndent(),
                )

                // Preserve any residual total usage not represented by the per-app counters.
                db.execSQL(
                    """
                        INSERT INTO session_segments (app, durationMillis, startDateTime)
                        SELECT
                            'REELS',
                            CASE
                                WHEN total_daily_usage - (reels_daily_usage + shorts_daily_usage + tiktok_daily_usage) > 0
                                    THEN total_daily_usage - (reels_daily_usage + shorts_daily_usage + tiktok_daily_usage)
                                ELSE 0
                            END,
                            strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
                        FROM user_settings
                        WHERE total_daily_usage - (reels_daily_usage + shorts_daily_usage + tiktok_daily_usage) > 0
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_settings_new (
                        id INTEGER NOT NULL,
                        active_block_option TEXT NOT NULL,
                        time_limit INTEGER NOT NULL,
                        interval_length INTEGER NOT NULL,
                        interval_window_start_at INTEGER NOT NULL,
                        interval_usage INTEGER NOT NULL,
                        timer_overlay_enabled INTEGER NOT NULL,
                        timer_overlay_x INTEGER NOT NULL,
                        timer_overlay_y INTEGER NOT NULL,
                        waiting_for_accessibility INTEGER NOT NULL,
                        has_seen_accessibility_explainer INTEGER NOT NULL,
                        pause_until_at INTEGER NOT NULL,
                        first_launch_at INTEGER NOT NULL DEFAULT 0,
                        has_seen_review_prompt INTEGER NOT NULL DEFAULT 0,
                        review_prompt_attempt_count INTEGER NOT NULL DEFAULT 0,
                        review_prompt_last_attempt_at INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO user_settings_new (
                        id,
                        active_block_option,
                        time_limit,
                        interval_length,
                        interval_window_start_at,
                        interval_usage,
                        timer_overlay_enabled,
                        timer_overlay_x,
                        timer_overlay_y,
                        waiting_for_accessibility,
                        has_seen_accessibility_explainer,
                        pause_until_at,
                        first_launch_at,
                        has_seen_review_prompt,
                        review_prompt_attempt_count,
                        review_prompt_last_attempt_at
                    )
                    SELECT
                        id,
                        active_block_option,
                        time_limit,
                        interval_length,
                        interval_window_start_at,
                        interval_usage,
                        timer_overlay_enabled,
                        timer_overlay_x,
                        timer_overlay_y,
                        waiting_for_accessibility,
                        has_seen_accessibility_explainer,
                        pause_until_at,
                        COALESCE(first_launch_at, 0),
                        COALESCE(has_seen_review_prompt, 0),
                        COALESCE(review_prompt_attempt_count, 0),
                        COALESCE(review_prompt_last_attempt_at, 0)
                    FROM user_settings
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE user_settings")
                db.execSQL("ALTER TABLE user_settings_new RENAME TO user_settings")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_settings_id ON user_settings (id)")
            }
        }
    }
}
