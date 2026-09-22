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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OnboardingIntervalTest {
    private val dao = mockk<UserSettingsDao>(relaxed = true)
    private var nowMillis = 25_000L
    private val clock = mockk<TimeProvider> {
        every { currentTimeInMillis() } answers { nowMillis }
    }

    private suspend fun saveWithInterval(start: Long, usage: Long, expectedStart: Long, expectedUsage: Long) {
        val settings = UserSettingsEntity(
            activeBlockOption = BlockOption.IntervalTimer,
            hasCompletedOnboarding = true,
            intervalLength = 10_000L,
            intervalWindowStartAt = start,
            intervalUsage = usage,
        )
        every { dao.observeUserSettings() } returns flowOf(settings)
        coEvery { dao.getUserSettings() } returns settings
        // Run the real transaction body while mocking only the database reads and writes.
        coEvery { dao.completeOnboarding(any(), any(), any(), any(), any(), any(), any()) } coAnswers { callOriginal() }

        OnboardingRepository(dao, clock).save(
            OnboardingPreferences(option = BlockOption.IntervalTimer, allowance = 4_000L, intervalLength = 60_000L),
        )

        coVerify(exactly = 1) {
            dao.completeOnboarding(BlockOption.IntervalTimer, 0L, 4_000L, 60_000L, false, false, expectedStart, expectedUsage)
        }
    }

    @Test
    fun `extending an expired interval does not revive exhausted usage`() = runTest {
        saveWithInterval(start = 1_000L, usage = 4_000L, expectedStart = 0L, expectedUsage = 0L)
    }

    @Test
    fun `interval expires at its exact old boundary`() = runTest {
        nowMillis = 11_000L
        saveWithInterval(start = 1_000L, usage = 4_000L, expectedStart = 0L, expectedUsage = 0L)
    }

    @Test
    fun `extending an active interval preserves its usage`() = runTest {
        nowMillis = 5_000L
        saveWithInterval(start = 1_000L, usage = 4_000L, expectedStart = 1_000L, expectedUsage = 4_000L)
    }

    @Test
    fun `an idle interval stays idle until viewing starts`() = runTest {
        saveWithInterval(start = 0L, usage = 0L, expectedStart = 0L, expectedUsage = 0L)
    }

    @Test
    fun `moving the clock backwards does not reset usage`() = runTest {
        nowMillis = 500L
        saveWithInterval(start = 1_000L, usage = 4_000L, expectedStart = 1_000L, expectedUsage = 4_000L)
    }
}
