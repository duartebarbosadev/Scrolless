/*
 * Copyright (C) 2025 Scrolless
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
package com.scrolless.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.data.repository.AppUsageRepository
import com.scrolless.app.core.data.repository.UserSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class UsageStatsViewModel @Inject constructor(
    appUsageRepository: AppUsageRepository,
    userSettingsStore: UserSettingsStore,
) : ViewModel() {

    val uiState: StateFlow<UsageStatsUiState> = combine(
        appUsageRepository.getTodayUsageByApp(),
        userSettingsStore.getTotalDailyUsage(),
        appUsageRepository.getUsageForLastDays(7),
    ) { todayByApp, totalDailyUsage, weeklyUsage ->
        // Calculate per-app stats
        val reelsUsage = todayByApp["REELS"] ?: 0L
        val shortsUsage = todayByApp["SHORTS"] ?: 0L
        val tiktokUsage = todayByApp["TIKTOK"] ?: 0L

        // Calculate weekly totals per app
        val weeklyByApp = weeklyUsage.groupBy { it.appName }
            .mapValues { (_, usages) -> usages.sumOf { it.usageMillis } }

        val weeklyReels = weeklyByApp["REELS"] ?: 0L
        val weeklyShorts = weeklyByApp["SHORTS"] ?: 0L
        val weeklyTiktok = weeklyByApp["TIKTOK"] ?: 0L
        val weeklyTotal = weeklyReels + weeklyShorts + weeklyTiktok

        UsageStatsUiState(
            totalTodayUsage = totalDailyUsage,
            reelsUsage = reelsUsage,
            shortsUsage = shortsUsage,
            tiktokUsage = tiktokUsage,
            weeklyTotal = weeklyTotal,
            weeklyReels = weeklyReels,
            weeklyShorts = weeklyShorts,
            weeklyTiktok = weeklyTiktok,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UsageStatsUiState(),
    )
}

data class UsageStatsUiState(
    val totalTodayUsage: Long = 0L,
    val reelsUsage: Long = 0L,
    val shortsUsage: Long = 0L,
    val tiktokUsage: Long = 0L,
    val weeklyTotal: Long = 0L,
    val weeklyReels: Long = 0L,
    val weeklyShorts: Long = 0L,
    val weeklyTiktok: Long = 0L,
    val isLoading: Boolean = true,
)
