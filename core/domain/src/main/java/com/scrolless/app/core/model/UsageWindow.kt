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
import java.time.Instant
import java.time.ZoneId

/**
 * How much the user already watched in a window that restarts every [lengthMillis].
 *  > So for daily limit it's a window that restarts on the next midnight time
 *  > For interval timer its on the user set duration
 *
 * [startMillis] anchors the repeating schedule and stays unchanged while the window is running, so
 * editing the settings never throws away watched time. `0` means no window started yet.
 */
@Immutable
data class UsageWindow(val startMillis: Long, val lengthMillis: Long, val usageMillis: Long) {

    /** `false` while no window is running, so there is nothing to count down. */
    val isStarted: Boolean
        get() = startMillis > 0L && lengthMillis > 0L

    /**
     * Milliseconds left before this window ends, without rolling it over.
     *
     * Reaching `0` is how a caller knows [usageMillis] belongs to a window that already ended.
     */
    fun remainingMillisAt(nowMillis: Long): Long {
        if (!isStarted) return 0L

        val elapsed = nowMillis - startMillis
        if (elapsed < 0L) return lengthMillis

        return (lengthMillis - elapsed).coerceAtLeast(0L)
    }

    /**
     * Returns the repeat of this window that contains [nowMillis], with its own usage.
     *
     * Windows repeat every [lengthMillis] starting from [startMillis], so this restarts the count
     * at the end of an interval for the interval timer, and at midnight for a daily one. Usage
     * carries over only while the saved window is still running, and the repeating schedule is
     * kept even when several windows passed while the app was closed.
     */
    fun windowAt(nowMillis: Long): UsageWindow {
        val hasNotStarted = startMillis == 0L || lengthMillis <= 0L
        if (hasNotStarted || nowMillis < startMillis) {
            return copy(startMillis = nowMillis, usageMillis = 0L)
        }

        val elapsed = nowMillis - startMillis
        if (elapsed < lengthMillis) return this

        // Jump straight to the window holding nowMillis instead of stepping through the ones that
        // passed while the app was closed, so the restarts stay on their original schedule.
        val windowsPassed = elapsed / lengthMillis

        return copy(startMillis = startMillis + windowsPassed * lengthMillis, usageMillis = 0L)
    }

    /**
     * Watched time in the window that contains [nowMillis], counting the session in progress.
     *
     * Only the part of the session that falls inside that window counts, so time watched before
     * the previous window ended is left behind. Use [addingSession] instead once the session
     * finished and the result has to be saved.
     */
    fun usageAt(sessionStartMillis: Long, nowMillis: Long): Long {
        val current = windowAt(nowMillis)

        return current.usageMillis + current.sessionMillisInside(sessionStartMillis, nowMillis)
    }

    /**
     * Adds the part of a finished session that belongs to the current window.
     *
     * The counterpart of [usageAt]: same arithmetic, but the result is a window to save rather
     * than a value to display.
     */
    fun addingSession(sessionStartMillis: Long, sessionEndMillis: Long): UsageWindow {
        val started = if (startMillis == 0L) copy(startMillis = sessionStartMillis) else this
        val current = started.windowAt(sessionEndMillis)
        val sessionMillis = current.sessionMillisInside(sessionStartMillis, sessionEndMillis)

        return current.copy(usageMillis = current.usageMillis + sessionMillis)
    }

    /**
     * How much of the session from [sessionStartMillis] to [sessionEndMillis] happened after this
     * window started. A session that began in the previous window only counts from [startMillis].
     */
    private fun sessionMillisInside(sessionStartMillis: Long, sessionEndMillis: Long): Long {
        val countedFrom = maxOf(sessionStartMillis, startMillis)

        return (sessionEndMillis - countedFrom).coerceAtLeast(0L)
    }

    companion object {

        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

        /** No window running, and nothing watched yet. */
        val EMPTY = UsageWindow(startMillis = 0L, lengthMillis = 0L, usageMillis = 0L)

        /**
         * A window holding today's usage, restarting at the local midnight after [nowMillis].
         *
         * The length is a fixed 24 hours, so on the two days a year that daylight saving shifts
         * the clock the restart is off by an hour. That only shows up in a session that runs
         * across midnight on exactly those nights.
         */
        fun forLocalDay(nowMillis: Long, usageMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): UsageWindow {
            val startOfDay = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().atStartOfDay(zoneId)

            return UsageWindow(
                startMillis = startOfDay.toInstant().toEpochMilli(),
                lengthMillis = DAY_MILLIS,
                usageMillis = usageMillis,
            )
        }
    }
}
