/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
package com.scrolless.app.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scrolless.app.core.data.database.ScrollessDatabase
import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.data.repository.UsageTrackerImpl
import com.scrolless.app.core.data.repository.UserSettingsStoreImpl
import com.scrolless.app.core.repository.UsageTracker
import com.scrolless.app.core.repository.UserSettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataDiModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScrollessDatabase =
        Room.databaseBuilder(context, ScrollessDatabase::class.java, "data.db")
            .addMigrations(ScrollessDatabase.MIGRATION_2_3, ScrollessDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration(true) // Not recommended but for now it shouldn't matter
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Insert default user settings row
                        db.execSQL(
                            """
                        INSERT INTO user_settings (id, active_block_option, time_limit, interval_length,
                                                   interval_window_start_at, interval_usage,
                                                   timer_overlay_enabled, last_reset_day, total_daily_usage,
                                                   reels_daily_usage, shorts_daily_usage, tiktok_daily_usage,
                                                   timer_overlay_x, timer_overlay_y, waiting_for_accessibility,
                                                   has_seen_accessibility_explainer, pause_until_at,
                                                   first_launch_at, has_seen_review_prompt)
                        VALUES (1, 'NothingSelected', 0, 0, 0, 0, 0, date('now'), 0, 0, 0, 0, 0, 100, 0, 0, 0,
                                CAST(strftime('%s','now') AS INTEGER) * 1000, 0)
                        """,
                        )
                    }
                },
            ).build()

    @Provides
    @Singleton
    fun provideUserSettingsDao(database: ScrollessDatabase): UserSettingsDao = database.userSettingsDao()

    @Provides
    @Singleton
    fun provideUserSettingsStore(userSettingsDao: UserSettingsDao): UserSettingsStore =
        UserSettingsStoreImpl(userSettingsDao = userSettingsDao)

    @Provides
    @Singleton
    fun provideUsageTracker(userSettingsStore: UserSettingsStore): UsageTracker = UsageTrackerImpl(userSettingsStore = userSettingsStore)
}
