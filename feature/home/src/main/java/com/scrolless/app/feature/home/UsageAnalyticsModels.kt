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

import androidx.compose.runtime.Immutable
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.model.usage.WeekdayUsageAverage
import java.time.LocalDate
import java.time.ZonedDateTime

enum class UsageAveragePeriod(val labelResId: Int) {
    LAST_WEEK(R.string.usage_analytics_average_week),
    LAST_MONTH(R.string.usage_analytics_average_month),
    LAST_YEAR(R.string.usage_analytics_average_year),
}

const val ANALYTICS_PAGER_DAY_COUNT = 365

@Immutable
data class UsageAnalyticsUiState(
    val selectedDate: LocalDate = ZonedDateTime.now().toLocalDate(),
    val today: LocalDate = ZonedDateTime.now().toLocalDate(),
    val dailyTotalMillis: Long = 0L,
    val sessionSegments: List<SessionSegment> = emptyList(),
    val appTotals: List<AppUsageTotal> = emptyList(),
    val daySummaries: Map<LocalDate, UsageAnalyticsDayUiState> = emptyMap(),
    val weekdayAverages: List<WeekdayUsageAverage> = emptyList(),
    val canNavigateNext: Boolean = false,
    val dataStartDate: LocalDate = ZonedDateTime.now().toLocalDate(),
)

@Immutable
data class UsageAnalyticsDayUiState(
    val date: LocalDate,
    val dailyTotalMillis: Long = 0L,
    val sessionSegments: List<SessionSegment> = emptyList(),
    val appTotals: List<AppUsageTotal> = emptyList(),
)

@Immutable
data class AppUsageTotal(val app: BlockableApp, val totalMillis: Long)
