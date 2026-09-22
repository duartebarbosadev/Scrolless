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
import com.scrolless.app.core.data.database.model.UserSettingsEntity
import com.scrolless.app.core.model.BlockOption
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// MockK verification checks calls returning Flow without collecting them.
@Suppress("UnusedFlow")
class OnboardingRepositoryTest {
    private val dao = mockk<UserSettingsDao>(relaxed = true)
    private val timeProvider = mockk<TimeProvider> {
        every { currentTimeInMillis() } returns 25_000L
    }

    private fun repository(
        settings: UserSettingsEntity = UserSettingsEntity(activeBlockOption = BlockOption.NothingSelected),
    ): OnboardingRepository {
        every { dao.observeUserSettings() } returns flowOf(settings)
        coEvery { dao.getUserSettings() } returns settings
        return OnboardingRepository(dao, timeProvider)
    }

    @Test
    fun `new setup proposes blocking without applying the draft`() = runTest {
        val repository = repository(UserSettingsEntity(activeBlockOption = BlockOption.NothingSelected))

        assertEquals(BlockOption.BlockAll, repository.load().option)
        coVerify(exactly = 1) { dao.getUserSettings() }
        verify(exactly = 1) { dao.observeUserSettings() }
        confirmVerified(dao)
    }

    @Test
    fun `reopening setup keeps the saved choices including disabled blocking`() = runTest {
        val repository = repository(
            UserSettingsEntity(
                hasCompletedOnboarding = true,
                activeBlockOption = BlockOption.NothingSelected,
                dailyLimit = 900_000L,
                intervalAllowance = 300_000L,
                intervalLength = 3_600_000L,
                allowVideosSentByDm = true,
                includeStories = true,
            ),
        )

        assertEquals(
            OnboardingPreferences(BlockOption.NothingSelected, 900_000L, 300_000L, 3_600_000L, true, true),
            repository.load(),
        )
    }

    @Test
    fun `finishing passes all choices to one completion update`() = runTest {
        val repository = repository()

        repository.save(OnboardingPreferences(BlockOption.DailyLimit, 900_000L, 300_000L, 3_600_000L, true, true))

        coVerify(exactly = 1) {
            dao.completeOnboarding(BlockOption.DailyLimit, 900_000L, 300_000L, 3_600_000L, true, true, 25_000L)
        }
        verify(exactly = 1) { dao.observeUserSettings() }
        confirmVerified(dao)
    }

    @Test
    fun `skipping only marks onboarding skipped`() = runTest {
        val repository = repository()

        repository.skip()

        coVerify(exactly = 1) { dao.skipOnboarding() }
        verify(exactly = 1) { dao.observeUserSettings() }
        confirmVerified(dao)
    }

    @Test
    fun `invalid timers do not finish onboarding`() = runTest {
        val repository = repository()
        val invalidChoices = listOf(
            OnboardingPreferences(option = BlockOption.DailyLimit),
            OnboardingPreferences(option = BlockOption.IntervalTimer, intervalLength = 3_600_000L),
            OnboardingPreferences(option = BlockOption.IntervalTimer, allowance = 300_000L),
        )

        for (preferences in invalidChoices) {
            try {
                repository.save(preferences)
                throw AssertionError("Expected invalid timer to be rejected")
            } catch (_: IllegalArgumentException) {
                // Validation must reject the draft before writing settings.
            }
        }
        verify(exactly = 1) { dao.observeUserSettings() }
        confirmVerified(dao)
    }

    @Test
    fun `completion only emits when onboarding status changes`() = runTest {
        val settings = UserSettingsEntity(activeBlockOption = BlockOption.NothingSelected, hasCompletedOnboarding = false)
        every { dao.observeUserSettings() } returns flowOf(
            settings,
            settings.copy(allowVideosSentByDm = true),
            settings.copy(hasCompletedOnboarding = true),
        )

        assertEquals(listOf(false, true), OnboardingRepository(dao, timeProvider).completed.toList())
    }
}
