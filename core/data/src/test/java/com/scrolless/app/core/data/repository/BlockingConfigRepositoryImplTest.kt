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

import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.data.database.model.BlockOptionType
import com.scrolless.app.core.data.database.model.UserSettingsEntity
import com.scrolless.app.core.model.BlockOption
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockingConfigRepositoryImplTest {

    private val dao = mockk<UserSettingsDao>(relaxed = true)
    private val repository = BlockingConfigRepositoryImpl(dao)

    @Test
    fun `changing the interval settings keeps the running window`() = runTest {
        repository.configureIntervalTimer(allowanceMillis = 2 * MINUTE_MILLIS, intervalLengthMillis = 60 * MINUTE_MILLIS)

        coVerify(exactly = 1) {
            dao.configureIntervalTimer(allowanceMillis = 2 * MINUTE_MILLIS, intervalLengthMillis = 60 * MINUTE_MILLIS)
        }
        coVerify(exactly = 0) { dao.updateIntervalState(any(), any()) }
    }

    @Test
    fun `selecting a mode does not overwrite its saved settings`() = runTest {
        repository.setActiveOption(BlockOption.DailyLimit(limitMillis = 1L))

        coVerify(exactly = 1) { dao.setActiveBlockOption(BlockOptionType.DailyLimit) }
        coVerify(exactly = 0) { dao.configureDailyLimit(any()) }
    }

    @Test
    fun `recording usage keeps the previous usage of the running window`() = runTest {
        coEvery { dao.getUserSettings() } returns config(windowStartMillis = 1_000L, usageMillis = 10_000L)

        val updated = repository.recordIntervalUsage(sessionStartMillis = 11_000L, sessionEndMillis = 21_000L)

        assertEquals(1_000L, updated.startMillis)
        assertEquals(20_000L, updated.usageMillis)
        coVerify { dao.updateIntervalState(windowStart = 1_000L, usage = 20_000L) }
    }

    @Test
    fun `recording a session across a boundary only counts the new window`() = runTest {
        coEvery { dao.getUserSettings() } returns config(
            intervalLengthMillis = 10_000L,
            windowStartMillis = 1_000L,
            usageMillis = 4_000L,
        )

        val updated = repository.recordIntervalUsage(sessionStartMillis = 9_000L, sessionEndMillis = 13_000L)

        assertEquals(11_000L, updated.startMillis)
        assertEquals(2_000L, updated.usageMillis)
        coVerify { dao.updateIntervalState(windowStart = 11_000L, usage = 2_000L) }
    }

    @Test
    fun `a session that started in interval mode is recorded after another mode is selected`() = runTest {
        coEvery { dao.getUserSettings() } returns config(
            activeOption = BlockOptionType.DailyLimit,
            windowStartMillis = 1_000L,
            usageMillis = 10_000L,
        )

        val updated = repository.recordIntervalUsage(sessionStartMillis = 11_000L, sessionEndMillis = 21_000L)

        assertEquals(20_000L, updated.usageMillis)
        coVerify { dao.updateIntervalState(windowStart = 1_000L, usage = 20_000L) }
    }

    @Test
    fun `reading a window that has not ended does not write to the database`() = runTest {
        coEvery { dao.getUserSettings() } returns config(windowStartMillis = 1_000L, usageMillis = 4_000L)

        val window = repository.getCurrentIntervalWindow(nowMillis = 5_000L)

        assertEquals(1_000L, window.startMillis)
        assertEquals(4_000L, window.usageMillis)
        coVerify(exactly = 0) { dao.updateIntervalState(any(), any()) }
    }

    @Test
    fun `reading a window that ended advances and saves it`() = runTest {
        coEvery { dao.getUserSettings() } returns config(
            intervalLengthMillis = 10_000L,
            windowStartMillis = 1_000L,
            usageMillis = 4_000L,
        )

        val window = repository.getCurrentIntervalWindow(nowMillis = 25_000L)

        assertEquals(21_000L, window.startMillis)
        assertEquals(0L, window.usageMillis)
        coVerify { dao.updateIntervalState(windowStart = 21_000L, usage = 0L) }
    }

    @Test
    fun `usage changes do not emit a new active option`() = runTest {
        every { dao.observeUserSettings() } returns flowOf(
            config(usageMillis = 0L),
            config(usageMillis = 10_000L),
            config(allowanceMillis = 2 * MINUTE_MILLIS, usageMillis = 10_000L),
        )

        val options = repository.observeActiveOption().toList()

        assertEquals(2, options.size)
    }

    @Test
    fun `a mode selected before it was configured blocks nothing`() = runTest {
        every { dao.observeUserSettings() } returns flowOf(
            config(activeOption = BlockOptionType.DailyLimit, dailyLimitMillis = 0L),
        )

        assertEquals(listOf(BlockOption.NothingSelected), repository.observeActiveOption().toList())
    }

    private fun config(
        activeOption: BlockOptionType = BlockOptionType.IntervalTimer,
        dailyLimitMillis: Long = 5 * MINUTE_MILLIS,
        allowanceMillis: Long = MINUTE_MILLIS,
        intervalLengthMillis: Long = 30 * MINUTE_MILLIS,
        windowStartMillis: Long = 1_000L,
        usageMillis: Long = 0L,
    ) = UserSettingsEntity(
        activeBlockOption = activeOption,
        dailyLimit = dailyLimitMillis,
        intervalAllowance = allowanceMillis,
        intervalLength = intervalLengthMillis,
        intervalWindowStartAt = windowStartMillis,
        intervalUsage = usageMillis,
        timerOverlayEnabled = false,
    )

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
