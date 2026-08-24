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
import com.scrolless.app.core.data.database.model.BlockingConfigEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BlockingConfigRepositoryImplTest {

    private val dao = mockk<UserSettingsDao>()
    private val repository = BlockingConfigRepositoryImpl(dao)

    @Test
    fun `saving expanded interval writes one coherent config and preserves usage`() = runTest {
        every { dao.observeBlockingConfig() } returns flowOf(
            BlockingConfigEntity(
                activeOption = BlockOptionType.IntervalTimer,
                limitMillis = MINUTE_MILLIS,
                intervalLengthMillis = 30 * MINUTE_MILLIS,
                intervalWindowStartMillis = 1_000L,
                intervalUsageMillis = 10_000L,
            ),
        )
        coEvery {
            dao.updateBlockingConfig(
                activeOption = any(),
                limitMillis = any(),
                intervalLengthMillis = any(),
                intervalWindowStartMillis = any(),
                intervalUsageMillis = any(),
            )
        } returns Unit

        repository.configureIntervalTimer(
            allowanceMillis = 2 * MINUTE_MILLIS,
            intervalLengthMillis = 60 * MINUTE_MILLIS,
        )

        coVerify(exactly = 1) {
            dao.updateBlockingConfig(
                activeOption = BlockOptionType.IntervalTimer,
                limitMillis = 2 * MINUTE_MILLIS,
                intervalLengthMillis = 60 * MINUTE_MILLIS,
                intervalWindowStartMillis = 1_000L,
                intervalUsageMillis = 10_000L,
            )
        }
    }

    @Test
    fun `configuring interval from another mode starts a fresh window`() = runTest {
        every { dao.observeBlockingConfig() } returns flowOf(
            BlockingConfigEntity(
                activeOption = BlockOptionType.DailyLimit,
                limitMillis = 5 * MINUTE_MILLIS,
                intervalLengthMillis = 30 * MINUTE_MILLIS,
                intervalWindowStartMillis = 1_000L,
                intervalUsageMillis = 10_000L,
            ),
        )
        coEvery {
            dao.updateBlockingConfig(
                activeOption = any(),
                limitMillis = any(),
                intervalLengthMillis = any(),
                intervalWindowStartMillis = any(),
                intervalUsageMillis = any(),
            )
        } returns Unit

        repository.configureIntervalTimer(
            allowanceMillis = 2 * MINUTE_MILLIS,
            intervalLengthMillis = 60 * MINUTE_MILLIS,
        )

        coVerify(exactly = 1) {
            dao.updateBlockingConfig(
                activeOption = BlockOptionType.IntervalTimer,
                limitMillis = 2 * MINUTE_MILLIS,
                intervalLengthMillis = 60 * MINUTE_MILLIS,
                intervalWindowStartMillis = 0L,
                intervalUsageMillis = 0L,
            )
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
