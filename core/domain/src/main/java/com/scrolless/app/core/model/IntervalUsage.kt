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
     * If [nowMillis] is still inside the saved interval, its usage is preserved. If the saved
     * interval has ended, returns [NOT_STARTED] so the timer remains idle until the user
     * watches another video.
     *
     * @return This same [IntervalUsage] instance when the clock moved backwards or the saved
     * interval is still active. Returns [NOT_STARTED] when the timer has not started, the interval
     * length is invalid, or the saved interval has elapsed.
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

        return NOT_STARTED
    }

    /** Milliseconds until the interval active at [nowMillis] ends, or 0 if not running. */
    fun remainingMillisAt(nowMillis: Long, lengthMillis: Long): Long {
        if (!isStarted || lengthMillis <= 0L) return 0L

        val current = activeIntervalAt(nowMillis, lengthMillis)
        if (!current.isStarted) return 0L

        return (current.startMillis + lengthMillis - nowMillis).coerceIn(0L, lengthMillis)
    }

    /**
     * Adds the part of a session watched during the interval active at [sessionEndMillis].
     *
     * If the interval timer has not started or the previous window has already ended, this
     * session starts a fresh interval anchored at [sessionStartMillis].
     *
     * If a session began in an earlier active interval and crossed into the next, only time
     * watched after the window boundary is counted in the new window.
     */
    fun plusSession(sessionStartMillis: Long, sessionEndMillis: Long, lengthMillis: Long): IntervalUsage {
        if (lengthMillis <= 0L) return this

        // If not started, or if clock moved backwards, or if the previous window ended before this session began:
        // this session anchors a fresh interval starting at sessionStartMillis.
        val schedule = if (!isStarted || sessionStartMillis >= startMillis + lengthMillis || sessionEndMillis < startMillis) {
            val baseUsage = if (isStarted && sessionEndMillis < startMillis) usageMillis else 0L
            copy(startMillis = sessionStartMillis, usageMillis = baseUsage)
        } else {
            this
        }

        val elapsedMillis = sessionEndMillis - schedule.startMillis
        if (elapsedMillis >= lengthMillis) {
            val intervalsPassed = elapsedMillis / lengthMillis
            val newStart = schedule.startMillis + intervalsPassed * lengthMillis
            val watchedInNewWindow = sessionEndMillis - maxOf(sessionStartMillis, newStart)
            return IntervalUsage(startMillis = newStart, usageMillis = watchedInNewWindow.coerceAtLeast(0L))
        }

        val watched = sessionEndMillis - maxOf(sessionStartMillis, schedule.startMillis)
        return schedule.copy(usageMillis = schedule.usageMillis + watched.coerceAtLeast(0L))
    }

    companion object {

        val NOT_STARTED = IntervalUsage(startMillis = 0L, usageMillis = 0L)
    }
}
