/*
 * Copyright (C) 2026 Scrolless
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
package com.scrolless.app.core.blocking.handler

import com.scrolless.app.core.model.BlockingResult
import kotlin.math.max
import timber.log.Timber

/**
 * The part of an interval timer that is saved between viewing sessions.
 *
 * @property windowStartMillis Epoch millis at which the current allowance window started.
 * @property usageMillis Milliseconds already consumed during the active allowance window.
 */
data class IntervalTimerState(val windowStartMillis: Long, val usageMillis: Long)

/**
 * Everything the overlay needs to keep counting correctly if an interval ends while it is visible.
 */
data class IntervalTimerSnapshot(val windowStartMillis: Long, val usageMillis: Long, val intervalLengthMillis: Long) {

    /**
     * Returns the usage for the window containing [nowMillis], including the active session.
     */
    fun usageIncludingSession(sessionStartAtMillis: Long, nowMillis: Long): Long {
        val safeUsage = usageMillis.coerceAtLeast(0L)
        val sessionDuration = (nowMillis - sessionStartAtMillis).coerceAtLeast(0L)
        if (intervalLengthMillis <= 0L) {
            return safeUsage + sessionDuration
        }

        val activeWindowStart = activeWindowStartAt(nowMillis)
        val usageBeforeSession = if (activeWindowStart == windowStartMillis) safeUsage else 0L
        val sessionUsage = (nowMillis - maxOf(sessionStartAtMillis, activeWindowStart)).coerceAtLeast(0L)
        return usageBeforeSession + sessionUsage
    }

    /**
     * Returns how much of a session belongs to the window containing [nowMillis].
     */
    fun sessionDurationInCurrentWindow(sessionDurationMillis: Long, nowMillis: Long): Long {
        val safeSessionDuration = sessionDurationMillis.coerceAtLeast(0L)
        if (intervalLengthMillis <= 0L) {
            return safeSessionDuration
        }

        val durationSinceWindowStart = (nowMillis - activeWindowStartAt(nowMillis)).coerceAtLeast(0L)
        return minOf(safeSessionDuration, durationSinceWindowStart)
    }

    private fun activeWindowStartAt(nowMillis: Long): Long {
        if (nowMillis <= windowStartMillis) {
            return windowStartMillis
        }

        val intervalsPassed = (nowMillis - windowStartMillis) / intervalLengthMillis
        return windowStartMillis + intervalsPassed * intervalLengthMillis
    }
}

/**
 * Blocks content when the watch allowance for the current interval window is exhausted.
 *
 * The handler keeps track of both the allowance usage and the start timestamp of the
 * allowance window. Once [intervalLengthMillis] has elapsed from the window start,
 * usage resets and the user receives a fresh allowance.
 */
