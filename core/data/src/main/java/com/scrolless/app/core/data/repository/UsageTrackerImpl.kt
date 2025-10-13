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

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class UsageTrackerImpl @Inject constructor(private val userSettingsStore: UserSettingsStore) : UsageTracker {

    override fun getDailyUsage(): Long {
        return (userSettingsStore.getTotalDailyUsage() as StateFlow<Long>).value
    }

    override suspend fun addToDailyUsage(sessionTime: Long) {
        val current = (userSettingsStore.getTotalDailyUsage() as StateFlow<Long>).value
        val newTotal = current + sessionTime
        userSettingsStore.updateTotalDailyUsage(newTotal)
    }

    override suspend fun checkDailyReset() {
        val currentDay = LocalDate.now()
        val lastDay = (userSettingsStore.getLastResetDay() as StateFlow<LocalDate>).value

        if (currentDay != lastDay) {
            // Reset usage to 0 and update last reset day
            userSettingsStore.updateTotalDailyUsage(0L)
            userSettingsStore.setLastResetDay(currentDay)
        }
    }
}
