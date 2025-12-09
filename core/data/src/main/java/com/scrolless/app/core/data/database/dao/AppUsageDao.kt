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
package com.scrolless.app.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.scrolless.app.core.data.database.model.AppUsage
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [AppUsage] related operations.
 */
@Dao
interface AppUsageDao {

    @Upsert
    suspend fun upsert(appUsage: AppUsage)

    @Query("SELECT * FROM app_usage WHERE date = :date")
    fun getUsageForDate(date: String): Flow<List<AppUsage>>

    @Query("SELECT * FROM app_usage WHERE date = :date AND app_name = :appName")
    suspend fun getUsageForAppAndDate(appName: String, date: String): AppUsage?

    @Query("SELECT * FROM app_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getUsageForDateRange(startDate: String, endDate: String): Flow<List<AppUsage>>

    @Query("SELECT COALESCE(SUM(usage_millis), 0) FROM app_usage WHERE date = :date")
    fun getTotalUsageForDate(date: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(usage_millis), 0) FROM app_usage WHERE date = :date AND app_name = :appName")
    fun getUsageForApp(appName: String, date: String): Flow<Long>

    @Query("UPDATE app_usage SET usage_millis = usage_millis + :additionalMillis WHERE app_name = :appName AND date = :date")
    suspend fun addUsage(appName: String, date: String, additionalMillis: Long): Int

    @Query("DELETE FROM app_usage WHERE date < :beforeDate")
    suspend fun deleteOldData(beforeDate: String)
}
