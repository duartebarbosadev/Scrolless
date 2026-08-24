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
package com.scrolless.app.core.model

import kotlin.math.max

sealed interface BlockOption {

    data object BlockAll : BlockOption

    data class DailyLimit(val limitMillis: Long) : BlockOption

    data class IntervalTimer(val allowanceMillis: Long, val window: IntervalTimerWindow) : BlockOption

    data object NothingSelected : BlockOption
}

/**
 * The interval window currently being tracked.
 *
 * [startMillis] anchors the repeating schedule. It stays unchanged when the user expands the
 * window, so watched time and the original schedule are preserved.
 */
data class IntervalTimerWindow(val startMillis: Long, val lengthMillis: Long, val usageMillis: Long) {

    /**
     * Returns watched time in the current window, including the session still in progress.
     *
     * If the window ended during the session, time watched before it ended does not count toward
     * the new window.
     */
    fun usageIncludingSession(sessionStartAtMillis: Long, nowMillis: Long): Long {
        val windowStart = currentWindowStartAt(nowMillis)
        val savedUsage = if (windowStart == startMillis) usageMillis else 0L
        val sessionStart = maxOf(sessionStartAtMillis, windowStart)

        return savedUsage + (nowMillis - sessionStart)
    }

    /**
     * Returns the part of a completed session that belongs to the current interval window.
     */
    fun sessionDurationInCurrentWindow(sessionDurationMillis: Long, nowMillis: Long): Long {
        val safeSessionDuration = sessionDurationMillis.coerceAtLeast(0L)
        if (lengthMillis <= 0L) {
            return safeSessionDuration
        }

        val durationSinceWindowStart = (nowMillis - currentWindowStartAt(nowMillis)).coerceAtLeast(0L)
        return minOf(safeSessionDuration, durationSinceWindowStart)
    }

    /**
     * Returns the start of the repeating interval window that contains [nowMillis].
     *
     * For example, if the saved schedule starts at 12:00 and repeats every 60 minutes,
     * 13:03 belongs to the window that started at 13:00. Finding that boundary lets callers count
     * only usage from the current window, even when the saved window ended during a session.
     */
    private fun currentWindowStartAt(nowMillis: Long): Long {
        val elapsed = (nowMillis - startMillis).coerceAtLeast(0L)
        return startMillis + (elapsed / lengthMillis) * lengthMillis
    }
}

/**
 * Applies new settings without resetting the active window.
 *
 * Changing a running timer from a 30-minute window to a 60-minute window therefore expands the
 * current window: its original start and watched time remain unchanged.
 */
fun BlockOption.IntervalTimer.withConfig(allowanceMillis: Long, intervalLengthMillis: Long): BlockOption.IntervalTimer = copy(
    allowanceMillis = max(0L, allowanceMillis),
    window = window.copy(lengthMillis = intervalLengthMillis),
)
