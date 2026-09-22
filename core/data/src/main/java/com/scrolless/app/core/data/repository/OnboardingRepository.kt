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

import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.model.BlockOption
import java.io.Serializable
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class OnboardingRepository @Inject constructor(private val dao: UserSettingsDao, private val timeProvider: TimeProvider) {
    val completed = dao.observeUserSettings().map { it.hasCompletedOnboarding }.distinctUntilChanged()

    suspend fun load(): OnboardingPreferences {
        val settings = dao.getUserSettings()
        return OnboardingPreferences(
            option = if (!settings.hasCompletedOnboarding && settings.activeBlockOption == BlockOption.NothingSelected) {
                BlockOption.BlockAll
            } else {
                settings.activeBlockOption
            },
            dailyLimit = settings.dailyLimit,
            allowance = settings.intervalAllowance,
            intervalLength = settings.intervalLength,
            allowDm = settings.allowVideosSentByDm,
            includeStories = settings.includeStories,
        )
    }

    suspend fun save(preferences: OnboardingPreferences) {
        require(preferences.option != BlockOption.DailyLimit || preferences.dailyLimit > 0)
        require(preferences.option != BlockOption.IntervalTimer || (preferences.allowance > 0 && preferences.intervalLength > 0))
        dao.completeOnboarding(
            preferences.option,
            preferences.dailyLimit,
            preferences.allowance,
            preferences.intervalLength,
            preferences.allowDm,
            preferences.includeStories,
            timeProvider.currentTimeInMillis(),
        )
    }

    suspend fun skip() = dao.skipOnboarding()
}

// Serializable so the unfinished draft survives process recreation through SavedStateHandle.
data class OnboardingPreferences(
    val option: BlockOption = BlockOption.BlockAll,
    val dailyLimit: Long = 0L,
    val allowance: Long = 0L,
    val intervalLength: Long = 0L,
    val allowDm: Boolean = false,
    val includeStories: Boolean = false,
) : Serializable
