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
package com.scrolless.app.core.domain

import com.scrolless.app.core.blocking.BlockingManagerImpl
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.BlockingConfig
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.model.BlockingSettings
import com.scrolless.app.core.model.IntervalUsage
import com.scrolless.app.core.repository.BlockingConfigRepository
import com.scrolless.app.core.repository.SessionTracker
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingManagerImplTest {

    private val sessionTracker = FakeSessionTracker(dailyUsageMillis = 60_000L)
    private val timeProvider = object : TimeProvider {
        override fun currentTimeInMillis(): Long = 0L
        override fun localDateNow(): LocalDate = LocalDate.EPOCH
        override fun localDateTimeNow(): LocalDateTime = LocalDateTime.MIN
    }
    private val manager = BlockingManagerImpl(sessionTracker, NoopBlockingConfigRepository(), timeProvider)

    @Test
    fun `a daily limit selected before it was configured blocks nothing`() = runTest {
        manager.init(BlockOption.DailyLimit, BlockingSettings(dailyLimitMillis = 0L))

        assertFalse(manager.onEnterBlockedContent())
        assertEquals(BlockingResult.Continue, manager.onPeriodicCheck(elapsedTime = 60_000L))
    }

    @Test
    fun `an interval timer selected before it was configured blocks nothing`() = runTest {
        manager.init(BlockOption.IntervalTimer, BlockingSettings(intervalAllowanceMillis = 0L, intervalLengthMillis = 0L))

        assertFalse(manager.onEnterBlockedContent())
        assertEquals(BlockingResult.Continue, manager.onPeriodicCheck(elapsedTime = 60_000L))
    }

    @Test
    fun `a configured daily limit blocks once it is reached`() = runTest {
        manager.init(BlockOption.DailyLimit, BlockingSettings(dailyLimitMillis = 30_000L))

        assertTrue(manager.onEnterBlockedContent())
    }

    private class FakeSessionTracker(private val dailyUsageMillis: Long) : SessionTracker {
        override suspend fun getDailyUsage(): Long = dailyUsageMillis
        override suspend fun addToDailyUsage(sessionTime: Long, app: BlockableApp) = Unit
        override fun onAppOpen(app: BlockableApp) = Unit
        override fun onAppClose() = Unit
    }

    private class NoopBlockingConfigRepository : BlockingConfigRepository {
        override fun observeConfig(): Flow<BlockingConfig> = flowOf(BlockingConfig())
        override suspend fun getConfig(): BlockingConfig = BlockingConfig()
        override suspend fun setActiveOption(option: BlockOption) = Unit
        override suspend fun configureDailyLimit(limitMillis: Long) = Unit
        override suspend fun configureIntervalTimer(allowanceMillis: Long, intervalLengthMillis: Long) = Unit
        override suspend fun recordIntervalUsage(sessionStartMillis: Long, sessionEndMillis: Long) = IntervalUsage.NOT_STARTED
    }
}
