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
 * How much was watched in the interval window that started at [startMillis]. `0` means not started.
 *
 * Windows repeat every `lengthMillis`, each repeat starting over from zero. Only a session end
 * writes this, so the saved value outlives the window it describes; every function below derives
 * the window that `nowMillis` falls in rather than writing the rollover down.
 */
@Immutable
data class IntervalUsage(val startMillis: Long, val usageMillis: Long) {

    val isStarted: Boolean
        get() = startMillis > 0L

    /** Returns the repeat of this window that contains [nowMillis], with its own usage. */
    fun currentAt(nowMillis: Long, lengthMillis: Long): IntervalUsage {
        if (!isStarted || lengthMillis <= 0L) return this

        // Negative when the clock moved backwards, which must not hand out a fresh allowance.
        val windowsPassed = ((nowMillis - startMillis) / lengthMillis).coerceAtLeast(0L)
        if (windowsPassed == 0L) return this

        return IntervalUsage(startMillis = startMillis + windowsPassed * lengthMillis, usageMillis = 0L)
    }

    /** Milliseconds left before the window that contains [nowMillis] ends. */
    fun remainingMillisAt(nowMillis: Long, lengthMillis: Long): Long {
        if (!isStarted || lengthMillis <= 0L) return 0L

        val current = currentAt(nowMillis, lengthMillis)

        return (current.startMillis + lengthMillis - nowMillis).coerceIn(0L, lengthMillis)
    }

    /**
     * Adds the part of a session that belongs to the window containing [sessionEndMillis], so a
     * session crossing a boundary only counts from the boundary on.
     *
     * Passing the current time as [sessionEndMillis] gives the usage including a session still in
     * progress, without having to save it.
     */
    fun plusSession(sessionStartMillis: Long, sessionEndMillis: Long, lengthMillis: Long): IntervalUsage {
        // Nothing started the schedule yet, or the clock moved back behind it. Restarting it at the
        // session keeps usage, so moving the clock back cannot hand out a fresh allowance.
        val schedule = if (!isStarted || sessionEndMillis < startMillis) copy(startMillis = sessionStartMillis) else this

        val current = schedule.currentAt(sessionEndMillis, lengthMillis)
        val watched = sessionEndMillis - maxOf(sessionStartMillis, current.startMillis)

        return current.copy(usageMillis = current.usageMillis + watched.coerceAtLeast(0L))
    }

    companion object {

        val NOT_STARTED = IntervalUsage(startMillis = 0L, usageMillis = 0L)
    }
}
