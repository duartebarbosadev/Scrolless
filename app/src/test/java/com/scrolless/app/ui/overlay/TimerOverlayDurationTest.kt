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

import com.scrolless.app.core.model.IntervalTimerWindow
import java.time.LocalDateTime
import java.time.ZoneId
import junit.framework.TestCase.assertEquals
import org.junit.Test

class TimerOverlayDurationTest {

    private val zoneId = ZoneId.of("Australia/Brisbane")

    @Test
    fun `daily timer includes baseline when session started today`() {
        val sessionStart = epochMillis(2026, 8, 13, 10, 0)
        val now = epochMillis(2026, 8, 13, 10, 5)

        val duration = calculateDisplayedTimerDuration(
            initialState = TimerOverlayInitialState.Daily(usageMillis = 20 * MINUTE_MILLIS),
            sessionStartAtMillis = sessionStart,
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(25 * MINUTE_MILLIS, duration)
    }

    @Test
    fun `daily timer resets baseline after midnight`() {
        val sessionStart = epochMillis(2026, 8, 13, 23, 55)
        val now = epochMillis(2026, 8, 14, 0, 5)

        val duration = calculateDisplayedTimerDuration(
            initialState = TimerOverlayInitialState.Daily(usageMillis = 20 * MINUTE_MILLIS),
            sessionStartAtMillis = sessionStart,
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(5 * MINUTE_MILLIS, duration)
    }

    @Test
    fun `interval timer keeps baseline across midnight`() {
        val windowStart = epochMillis(2026, 8, 13, 23, 30)
        val sessionStart = epochMillis(2026, 8, 13, 23, 55)
        val now = epochMillis(2026, 8, 14, 0, 5)

        val duration = calculateDisplayedTimerDuration(
            initialState = TimerOverlayInitialState.Interval(
                IntervalTimerWindow(
                    startMillis = windowStart,
                    lengthMillis = 60 * MINUTE_MILLIS,
                    usageMillis = 20 * MINUTE_MILLIS,
                ),
            ),
            sessionStartAtMillis = sessionStart,
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(30 * MINUTE_MILLIS, duration)
    }

    @Test
    fun `interval timer keeps counting when session exceeds blocking allowance`() {
        val windowStart = epochMillis(2026, 8, 13, 12, 0)
        val sessionStart = epochMillis(2026, 8, 13, 12, 10)
        val now = epochMillis(2026, 8, 13, 12, 40)

        val duration = calculateDisplayedTimerDuration(
            initialState = TimerOverlayInitialState.Interval(
                IntervalTimerWindow(
                    startMillis = windowStart,
                    lengthMillis = 60 * MINUTE_MILLIS,
                    usageMillis = 0L,
                ),
            ),
            sessionStartAtMillis = sessionStart,
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(30 * MINUTE_MILLIS, duration)
    }

    @Test
    fun `interval timer resets baseline and pre-boundary session usage at window boundary`() {
        val windowStart = epochMillis(2026, 8, 13, 12, 0)
        val sessionStart = epochMillis(2026, 8, 13, 12, 58)
        val now = epochMillis(2026, 8, 13, 13, 3)

        val duration = calculateDisplayedTimerDuration(
            initialState = TimerOverlayInitialState.Interval(
                IntervalTimerWindow(
                    startMillis = windowStart,
                    lengthMillis = 60 * MINUTE_MILLIS,
                    usageMillis = 20 * MINUTE_MILLIS,
                ),
            ),
            sessionStartAtMillis = sessionStart,
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(3 * MINUTE_MILLIS, duration)
    }

    @Test
    fun `current day session duration excludes time before midnight`() {
        val sessionStart = epochMillis(2026, 8, 13, 23, 55)
        val sessionEnd = epochMillis(2026, 8, 14, 0, 5)

        val duration = sessionDurationInCurrentLocalDay(
            sessionStartAtMillis = sessionStart,
            sessionEndAtMillis = sessionEnd,
            zoneId = zoneId,
        )

        assertEquals(5 * MINUTE_MILLIS, duration)
    }

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zoneId).toInstant().toEpochMilli()

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
