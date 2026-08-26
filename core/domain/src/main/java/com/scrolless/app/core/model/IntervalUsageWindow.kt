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
 * A saved window can be older than the repeat that contains the current time. Every read goes
 * through [currentAt] so that callers only ever see the usage of the window they asked about.
 */
@Immutable
data class IntervalUsageWindow(val startMillis: Long, val lengthMillis: Long, val usageMillis: Long) {

    /** `false` while no window is running, so there is nothing to count down. */
    val isStarted: Boolean
        get() = startMillis > 0L && lengthMillis > 0L

    /**
     * Returns the repeat of this window that contains [nowMillis], with its own usage.
     *
     * Usage carries over only while the saved window is still running. The repeating schedule is
     * kept even when several windows passed while the app was closed.
     */
    fun currentAt(nowMillis: Long): IntervalUsageWindow {
        if (!isStarted || nowMillis < startMillis) {
            return copy(startMillis = nowMillis, usageMillis = 0L)
        }

        val elapsed = nowMillis - startMillis
        if (elapsed < lengthMillis) return this

        // Jump straight to the window holding nowMillis instead of stepping through the ones that
        // passed while the app was closed, so the restarts stay on their original schedule.
        val windowsPassed = elapsed / lengthMillis

        return copy(startMillis = startMillis + windowsPassed * lengthMillis, usageMillis = 0L)
    }

    /** Watched time in the window that contains [nowMillis]. */
    fun usageMillisAt(nowMillis: Long): Long = currentAt(nowMillis).usageMillis

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
        val started = if (startMillis == 0L) copy(startMillis = sessionStartMillis) else this
        val current = started.currentAt(sessionEndMillis)
        val countedFrom = maxOf(sessionStartMillis, current.startMillis)

        return current.copy(usageMillis = current.usageMillis + (sessionEndMillis - countedFrom).coerceAtLeast(0L))
    }

    companion object {

        /** No window running, and nothing watched yet. */
        val EMPTY = IntervalUsageWindow(startMillis = 0L, lengthMillis = 0L, usageMillis = 0L)
    }
}
