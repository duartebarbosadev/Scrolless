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
import com.scrolless.app.core.model.IntervalTimerWindow
import kotlin.math.max
import timber.log.Timber

/**
 * The interval start and watched time saved between viewing sessions.
 */
data class IntervalTimerState(val windowStartMillis: Long, val usageMillis: Long)

/**
 * Lets the user watch for a limited amount of time during each interval window.
 *
 * Content is blocked after the allowed watch time is used. When [intervalLengthMillis] has passed,
 * watched time returns to zero and the user receives the full allowance again.
 */
class IntervalTimerBlockHandler(
    allowanceMillis: Long,
    private val intervalLengthMillis: Long,
    initialState: IntervalTimerState,
    private val currentTimeProvider: () -> Long = System::currentTimeMillis,
    private val saveState: suspend (IntervalTimerState) -> Unit = {},
) : BlockOptionHandler {

    private val safeAllowanceMillis: Long = max(0L, allowanceMillis)

    internal var state: IntervalTimerState = initialState
        private set

    // Usage that existed when the current viewing session started. The elapsed session
    // time is added to this value during checks, so we do not need to save every second.
    private var sessionUsageBase: Long = initialState.usageMillis

    private fun currentWindow(): IntervalTimerWindow = IntervalTimerWindow(
        startMillis = state.windowStartMillis,
        lengthMillis = intervalLengthMillis,
        usageMillis = state.usageMillis,
    )

    /**
     * Advances to the window containing [now] and clears usage from older windows.
     *
     * Windows advance from their saved start instead of restarting whenever the app checks them.
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
     * Starts a window with no watched time.
     */
    private suspend fun resetWindow(newStartMillis: Long) {
        updateState(IntervalTimerState(windowStartMillis = newStartMillis, usageMillis = 0L))
    }

    /**
     * Waits for the save so the overlay cannot immediately load the previous value.
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
     * Counts only the part of the current session watched inside the active interval window.
     */
    override suspend fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        val now = currentTimeProvider()
        ensureWindowFresh(now)

        // elapsedTime covers the whole viewing session. If the session crossed a window
        // boundary, only the part inside the current window should count.
        val sessionUsage = currentWindow().sessionDurationInCurrentWindow(elapsedTime, now)
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
        val sessionUsage = currentWindow().sessionDurationInCurrentWindow(sessionTime, now)
        val updatedUsage = (sessionUsageBase + sessionUsage).coerceAtMost(safeAllowanceMillis)
        if (updatedUsage != state.usageMillis) {
            Timber.v("IntervalTimer.onExit: +%d -> usage=%d", sessionTime, updatedUsage)
            updateState(state.copy(usageMillis = updatedUsage))
        } else {
            sessionUsageBase = updatedUsage
        }
    }
}
