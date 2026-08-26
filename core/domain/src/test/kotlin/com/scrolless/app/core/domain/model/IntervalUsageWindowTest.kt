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
package com.scrolless.app.core.domain.model

import com.scrolless.app.core.model.IntervalUsageWindow
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalUsageWindowTest {

    private val zoneId = ZoneId.of("Australia/Brisbane")

    @Test
    fun `a window that is still running is left untouched`() {
        val window = window(startMillis = 1_000L, usageMillis = 5_000L)

        assertEquals(window, window.currentAt(nowMillis = 5_000L))
    }

    @Test
    fun `a window that ended moves to the interval containing now and clears usage`() {
        val window = window(startMillis = 1_000L, lengthMillis = 10_000L, usageMillis = 5_000L)

        val current = window.currentAt(nowMillis = 35_000L)

        assertEquals(31_000L, current.startMillis)
        assertEquals(0L, current.usageMillis)
    }

    @Test
    fun `usage of a window that ended belongs to the previous one and is dropped`() {
        val window = window(startMillis = 1_000L, lengthMillis = 10_000L, usageMillis = 5_000L)

        assertEquals(5_000L, window.usageMillisAt(nowMillis = 9_000L))
        assertEquals(0L, window.usageMillisAt(nowMillis = 11_000L))
    }

    @Test
    fun `adding a session to a running window keeps its start and previous usage`() {
        val window = window(startMillis = 1_000L, lengthMillis = 60 * MINUTE_MILLIS, usageMillis = 10_000L)

        val updated = window.plusSession(sessionStartMillis = 11_000L, sessionEndMillis = 21_000L)

        assertEquals(1_000L, updated.startMillis)
        assertEquals(20_000L, updated.usageMillis)
    }

    @Test
    fun `adding the first session starts the window when the session started`() {
        val window = window(startMillis = 0L, usageMillis = 0L)

        val updated = window.plusSession(sessionStartMillis = 1_000L, sessionEndMillis = 11_000L)

        assertEquals(1_000L, updated.startMillis)
        assertEquals(10_000L, updated.usageMillis)
    }

    @Test
    fun `a session crossing a boundary only counts the time in the new window`() {
        val window = window(startMillis = 1_000L, lengthMillis = 10_000L, usageMillis = 4_000L)

        val updated = window.plusSession(sessionStartMillis = 9_000L, sessionEndMillis = 13_000L)

        assertEquals(11_000L, updated.startMillis)
        assertEquals(2_000L, updated.usageMillis)
    }

    @Test
    fun `a session still running adds to the saved usage without changing it`() {
        val window = window(startMillis = 1_000L, usageMillis = 4_000L)

        val usage = window.plusSession(sessionStartMillis = 5_000L, sessionEndMillis = 8_000L).usageMillis

        assertEquals(7_000L, usage)
        assertEquals(4_000L, window.usageMillis)
    }

    @Test
    fun `a window keeps counting across midnight`() {
        val window = IntervalUsageWindow(
            startMillis = epochMillis(2026, 8, 13, 23, 30),
            lengthMillis = 60 * MINUTE_MILLIS,
            usageMillis = 20 * MINUTE_MILLIS,
        )

        val usage = window.plusSession(
            sessionStartMillis = epochMillis(2026, 8, 13, 23, 55),
            sessionEndMillis = epochMillis(2026, 8, 14, 0, 5),
        ).usageMillis

        assertEquals(30 * MINUTE_MILLIS, usage)
    }

    @Test
    fun `a window that never started is not running and has nothing left`() {
        assertEquals(false, IntervalUsageWindow.EMPTY.isStarted)
        assertEquals(0L, IntervalUsageWindow.EMPTY.remainingMillisAt(nowMillis = 5_000L))
    }

    @Test
    fun `remaining time counts down to the end of the window`() {
        val window = window(startMillis = 1_000L, lengthMillis = 60 * MINUTE_MILLIS)

        assertEquals(60 * MINUTE_MILLIS, window.remainingMillisAt(nowMillis = 1_000L))
        assertEquals(45 * MINUTE_MILLIS, window.remainingMillisAt(nowMillis = 1_000L + 15 * MINUTE_MILLIS))
    }

    @Test
    fun `remaining time restarts with the next window instead of staying at zero`() {
        val window = window(startMillis = 1_000L, lengthMillis = 60 * MINUTE_MILLIS)

        assertEquals(60 * MINUTE_MILLIS, window.remainingMillisAt(nowMillis = 1_000L + 60 * MINUTE_MILLIS))
        assertEquals(50 * MINUTE_MILLIS, window.remainingMillisAt(nowMillis = 1_000L + 70 * MINUTE_MILLIS))
    }

    @Test
    fun `a clock moved backwards restarts the window instead of counting down forever`() {
        val window = window(startMillis = 10_000L, lengthMillis = 10_000L, usageMillis = 5_000L)

        assertEquals(0L, window.usageMillisAt(nowMillis = 4_000L))
        assertEquals(10_000L, window.remainingMillisAt(nowMillis = 4_000L))
    }

    private fun window(startMillis: Long = 1_000L, lengthMillis: Long = 30 * MINUTE_MILLIS, usageMillis: Long = 0L) =
        IntervalUsageWindow(startMillis = startMillis, lengthMillis = lengthMillis, usageMillis = usageMillis)

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zoneId).toInstant().toEpochMilli()

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
