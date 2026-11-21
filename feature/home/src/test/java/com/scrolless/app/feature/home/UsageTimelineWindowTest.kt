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
package com.scrolless.app.feature.home

import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.feature.home.components.TimelineWindow
import com.scrolless.app.feature.home.components.timelineWindowFor
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageTimelineWindowTest {

    @Test
    fun `timeline shows the whole day when there are no sessions`() {
        assertEquals(TimelineWindow(startHour = 0, endHour = 24), timelineWindowFor(emptyList()))
    }

    @Test
    fun `timeline zooms to the active hours in four whole-hour steps`() {
        val window = timelineWindowFor(
            listOf(
                segment(hour = 21, minute = 10, minutes = 10),
                segment(hour = 22, minute = 30, minutes = 15),
            ),
        )

        assertEquals(TimelineWindow(startHour = 20, endHour = 24), window)
        assertEquals(listOf(20, 21, 22, 23, 24), window.tickHours)
    }

    @Test
    fun `timeline rounds the active span up and keeps it inside the day`() {
        val window = timelineWindowFor(
            listOf(
                segment(hour = 1, minute = 0, minutes = 30),
                segment(hour = 6, minute = 0, minutes = 20),
            ),
        )

        assertEquals(TimelineWindow(startHour = 0, endHour = 8), window)
        assertEquals(listOf(0, 2, 4, 6, 8), window.tickHours)
    }

    @Test
    fun `timeline keeps a short session that starts on the hour inside the window`() {
        val window = timelineWindowFor(
            listOf(
                segment(hour = 16, minute = 10, minutes = 10),
                segment(hour = 20, minute = 0, minutes = 0, seconds = 40),
            ),
        )

        assertEquals(TimelineWindow(startHour = 15, endHour = 23), window)
        assertTrue(window.fractionOf(20 * 60f) < 1f)
    }

    private fun segment(hour: Int, minute: Int, minutes: Long, seconds: Long = 0) = SessionSegment(
        app = BlockableApp.TIKTOK,
        durationMillis = TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds),
        startDateTime = LocalDateTime.of(2026, 10, 15, hour, minute),
    )
}
