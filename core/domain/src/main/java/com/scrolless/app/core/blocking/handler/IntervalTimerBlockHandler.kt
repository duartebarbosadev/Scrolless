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
package com.scrolless.app.core.blocking.handler

import com.scrolless.app.core.model.BlockingResult
import kotlin.math.max
import timber.log.Timber

/**
 * Stores the persisted state of the interval timer cycle.
 *
 * @property windowStartMillis Epoch millis at which the current allowance window started.
 * @property usageMillis Milliseconds already consumed during the active allowance window.
 */
data class IntervalTimerState(
    val windowStartMillis: Long,
    val usageMillis: Long,
)

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
    private val onStateChanged: (IntervalTimerState) -> Unit,
    private val currentTimeProvider: () -> Long = System::currentTimeMillis,
) : BlockOptionHandler {

    private val safeAllowanceMillis: Long = max(0L, allowanceMillis)

    private var state: IntervalTimerState = initialState
    private var sessionUsageBase: Long = initialState.usageMillis

    /**
     * Reset stale window timestamps so allowance resets stay in sync with the clock.
     */
    private fun ensureWindowFresh(now: Long) {
        val currentStart = state.windowStartMillis

        // No interval configured – treat allowance as a single bucket and clamp bad timestamps.
        if (intervalLengthMillis <= 0L) {
            if (currentStart == 0L || now < currentStart) {
                resetWindow(now)
            }
            return
        }

        // Missing or future start values can happen on first run or after clock changes.
        if (currentStart == 0L || now < currentStart) {
            resetWindow(now)
            return
        }

        // Advance the window in fixed-size steps if one or more full intervals already passed.
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
     * Start a fresh allowance window at [newStartMillis] and clear accumulated usage.
     */
    private fun resetWindow(newStartMillis: Long) {
        updateState(
            IntervalTimerState(windowStartMillis = newStartMillis, usageMillis = 0L),
            resetSessionBase = true,
        )
    }

    /**
     * Persist a new interval state and optionally update the session baseline.
     */
    private fun updateState(newState: IntervalTimerState, resetSessionBase: Boolean) {
        if (newState != state) {
            Timber.v(
                "IntervalTimer.stateChanged: start=%d -> %d, usage=%d -> %d",
                state.windowStartMillis,
                newState.windowStartMillis,
                state.usageMillis,
                newState.usageMillis,
            )
            state = newState
            onStateChanged(newState)
        }
        if (resetSessionBase) {
            sessionUsageBase = newState.usageMillis
        }
    }

    override fun onEnterContent(currentDailyUsage: Long): Boolean {
        val now = currentTimeProvider()
        ensureWindowFresh(now)
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
    override fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        val now = currentTimeProvider()
        ensureWindowFresh(now)

        val projectedUsage = sessionUsageBase + elapsedTime
        return if (projectedUsage >= safeAllowanceMillis) {
            val clamped = safeAllowanceMillis
            if (state.usageMillis != clamped) {
                updateState(state.copy(usageMillis = clamped), resetSessionBase = true)
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
    }

    override fun onExitContent(sessionTime: Long) {
        if (sessionTime <= 0L) {
            Timber.v("IntervalTimer.onExit: ignore non-positive session=%d", sessionTime)
            return
        }

        val now = currentTimeProvider()
        ensureWindowFresh(now)

        val updatedUsage = (sessionUsageBase + sessionTime).coerceAtMost(safeAllowanceMillis)
        if (updatedUsage != state.usageMillis) {
            Timber.v("IntervalTimer.onExit: +%d -> usage=%d", sessionTime, updatedUsage)
            updateState(state.copy(usageMillis = updatedUsage), resetSessionBase = true)
        } else {
            sessionUsageBase = updatedUsage
        }
    }
}
