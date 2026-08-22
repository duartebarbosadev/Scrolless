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
package com.scrolless.app.ui.overlay

import com.scrolless.app.core.blocking.handler.IntervalTimerSnapshot
import java.time.Instant
import java.time.ZoneId

/** State captured when a timer overlay session begins. */
sealed interface TimerOverlayInitialState {

    data class Daily(val usageMillis: Long) : TimerOverlayInitialState

    data class Interval(val snapshot: IntervalTimerSnapshot) : TimerOverlayInitialState
}

/**
 * Returns the time shown in the overlay.
 *
 * Daily timers reset at midnight. Interval timers use their configured fixed window boundary.
 * [zoneId] is used to find local midnight and can be replaced in tests.
 */
internal fun calculateDisplayedTimerDuration(
    initialState: TimerOverlayInitialState,
    sessionStartAtMillis: Long,
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long = when (initialState) {
    is TimerOverlayInitialState.Interval ->
        initialState.snapshot.usageIncludingSession(
            sessionStartAtMillis = sessionStartAtMillis,
            nowMillis = nowMillis,
        )

    is TimerOverlayInitialState.Daily -> {
        val persistedUsageBeforeSession = initialState.usageMillis.coerceAtLeast(0L)
        val currentDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val sessionStartDate = Instant.ofEpochMilli(sessionStartAtMillis).atZone(zoneId).toLocalDate()
        val persistedUsageToday = if (sessionStartDate == currentDate) persistedUsageBeforeSession else 0L
        val sessionUsageToday = sessionDurationInCurrentLocalDay(
            sessionStartAtMillis = sessionStartAtMillis,
            sessionEndAtMillis = nowMillis,
            zoneId = zoneId,
        )

        persistedUsageToday + sessionUsageToday
    }
}

/** Returns how much of the session happened after the most recent local midnight. */
internal fun sessionDurationInCurrentLocalDay(
    sessionStartAtMillis: Long,
    sessionEndAtMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long {
    val currentDate = Instant.ofEpochMilli(sessionEndAtMillis).atZone(zoneId).toLocalDate()
    val currentDayStartMillis = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val currentDaySessionStartMillis = maxOf(sessionStartAtMillis, currentDayStartMillis)
    return (sessionEndAtMillis - currentDaySessionStartMillis).coerceAtLeast(0L)
}
