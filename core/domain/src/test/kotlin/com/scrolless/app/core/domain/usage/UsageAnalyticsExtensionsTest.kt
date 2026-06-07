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
package com.scrolless.app.core.domain.usage

import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.model.usage.calculateDailyTotals
import com.scrolless.app.core.model.usage.calculateWeekdayAverages
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UsageAnalyticsExtensionsTest {

    @Test
    fun `daily totals sum all tracked apps for selected day`() {
        val date = LocalDate.of(2026, 5, 26)
        val segments = listOf(
            segment(BlockableApp.REELS, date, 8, TimeUnit.MINUTES.toMillis(10)),
            segment(BlockableApp.SHORTS, date, 9, TimeUnit.MINUTES.toMillis(5)),
            segment(BlockableApp.TIKTOK, date.plusDays(1), 10, TimeUnit.MINUTES.toMillis(30)),
        )

        val totals = segments.calculateDailyTotals(
            startDate = date,
            endDateInclusive = date,
        )

        assertEquals(1, totals.size)
        assertEquals(TimeUnit.MINUTES.toMillis(15), totals.first().totalMillis)
    }

    @Test
    fun `daily totals include empty days as zero`() {
        val startDate = LocalDate.of(2026, 5, 25)
        val endDate = LocalDate.of(2026, 5, 27)
        val segments = listOf(
            segment(BlockableApp.REELS, startDate, 8, TimeUnit.MINUTES.toMillis(10)),
            segment(BlockableApp.SHORTS, endDate, 9, TimeUnit.MINUTES.toMillis(20)),
        )

        val totals = segments.calculateDailyTotals(
            startDate = startDate,
            endDateInclusive = endDate,
        )

        assertEquals(listOf(startDate, startDate.plusDays(1), endDate), totals.map { it.date })
        assertEquals(0L, totals[1].totalMillis)
    }

    @Test
    fun `weekday averages include only supplied window`() {
        val monday = LocalDate.of(2026, 5, 25)
        val sunday = monday.plusDays(6)
        val oldMonday = monday.minusWeeks(9)
        val segments = listOf(
            segment(BlockableApp.REELS, oldMonday, 8, TimeUnit.HOURS.toMillis(5)),
            segment(BlockableApp.REELS, monday, 8, TimeUnit.MINUTES.toMillis(30)),
            segment(BlockableApp.SHORTS, monday, 12, TimeUnit.MINUTES.toMillis(30)),
            segment(BlockableApp.TIKTOK, monday.plusDays(1), 9, TimeUnit.MINUTES.toMillis(14)),
        )

        val dailyTotals = segments.calculateDailyTotals(
            startDate = monday,
            endDateInclusive = sunday,
        )
        val averages = dailyTotals.calculateWeekdayAverages(
            startDate = monday,
            endDateInclusive = sunday,
        )

        assertEquals(TimeUnit.MINUTES.toMillis(60), averages.first { it.dayOfWeek == DayOfWeek.MONDAY }.averageMillis)
        assertEquals(TimeUnit.MINUTES.toMillis(14), averages.first { it.dayOfWeek == DayOfWeek.TUESDAY }.averageMillis)
        assertEquals(0L, averages.first { it.dayOfWeek == DayOfWeek.WEDNESDAY }.averageMillis)
    }

    private fun segment(app: BlockableApp, date: LocalDate, hour: Int, durationMillis: Long): SessionSegment = SessionSegment(
        app = app,
        durationMillis = durationMillis,
        startDateTime = date.atTime(hour, 0),
    )
}