class IntervalTimerBlockHandler(
    allowanceMillis: Long,
    private val intervalLengthMillis: Long,
    initialState: IntervalTimerState,
    private val currentTimeProvider: () -> Long = System::currentTimeMillis,
    private val saveState: suspend (IntervalTimerState) -> Unit = {},
) : BlockOptionHandler {

    private val safeAllowanceMillis: Long = max(0L, allowanceMillis)

    // Usage already saved for the active interval window.
    internal var state: IntervalTimerState = initialState
        private set

    // Usage that existed when the current viewing session started. The elapsed session
    // time is added to this value during checks, so we do not need to save every second.
    private var sessionUsageBase: Long = initialState.usageMillis

    // Use the same interval-boundary calculation for blocking and for the timer overlay.
    private fun snapshot(): IntervalTimerSnapshot = IntervalTimerSnapshot(
        windowStartMillis = state.windowStartMillis,
        usageMillis = state.usageMillis,
        intervalLengthMillis = intervalLengthMillis,
    )

    /**
     * Move to the window containing [now], clearing usage if the previous window ended.
     */
    private suspend fun ensureWindowFresh(now: Long) {
        val currentStart = state.windowStartMillis

        // With no valid interval length there is no boundary to cross. We only repair a
        // missing start time or a start time made invalid by the device clock moving back.
        if (intervalLengthMillis <= 0L) {
            if (currentStart == 0L || now < currentStart) {
                resetWindow(now)
            }
            return
        }

        // Start a new window on first run or after the device clock moves behind our saved start.
        if (currentStart == 0L || now < currentStart) {
            resetWindow(now)
            return
        }

        // Keep the original interval schedule. For example, if several windows passed while
        // the app was closed, advance by whole windows instead of starting a new schedule now.
        val elapsed = now - currentStart
        if (elapsed >= intervalLengthMillis) {
            val intervalsPassed = elapsed / intervalLengthMillis
            val newStart = currentStart + intervalsPassed * intervalLengthMillis
            Timber.v(
                "IntervalTimer.reset: elapsed=%d, intervals=%d -> newStart=%d",
                elapsed,
                intervalsPassed,
                newStart,
            )
            resetWindow(newStart)
        }
    }

    /**
     * Start a fresh allowance window and forget usage from the previous one.
     */
    private suspend fun resetWindow(newStartMillis: Long) {
        updateState(IntervalTimerState(windowStartMillis = newStartMillis, usageMillis = 0L))
    }

    /**
     * Keep the in-memory value and the value in settings in sync.
     * The save is awaited so callers cannot immediately read an older value.
     */
    private suspend fun updateState(newState: IntervalTimerState) {
        if (newState == state) {
            sessionUsageBase = newState.usageMillis
            return
        }

        Timber.v(
            "IntervalTimer.stateChanged: start=%d -> %d, usage=%d -> %d",
            state.windowStartMillis,
            newState.windowStartMillis,
            state.usageMillis,
            newState.usageMillis,
        )
        state = newState
        sessionUsageBase = newState.usageMillis
        saveState(newState)
    }

    override suspend fun onEnterContent(currentDailyUsage: Long): Boolean {
        val now = currentTimeProvider()

        // The previous window may have ended while the user was outside blocked content.
        ensureWindowFresh(now)

        // Remember where this session starts so elapsed time is not counted twice.
        sessionUsageBase = state.usageMillis

        val shouldBlock = state.usageMillis >= safeAllowanceMillis
        Timber.d(
            "IntervalTimer.onEnter: usage=%d/%d start=%d -> block=%s",
            state.usageMillis,
            safeAllowanceMillis,
            state.windowStartMillis,
            shouldBlock,
        )
        return shouldBlock
    }

    /**
     * Checks if adding the session time would exceed the interval limit.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @param elapsedTime Time elapsed in current session in milliseconds.
     * @return [BlockingResult.BlockNow] if should block, [BlockingResult.Continue] otherwise.
     */
    override suspend fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        val now = currentTimeProvider()
        ensureWindowFresh(now)

        // elapsedTime covers the whole viewing session. If the session crossed a window
        // boundary, only the part inside the current window should count.
        val sessionUsage = snapshot().sessionDurationInCurrentWindow(elapsedTime, now)
        val projectedUsage = sessionUsageBase + sessionUsage
        val result = if (projectedUsage >= safeAllowanceMillis) {
            val clamped = safeAllowanceMillis
            if (state.usageMillis != clamped) {
                updateState(state.copy(usageMillis = clamped))
            } else {
                sessionUsageBase = clamped
            }
            Timber.v(
                "IntervalTimer.onPeriodic: projected=%d/%d -> block",
                projectedUsage,
                safeAllowanceMillis,
            )
            BlockingResult.BlockNow
        } else {
            val remaining = safeAllowanceMillis - projectedUsage
            Timber.v(
                "IntervalTimer.onPeriodic: projected=%d/%d -> continue for %d ms",
                projectedUsage,
                safeAllowanceMillis,
                remaining,
            )
            BlockingResult.CheckLater(remaining)
        }
        return result
    }

    override suspend fun onExitContent(sessionTime: Long) {
        if (sessionTime <= 0L) {
            Timber.v("IntervalTimer.onExit: ignore non-positive session=%d", sessionTime)
            return
        }

        val now = currentTimeProvider()
        ensureWindowFresh(now)

        // Save only the part of the session that belongs to the active interval window.
        val sessionUsage = snapshot().sessionDurationInCurrentWindow(sessionTime, now)
        val updatedUsage = (sessionUsageBase + sessionUsage).coerceAtMost(safeAllowanceMillis)
        if (updatedUsage != state.usageMillis) {
            Timber.v("IntervalTimer.onExit: +%d -> usage=%d", sessionTime, updatedUsage)
            updateState(state.copy(usageMillis = updatedUsage))
        } else {
            sessionUsageBase = updatedUsage
        }
    }
}
