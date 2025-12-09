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
package com.scrolless.app.core.data.repository

import com.scrolless.app.core.data.database.dao.AppUsageDao
import com.scrolless.app.core.data.database.model.AppUsage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for accessing app usage data.
 */
interface AppUsageRepository {
    /**
     * Get usage for all apps for today.
     */
    fun getTodayUsage(): Flow<List<AppUsage>>

    /**
     * Get usage for a specific date.
     */
    fun getUsageForDate(date: LocalDate): Flow<List<AppUsage>>

    /**
     * Get usage for the last N days.
     */
    fun getUsageForLastDays(days: Int): Flow<List<AppUsage>>

    /**
     * Get total usage for today (all apps combined).
     */
    fun getTotalTodayUsage(): Flow<Long>

    /**
     * Get usage grouped by app for today.
     * Returns a map of app name to usage in milliseconds.
     */
    fun getTodayUsageByApp(): Flow<Map<String, Long>>
}

@Singleton
class AppUsageRepositoryImpl @Inject constructor(
    private val appUsageDao: AppUsageDao,
) : AppUsageRepository {

    override fun getTodayUsage(): Flow<List<AppUsage>> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return appUsageDao.getUsageForDate(today)
    }

    override fun getUsageForDate(date: LocalDate): Flow<List<AppUsage>> {
        return appUsageDao.getUsageForDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    override fun getUsageForLastDays(days: Int): Flow<List<AppUsage>> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong() - 1)
        return appUsageDao.getUsageForDateRange(
            startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
        )
    }

    override fun getTotalTodayUsage(): Flow<Long> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return appUsageDao.getTotalUsageForDate(today)
    }

    override fun getTodayUsageByApp(): Flow<Map<String, Long>> {
        return getTodayUsage().map { usageList ->
            usageList.associate { it.appName to it.usageMillis }
        }
    }
}
