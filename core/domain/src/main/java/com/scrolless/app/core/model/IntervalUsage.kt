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

import androidx.compose.runtime.Immutable

/**
 * Saved usage for an interval timer.
 *
 * For example, a timer starting at 10:00 with a 30-minute length has intervals from 10:00–10:30,
 * 10:30–11:00, and so on. [startMillis] is the start of the last interval saved after a session
 * ended, and [usageMillis] is the time watched during that interval. `0` means the timer has not
 * started.
 *
 * Time passing does not update the database by itself. If the saved interval has ended, the
 * functions below calculate the current interval in memory with zero usage. That new interval is
 * saved when the next viewing session ends.
 */
@Immutable
data class IntervalUsage(val startMillis: Long, val usageMillis: Long) {

    val isStarted: Boolean
        get() = startMillis > 0L

    /**
     * Returns the interval active at [nowMillis].
     *
     * If [nowMillis] is still inside the saved interval, its usage is preserved. If one or more
     * intervals have ended, the returned value starts at the most recent boundary with zero usage.
     *
     * @return This same [IntervalUsage] instance when the timer has not started, the interval length
     * is invalid, the clock moved backwards, or the saved interval is still active. Otherwise,
     * returns a new instance for the active interval with zero usage.
     */
    fun activeIntervalAt(nowMillis: Long, lengthMillis: Long): IntervalUsage {
        if (!isStarted || lengthMillis <= 0L) return this

        if (nowMillis < startMillis) {
            // The saved interval may start at 10:00 while the device clock now says 9:50. This can
            // happen if the user manually changes the date or time, or if automatic clock sync
            // corrects a device clock that was running ahead. Keep the saved interval and its usage;
            // treating this as a new interval would incorrectly give the user another allowance.
            return this
        }

        val elapsedMillis = nowMillis - startMillis
        if (elapsedMillis < lengthMillis) {
            // The saved interval is still active, so its start and usage are already correct.
            return this
        }

        val intervalsPassed = elapsedMillis / lengthMillis

        return IntervalUsage(startMillis = startMillis + intervalsPassed * lengthMillis, usageMillis = 0L)
    }

    /** Milliseconds until the interval active at [nowMillis] ends. */
    fun remainingMillisAt(nowMillis: Long, lengthMillis: Long): Long {
        if (!isStarted || lengthMillis <= 0L) return 0L

        val current = activeIntervalAt(nowMillis, lengthMillis)

        return (current.startMillis + lengthMillis - nowMillis).coerceIn(0L, lengthMillis)
    }

    /**
     * Adds the part of a session watched during the interval active at [sessionEndMillis]. If the
     * session began in an earlier interval, only time watched after the current interval started is
     * added.
     *
     * Passing the current time as [sessionEndMillis] gives the usage including a session still in
     * progress, without having to save it.
     */
    fun plusSession(sessionStartMillis: Long, sessionEndMillis: Long, lengthMillis: Long): IntervalUsage {
        // The first session starts the timer. If the device clock was changed to a time before the
        // saved interval, move its start to this session so watched time can still be counted, but
        // preserve the saved usage so changing the clock does not restore the allowance.
        val schedule = if (!isStarted || sessionEndMillis < startMillis) copy(startMillis = sessionStartMillis) else this

        val current = schedule.activeIntervalAt(sessionEndMillis, lengthMillis)
        val watched = sessionEndMillis - maxOf(sessionStartMillis, current.startMillis)

        return current.copy(usageMillis = current.usageMillis + watched.coerceAtLeast(0L))
    }

    companion object {

        val NOT_STARTED = IntervalUsage(startMillis = 0L, usageMillis = 0L)
    }
}
