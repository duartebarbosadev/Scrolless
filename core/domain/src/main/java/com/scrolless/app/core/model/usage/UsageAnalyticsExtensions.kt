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

import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Calculates daily usage totals for each date in the specified range.
 */
fun List<SessionSegment>.calculateDailyTotals(startDate: LocalDate, endDateInclusive: LocalDate): List<DailyUsageTotal> {
    if (endDateInclusive.isBefore(startDate)) return emptyList()

    val segmentsByDate = groupBy { it.startDateTime.toLocalDate() }

    val dayCount = ChronoUnit.DAYS.between(startDate, endDateInclusive).toInt()
    return (0..dayCount).map { offset ->
        val date = startDate.plusDays(offset.toLong())
        val segments = segmentsByDate[date].orEmpty()
        DailyUsageTotal(
            date = date,
            totalMillis = segments.sumOf { it.durationMillis.coerceAtLeast(0L) },
            millisByApp = segments.groupBy { it.app }
                .mapValues { (_, appSegments) -> appSegments.sumOf { it.durationMillis.coerceAtLeast(0L) } }
                .filterValues { it > 0L },
        )
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
        .associateBy { it.date }

    val dayCount = ChronoUnit.DAYS.between(startDate, endDateInclusive).toInt()
    val dates = (0..dayCount).map { offset -> startDate.plusDays(offset.toLong()) }
    val totalsByWeekday = dates.groupBy(
        keySelector = { it.dayOfWeek },
        valueTransform = { totalsByDate[it] },
    )

    return DayOfWeek.entries.map { dayOfWeek ->
        val totals = totalsByWeekday[dayOfWeek].orEmpty()
        if (totals.isEmpty()) {
            return@map WeekdayUsageAverage(dayOfWeek = dayOfWeek, averageMillis = 0L)
        }
        // Days without usage still count towards the average, so divide by every matching date
        val dayCountForWeekday = totals.size
        val averageMillisByApp = mutableMapOf<BlockableApp, Long>()
        totals.filterNotNull().forEach { daily ->
            daily.millisByApp.forEach { (app, millis) -> averageMillisByApp[app] = (averageMillisByApp[app] ?: 0L) + millis }
        }
        WeekdayUsageAverage(
            dayOfWeek = dayOfWeek,
            averageMillis = totals.sumOf { it?.totalMillis ?: 0L } / dayCountForWeekday,
            averageMillisByApp = averageMillisByApp.mapValues { (_, millis) -> millis / dayCountForWeekday },
        )
    }
}
