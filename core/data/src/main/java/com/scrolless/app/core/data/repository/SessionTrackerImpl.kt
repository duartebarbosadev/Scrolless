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

import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.repository.SessionSegmentStore
import com.scrolless.app.core.repository.SessionTracker
import java.time.Duration
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
class SessionTrackerImpl @Inject constructor(private val timeProvider: TimeProvider, private val sessionSegmentStore: SessionSegmentStore) :
    SessionTracker {

    companion object {
        private const val SESSION_MERGE_WINDOW_MILLIS = 30_000L
    }

    private val usageMutex = Mutex()
    private val sessionState = AtomicReference(SessionState(sessionStartLocalDate = timeProvider.localDateNow()))

    private var shouldStartNewSessionOnNextUsage = true

    override fun getDailyUsage(): Long {
        return (sessionSegmentStore.getTotalDurationForToday() as StateFlow<Long>).value
    }

    override suspend fun addToDailyUsage(sessionTime: Long, app: BlockableApp) {

        usageMutex.withLock {
            val currentSessionState = sessionState.get()
            val usageEnd = timeProvider.localDateTimeNow()
            val usageStart = usageEnd.minus(Duration.ofMillis(sessionTime))

            // If the start date and end date is different
            //  create a segment for the yesterday part and a segment for today's
            val dayChanged = usageStart.toLocalDate() != usageEnd.toLocalDate()
            if (dayChanged) {

                Timber.i(
                    "Day has changed since last session segment. Finishing yesterday session and creating a new one for today for app %s",
                    app,
                )

                val midnight = usageEnd.toLocalDate().atStartOfDay()
                val sessionTimeYesterday = Duration.between(usageStart, midnight).toMillis()
                val sessionTimeToday = Duration.between(midnight, usageEnd).toMillis()
                Timber.d(
                    "Session time of %s spans across two days. Allocating %s to yesterday and %s to today",
                    sessionTime,
                    sessionTimeYesterday,
                    sessionTimeToday,
                )

                // create session for yesterday
                createSegment(
                    shouldCreateNewSession = shouldStartNewSessionOnNextUsage,
                    app = app,
                    sessionTime = sessionTimeYesterday,
                    currentSessionState = currentSessionState,
                    sessionStartDateTime = usageStart,
                )
                // create session for today
                createSegment(
                    shouldCreateNewSession = true,
                    app = app,
                    sessionTime = sessionTimeToday,
                    currentSessionState = currentSessionState,
                    sessionStartDateTime = midnight,
                )
                return
            }

            createSegment(
                shouldCreateNewSession = shouldStartNewSessionOnNextUsage,
                app = app,
                sessionTime = sessionTime,
                currentSessionState = currentSessionState,
                sessionStartDateTime = usageStart,
            )
        }
    }

    private suspend fun createSegment(
        shouldCreateNewSession: Boolean,
        app: BlockableApp,
        sessionTime: Long,
        currentSessionState: SessionState,
        sessionStartDateTime: LocalDateTime,
    ): Any? = if (shouldCreateNewSession) {

        // Start a new session segment
        val newSegment = SessionSegment(app, sessionTime, sessionStartDateTime)
        Timber.i("Creating a session segment with session time of %s", sessionTime)
        val newSessionId = sessionSegmentStore.addSessionSegment(newSegment)
        sessionState.updateAndGet {
            it.copy(
                sessionId = newSessionId,
                currentSessionTotalTime = sessionTime,
                sessionStartLocalDate = sessionStartDateTime.toLocalDate(),
            )
        }
        shouldStartNewSessionOnNextUsage = false
    } else {
        // Update existing session segment
        val updatedSessionTotal = currentSessionState.currentSessionTotalTime + sessionTime
        Timber.i("Updating current session with session time of %s", updatedSessionTotal)
        sessionSegmentStore.updateSessionSegmentDuration(currentSessionState.sessionId, updatedSessionTotal)
        sessionState.updateAndGet {
            it.copy(
                segmentApp = app,
                currentSessionTotalTime = updatedSessionTotal,
            )
        }
    }

    override fun onAppOpen(app: BlockableApp) {
        val now = timeProvider.currentTimeInMillis()
        val state = sessionState.get()

        // Get if we should start a new session
        //  based on whether the app has changed
        //  or if the last app close was long enough ago
        //  or if the session started on a different day
        shouldStartNewSessionOnNextUsage = when {
            state.sessionId == -1L || state.segmentApp == null -> true
            state.segmentApp != app -> true
            state.lastAppCloseTimestamp <= 0L -> false
            state.sessionStartLocalDate != timeProvider.localDateNow() -> true
            else -> (now - state.lastAppCloseTimestamp) > SESSION_MERGE_WINDOW_MILLIS
        }

        // If we are starting a new session, create a session state with the new app and start time.
        //  Otherwise, we will continue the existing session segment.
        if (shouldStartNewSessionOnNextUsage) {
            Timber.d("Starting a new session segment for app %s", app)
            sessionState.updateAndGet {
                it.copy(
                    segmentApp = app,
                    sessionStartLocalDate = timeProvider.localDateNow(),
                )
            }
        } else {
            Timber.d("Continuing existing session segment on app %s", app)
        }
    }

    override fun onAppClose() {
        Timber.d("App closed, storing close timestamp for session merge decision")
        sessionState.updateAndGet { it.copy(lastAppCloseTimestamp = timeProvider.currentTimeInMillis()) }
    }

    /**
     * Represents the internal state for session tracking logic within [SessionTrackerImpl].
     *
     * This data class allows the tracker to make decisions about
     * whether to create a new session segment or append time to an existing one based on
     * user activity, such as switching apps or brief pauses in usage.
     *
     * @param segmentApp The last app that was tracked. Used to detect app switches.
     * @param shouldStartNewSessionOnNextUsage A flag to indicate if a new session should be started.
     * @param sessionId The ID of the last session segment, for updating existing segments.
     * @param lastAppCloseTimestamp The timestamp when the last app was closed, for session merging.
     * @param currentSessionTotalTime The total time of the current session, accumulated across usage reports.
     */
    private data class SessionState(
        val segmentApp: BlockableApp? = null,
        val sessionId: Long = -1L,
        val sessionStartLocalDate: LocalDate,
        val lastAppCloseTimestamp: Long = -1L,
        val currentSessionTotalTime: Long = 0L,
    )
}
