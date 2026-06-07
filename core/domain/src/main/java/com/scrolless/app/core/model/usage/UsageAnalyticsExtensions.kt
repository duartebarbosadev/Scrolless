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
package com.scrolless.app.core.model.usage

import com.scrolless.app.core.model.SessionSegment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Calculates daily usage totals for each date in the specified range.
 */
fun List<SessionSegment>.calculateDailyTotals(startDate: LocalDate, endDateInclusive: LocalDate): List<DailyUsageTotal> {
    if (endDateInclusive.isBefore(startDate)) return emptyList()

    val totalsByDate = groupBy { it.startDateTime.toLocalDate() }
        .mapValues { (_, segments) -> segments.sumOf { it.durationMillis.coerceAtLeast(0L) } }

    val dayCount = ChronoUnit.DAYS.between(startDate, endDateInclusive).toInt()
    return (0..dayCount).map { offset ->
        val date = startDate.plusDays(offset.toLong())
        DailyUsageTotal(date = date, totalMillis = totalsByDate[date] ?: 0L)
    }
}

/**
 * Calculates weekday usage averages from a list of daily totals.
 */
fun List<DailyUsageTotal>.calculateWeekdayAverages(startDate: LocalDate, endDateInclusive: LocalDate): List<WeekdayUsageAverage> {
    if (endDateInclusive.isBefore(startDate)) {
        return DayOfWeek.entries.map { WeekdayUsageAverage(it, 0L) }
    }

    val totalsByDate = filter { it.date in startDate..endDateInclusive }
        .associate { it.date to it.totalMillis }

    val dayCount = ChronoUnit.DAYS.between(startDate, endDateInclusive).toInt()
    val dates = (0..dayCount).map { offset -> startDate.plusDays(offset.toLong()) }
    val totalsByWeekday = dates
        .groupBy { it.dayOfWeek }
        .mapValues { (_, dates) ->
            dates.map { date -> totalsByDate[date] ?: 0L }
        }

    return DayOfWeek.entries.map { dayOfWeek ->
        val totals = totalsByWeekday[dayOfWeek].orEmpty()
        val averageMillis = if (totals.isEmpty()) {
            0L
        } else {
            totals.sum() / totals.size
        }
        WeekdayUsageAverage(dayOfWeek = dayOfWeek, averageMillis = averageMillis)
    }
}
