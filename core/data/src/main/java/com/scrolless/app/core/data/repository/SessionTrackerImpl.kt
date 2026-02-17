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
import java.util.concurrent.atomic.AtomicReference
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

    companion object {
        private const val SESSION_MERGE_WINDOW_MILLIS = 30_000L
    }

    private val usageMutex = Mutex()
    private val sessionState = AtomicReference(SessionState())

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
            val currentSessionState = sessionState.get()

            // Update total usage
            val currentTotal = (userSettingsStore.getTotalDailyUsage() as StateFlow<Long>).value
            val newTotal = currentTotal + sessionTime
            userSettingsStore.updateTotalDailyUsage(newTotal)

            val shouldCreateNewSession = currentSessionState.shouldStartNewSessionOnNextUsage ||
                currentSessionState.lastSessionId == 0L ||
                currentSessionState.lastSegmentApp != app

            if (shouldCreateNewSession) {
                // Start a new session segment
                val newSegment = SessionSegment(app, sessionTime, LocalDateTime.now())
                Timber.i("Creation a session segment with session time of %s", sessionTime)
                val newSessionId = sessionSegmentStore.addSessionSegment(newSegment)
                sessionState.updateAndGet {
                    it.copy(
                        lastSegmentApp = app,
                        shouldStartNewSessionOnNextUsage = false,
                        lastSessionId = newSessionId,
                        currentSessionTotalTime = sessionTime,
                    )
                }
            } else {
                // Update existing session segment
                val updatedSessionTotal = currentSessionState.currentSessionTotalTime + sessionTime
                Timber.i("Updating current session with session time of %s", updatedSessionTotal)
                sessionSegmentStore.updateSessionSegmentDuration(currentSessionState.lastSessionId, updatedSessionTotal)
                sessionState.updateAndGet {
                    it.copy(
                        lastSegmentApp = app,
                        shouldStartNewSessionOnNextUsage = false,
                        currentSessionTotalTime = updatedSessionTotal,
                    )
                }
            }

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
        val now = System.currentTimeMillis()
        sessionState.updateAndGet { state ->
            val shouldStartNewSession = when {
                state.lastSessionId == 0L || state.lastSegmentApp == null -> true
                state.lastSegmentApp != app -> true
                state.lastAppCloseTimestamp <= 0L -> false
                else -> (now - state.lastAppCloseTimestamp) > SESSION_MERGE_WINDOW_MILLIS
            }
            state.copy(shouldStartNewSessionOnNextUsage = shouldStartNewSession)
        }
    }

    override fun onAppClose() {
        Timber.d("App closed, storing close timestamp for session merge decision")
        sessionState.updateAndGet { it.copy(lastAppCloseTimestamp = System.currentTimeMillis()) }
    }

    private data class SessionState(
        val lastSegmentApp: BlockableApp? = null,
        val shouldStartNewSessionOnNextUsage: Boolean = true,
        val lastSessionId: Long = 0L,
        val lastAppCloseTimestamp: Long = -1L,
        val currentSessionTotalTime: Long = 0L,
    )
}
