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
 * How much the user already watched in the interval timer window that started at [startMillis].
 *
 * Windows repeat every [lengthMillis], and each repeat starts over from zero. [startMillis]
 * anchors that schedule and stays unchanged while the window is running, so editing the interval
 * settings widens the running window instead of throwing away watched time. `0` means no window
 * started yet.
 *
 * [lengthMillis] mirrors [BlockingSettings.intervalLengthMillis]; only the persistence mapper
 * builds this from the saved settings, so the two cannot drift apart.
 *
 * The saved window is only written when a session ends, so it can be left behind by the clock.
 * [currentAt] and [remainingMillisAt] answer for the window the current time falls in, which is
 * why a window can restart without anything being saved.
 */
@Immutable
data class IntervalUsageWindow(val startMillis: Long, val lengthMillis: Long, val usageMillis: Long) {

    /** `false` while no window is running, so there is nothing to count down. */
    val isStarted: Boolean
        get() = startMillis > 0L && lengthMillis > 0L

    /**
     * Returns the repeat of this window that contains [nowMillis], with its own usage.
     *
     * The result depends only on [nowMillis], never on when or how often this is called. So the
     * Home screen and the accessibility service compute the same window from the same saved row,
     * without either one having to write the restart down for the other to see it.
     */
    fun currentAt(nowMillis: Long): IntervalUsageWindow {
        if (!isStarted) return this

        // Negative when the clock moved backwards, which must not hand out a fresh allowance.
        val windowsPassed = ((nowMillis - startMillis) / lengthMillis).coerceAtLeast(0L)
        if (windowsPassed == 0L) return this

        return copy(startMillis = startMillis + windowsPassed * lengthMillis, usageMillis = 0L)
    }

    /** Milliseconds left before the window that contains [nowMillis] ends. */
    fun remainingMillisAt(nowMillis: Long): Long {
        if (!isStarted) return 0L

        val current = currentAt(nowMillis)

        return (current.startMillis + lengthMillis - nowMillis).coerceIn(0L, lengthMillis)
    }

    /**
     * Adds the part of a session that belongs to the window containing [sessionEndMillis].
     *
     * Time watched before that window started is left behind, so a session crossing a boundary
     * only counts from the boundary on. Passing the current time as [sessionEndMillis] gives the
     * usage including a session still in progress, without having to save it.
     */
    fun plusSession(sessionStartMillis: Long, sessionEndMillis: Long): IntervalUsageWindow {
        // Nothing started the schedule yet, or the clock moved back behind it. Restarting it at the
        // session keeps usage, so moving the clock back cannot hand out a fresh allowance.
        val schedule = if (!isStarted || sessionEndMillis < startMillis) copy(startMillis = sessionStartMillis) else this

        val current = schedule.currentAt(sessionEndMillis)
        val watched = sessionEndMillis - maxOf(sessionStartMillis, current.startMillis)

        return current.copy(usageMillis = current.usageMillis + watched.coerceAtLeast(0L))
    }

    companion object {

        /** No window running, and nothing watched yet. */
        val EMPTY = IntervalUsageWindow(startMillis = 0L, lengthMillis = 0L, usageMillis = 0L)
    }
}
