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
import com.scrolless.app.core.repository.SessionTracker
import com.scrolless.app.core.repository.UserSettingsStore
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@Singleton
class SessionTrackerImpl @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
    private val sessionSegmentStore: SessionSegmentStore,
) : SessionTracker {

    private val usageMutex = Mutex()

    private var lastSegmentApp : BlockableApp? = null

    private var hasAppBeenClosed = true
    private var lastSessionId = 0L
    private var lastSessionCreationTimestamp : Long = -1
    private var currentSessionTotalTime = 0L

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

    override suspend fun addToDailyUsage(sessionTime: Long, app: BlockableApp) {
        usageMutex.withLock {

            // Update total usage
            val currentTotal = (userSettingsStore.getTotalDailyUsage() as StateFlow<Long>).value
            val newTotal = currentTotal + sessionTime
            userSettingsStore.updateTotalDailyUsage(newTotal)

            val timeDiffNowAndLastSession = (System.currentTimeMillis() - lastSessionCreationTimestamp)
            // Create a new session if there's no last session or a last app saved, or app was closed and wasn't reopen in more than 30 seconds
            val shouldCreateNewSession = if (lastSegmentApp == null || lastSessionId == 0L || lastSegmentApp != app) {
                true
            }
            else if (hasAppBeenClosed && timeDiffNowAndLastSession > 30_000) {
                true
            } else {
                false
            }

            if (shouldCreateNewSession) {
                // Start a new session segment
                lastSessionCreationTimestamp = System.currentTimeMillis()
                // Reset current session total time for the new session
                currentSessionTotalTime = sessionTime
                val newSegment = SessionSegment(app, sessionTime, LocalDateTime.now())
                Timber.i("Creation a session segment with session time of %s", sessionTime)
                lastSessionId = sessionSegmentStore.addSessionSegment(newSegment)
                hasAppBeenClosed = false
            } else {
                // Update existing session segment
                currentSessionTotalTime += sessionTime
                Timber.i("Updating current session with session time of %s", currentSessionTotalTime)
                sessionSegmentStore.updateSessionSegmentDuration(lastSessionId, currentSessionTotalTime)
            }

            lastSegmentApp = app

            // Todo remove in future: brainrot usage in userSettings
            // Update per-app usage if app is known
            when (app) {
                BlockableApp.REELS -> {
                    val current = (userSettingsStore.getReelsDailyUsage() as StateFlow<Long>).value
                    userSettingsStore.updateReelsDailyUsage(current + sessionTime)
                }

                BlockableApp.SHORTS -> {
                    val current = (userSettingsStore.getShortsDailyUsage() as StateFlow<Long>).value
                    userSettingsStore.updateShortsDailyUsage(current + sessionTime)
                }

                BlockableApp.TIKTOK -> {
                    val current = (userSettingsStore.getTiktokDailyUsage() as StateFlow<Long>).value
                    userSettingsStore.updateTiktokDailyUsage(current + sessionTime)
                }
            }
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

    override fun onAppOpen(app: BlockableApp) {
    }

    override fun onAppClose() {
        // TODO make a buffer to make sure that if the app closes or opens quickly we don't start a new session
        // TODO Make it so that if the screen is off we start a new session (after a buffer time)
        // Marking to start a new session on next usage
        Timber.d("App closed, marking to start new session")
        hasAppBeenClosed = true
    }

}
