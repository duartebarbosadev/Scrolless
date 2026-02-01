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

import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.repository.SessionSegmentStore
import com.scrolless.app.core.repository.UsageTracker
import com.scrolless.app.core.repository.UserSettingsStore
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SessionTrackerImpl @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
    private val sessionSegmentStore: SessionSegmentStore,
) : UsageTracker {

    private val usageMutex = Mutex()

    override fun getDailyUsage(): Long {
        return (userSettingsStore.getTotalDailyUsage() as StateFlow<Long>).value
    }

    override fun getAppDailyUsage(app: BlockableApp): Long {
        return when (app) {
            BlockableApp.REELS -> (userSettingsStore.getReelsDailyUsage() as StateFlow<Long>).value
            BlockableApp.SHORTS -> (userSettingsStore.getShortsDailyUsage() as StateFlow<Long>).value
            BlockableApp.TIKTOK -> (userSettingsStore.getTiktokDailyUsage() as StateFlow<Long>).value
        }
    }

    override suspend fun addToDailyUsage(sessionTime: Long, app: BlockableApp?) = usageMutex.withLock {
        // Update total usage
        val currentTotal = (userSettingsStore.getTotalDailyUsage() as StateFlow<Long>).value
        val newTotal = currentTotal + sessionTime
        userSettingsStore.updateTotalDailyUsage(newTotal)

        // Update per-app usage if app is known
        when (app) {
            BlockableApp.REELS -> {
                val current = (userSettingsStore.getReelsDailyUsage() as StateFlow<Long>).value
                userSettingsStore.updateReelsDailyUsage(current + sessionTime)
                sessionSegmentStore.addSessionSegment(SessionSegment(app, sessionTime, LocalDateTime.now()))
            }
            BlockableApp.SHORTS -> {
                val current = (userSettingsStore.getShortsDailyUsage() as StateFlow<Long>).value
                userSettingsStore.updateShortsDailyUsage(current + sessionTime)
                sessionSegmentStore.addSessionSegment(SessionSegment(app, sessionTime, LocalDateTime.now()))
            }
            BlockableApp.TIKTOK -> {
                val current = (userSettingsStore.getTiktokDailyUsage() as StateFlow<Long>).value
                userSettingsStore.updateTiktokDailyUsage(current + sessionTime)
                sessionSegmentStore.addSessionSegment(SessionSegment(app, sessionTime, LocalDateTime.now()))
            }
            null -> Unit
        }
    }

    override suspend fun checkDailyReset() {
        val currentDay = LocalDate.now()
        val lastDay = (userSettingsStore.getLastResetDay() as StateFlow<LocalDate>).value

        if (currentDay != lastDay) {
            // Reset all usage (total + per-app) and update last reset day
            userSettingsStore.resetAllDailyUsage()
            userSettingsStore.setLastResetDay(currentDay)
        }
    }
}
