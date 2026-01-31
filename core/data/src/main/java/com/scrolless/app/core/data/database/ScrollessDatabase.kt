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
    version = 4,
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
    }
}
