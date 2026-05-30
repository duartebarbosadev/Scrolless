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
package com.scrolless.app.core.data.repository

import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.model.usage.DailyUsageTotal
import com.scrolless.app.core.model.usage.WeekdayUsageAverage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal object SessionUsageAnalytics {
    fun dailyTotals(sessionSegments: List<SessionSegment>, startDate: LocalDate, endDateInclusive: LocalDate): List<DailyUsageTotal> {
        if (endDateInclusive.isBefore(startDate)) return emptyList()

        val totalsByDate = sessionSegments
            .groupBy { it.startDateTime.toLocalDate() }
            .mapValues { (_, segments) -> segments.sumOf { it.durationMillis.coerceAtLeast(0L) } }

        val dayCount = ChronoUnit.DAYS.between(startDate, endDateInclusive).toInt()
        return (0..dayCount).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            DailyUsageTotal(date = date, totalMillis = totalsByDate[date] ?: 0L)
        }
    }

    fun weekdayAverages(
        sessionSegments: List<SessionSegment>,
        startDate: LocalDate,
        endDateInclusive: LocalDate,
    ): List<WeekdayUsageAverage> {
        val dailyTotals = dailyTotals(
            sessionSegments = sessionSegments,
            startDate = startDate,
            endDateInclusive = endDateInclusive,
        )
        val totalsByWeekday = dailyTotals.groupBy { it.date.dayOfWeek }

        return DayOfWeek.entries.map { dayOfWeek ->
            val totals = totalsByWeekday[dayOfWeek].orEmpty()
            val averageMillis = if (totals.isEmpty()) {
                0L
            } else {
                totals.sumOf { it.totalMillis } / totals.size
            }
            WeekdayUsageAverage(dayOfWeek = dayOfWeek, averageMillis = averageMillis)
        }
    }
}
