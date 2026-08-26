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
package com.scrolless.app.core.domain.handler

import com.scrolless.app.core.blocking.handler.IntervalTimerBlockHandler
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockingConfig
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.model.IntervalUsageWindow
import com.scrolless.app.core.repository.BlockingConfigRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntervalTimerBlockHandlerTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeBlockingConfigRepository()
    private var nowMillis = 0L

    private val timeProvider = object : TimeProvider {
        override fun currentTimeInMillis(): Long = nowMillis
        override fun localDateNow(): LocalDate = LocalDate.EPOCH
        override fun localDateTimeNow(): LocalDateTime = LocalDateTime.MIN
    }

    private val handler = IntervalTimerBlockHandler(
        allowanceMillis = ALLOWANCE_MILLIS,
        blockingConfigRepository = repository,
        timeProvider = timeProvider,
    )

    @Test
    fun `entering under the allowance does not block`() = runTest(testDispatcher) {
        nowMillis = 2_000L
        repository.window = window(usageMillis = 500L)

        assertFalse(handler.onEnterContent(currentDailyUsage = 0L))
    }

    @Test
    fun `entering with the allowance used up blocks`() = runTest(testDispatcher) {
        nowMillis = 2_000L
        repository.window = window(usageMillis = ALLOWANCE_MILLIS)

        assertTrue(handler.onEnterContent(currentDailyUsage = 0L))
    }

    @Test
    fun `the running session counts toward the allowance`() = runTest(testDispatcher) {
        nowMillis = 4_500L
        repository.window = window(usageMillis = 2_000L)

        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 3_500L)

        assertTrue(result is BlockingResult.BlockNow)
    }

    @Test
    fun `the next check is scheduled for the remaining allowance`() = runTest(testDispatcher) {
        nowMillis = 2_500L
        repository.window = window(startMillis = 500L, usageMillis = 1_000L)

        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 2_000L)

        assertEquals(2_000L, (result as BlockingResult.CheckLater).delayMillis)
    }

    @Test
    fun `a session crossing a window boundary only counts the new window`() = runTest(testDispatcher) {
        nowMillis = 13_000L
        repository.window = window(startMillis = 1_000L, lengthMillis = 10_000L, usageMillis = 1_000L)

        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 4_000L)

        assertEquals(3_000L, (result as BlockingResult.CheckLater).delayMillis)
    }

    @Test
    fun `exiting records the session with its timestamps`() = runTest(testDispatcher) {
        handler.onExitContent(sessionStartMillis = 2_000L, sessionEndMillis = 4_000L)

        assertEquals(2_000L to 4_000L, repository.recordedSession)
    }

    @Test
    fun `exiting without watched time records nothing`() = runTest(testDispatcher) {
        handler.onExitContent(sessionStartMillis = 2_000L, sessionEndMillis = 2_000L)

        assertEquals(null, repository.recordedSession)
    }

    private fun window(startMillis: Long = 1_000L, lengthMillis: Long = INTERVAL_LENGTH_MILLIS, usageMillis: Long = 0L) =
        IntervalUsageWindow(startMillis = startMillis, lengthMillis = lengthMillis, usageMillis = usageMillis)

    private class FakeBlockingConfigRepository : BlockingConfigRepository {

        var window = IntervalUsageWindow(startMillis = 1_000L, lengthMillis = INTERVAL_LENGTH_MILLIS, usageMillis = 0L)
        var recordedSession: Pair<Long, Long>? = null

        override fun observeConfig(): Flow<BlockingConfig> = flowOf(BlockingConfig())

        override fun observeActiveOption(): Flow<BlockOption> = flowOf(BlockOption.NothingSelected)

        override suspend fun getConfig(): BlockingConfig = BlockingConfig()

        override suspend fun setActiveOption(option: BlockOption) = Unit

        override suspend fun configureDailyLimit(limitMillis: Long) = Unit

        override suspend fun configureIntervalTimer(allowanceMillis: Long, intervalLengthMillis: Long) = Unit

        override suspend fun getCurrentIntervalWindow(nowMillis: Long): IntervalUsageWindow = window.currentAt(nowMillis)

        override suspend fun recordIntervalUsage(sessionStartMillis: Long, sessionEndMillis: Long): IntervalUsageWindow {
            recordedSession = sessionStartMillis to sessionEndMillis
            window = window.plusSession(sessionStartMillis, sessionEndMillis)

            return window
        }
    }

    private companion object {
        const val ALLOWANCE_MILLIS = 5_000L
        const val INTERVAL_LENGTH_MILLIS = 10_000L
    }
}
