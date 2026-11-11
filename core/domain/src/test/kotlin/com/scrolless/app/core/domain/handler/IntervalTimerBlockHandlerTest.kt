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
import com.scrolless.app.core.model.BlockedHours
import com.scrolless.app.core.model.BlockingConfig
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.model.BlockingSettings
import com.scrolless.app.core.model.IntervalUsage
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
    private var localNow: LocalDateTime = LocalDateTime.of(2026, 10, 30, 12, 0)

    private val timeProvider = object : TimeProvider {
        override fun currentTimeInMillis(): Long = nowMillis
        override fun localDateNow(): LocalDate = LocalDate.EPOCH
        override fun localDateTimeNow(): LocalDateTime = localNow
    }

    private val handler = handlerWith(BlockedHours.NONE)

    // Blocks 20:00 to 06:00 and 09:00 to 16:00, the schedule from the feature request.
    private val scheduledHandler = handlerWith(BlockedHours.of(20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 9, 10, 11, 12, 13, 14, 15))

    private fun handlerWith(blockedHours: BlockedHours) = IntervalTimerBlockHandler(
        allowanceMillis = ALLOWANCE_MILLIS,
        intervalLengthMillis = INTERVAL_LENGTH_MILLIS,
        blockedHours = blockedHours,
        blockingConfigRepository = repository,
        timeProvider = timeProvider,
    )

    @Test
    fun `covered content becomes allowed in the next interval without recording a session`() = runTest(testDispatcher) {
        nowMillis = 2_000L
        val savedUsage = IntervalUsage(startMillis = 1_000L, usageMillis = ALLOWANCE_MILLIS)
        repository.usage = savedUsage
        repeat(3) { assertTrue(handler.shouldBlockContent(currentDailyUsage = 0L)) }
        assertEquals(savedUsage, repository.usage)

        nowMillis = 1_000L + INTERVAL_LENGTH_MILLIS
        assertFalse(handler.shouldBlockContent(currentDailyUsage = 0L))
        assertEquals(savedUsage, repository.usage)
    }

    @Test
    fun `entering under the allowance does not block`() = runTest(testDispatcher) {
        nowMillis = 2_000L
        repository.usage = IntervalUsage(startMillis = 1_000L, usageMillis = 500L)

        assertFalse(handler.onEnterContent(currentDailyUsage = 0L))
    }

    @Test
    fun `entering with the allowance used up blocks`() = runTest(testDispatcher) {
        nowMillis = 2_000L
        repository.usage = IntervalUsage(startMillis = 1_000L, usageMillis = ALLOWANCE_MILLIS)

        assertTrue(handler.onEnterContent(currentDailyUsage = 0L))
    }

    @Test
    fun `the running session counts toward the allowance`() = runTest(testDispatcher) {
        nowMillis = 4_500L
        repository.usage = IntervalUsage(startMillis = 1_000L, usageMillis = 2_000L)

        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 3_500L)

        assertTrue(result is BlockingResult.BlockNow)
    }

    @Test
    fun `the next check is scheduled for the remaining allowance`() = runTest(testDispatcher) {
        nowMillis = 2_500L
        repository.usage = IntervalUsage(startMillis = 500L, usageMillis = 1_000L)

        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 2_000L)

        assertEquals(2_000L, (result as BlockingResult.CheckLater).delayMillis)
    }

    @Test
    fun `a session crossing a window boundary only counts the new window`() = runTest(testDispatcher) {
        nowMillis = 13_000L
        repository.usage = IntervalUsage(startMillis = 1_000L, usageMillis = 1_000L)

        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 4_000L)

        assertEquals(3_000L, (result as BlockingResult.CheckLater).delayMillis)
    }

    @Test
    fun `entering during a blocked hour blocks with allowance left`() = runTest(testDispatcher) {
        nowMillis = 2_000L
        repository.usage = IntervalUsage.NOT_STARTED
        localNow = LocalDateTime.of(2026, 10, 30, 22, 15)

        assertTrue(scheduledHandler.onEnterContent(currentDailyUsage = 0L))
    }

    @Test
    fun `entering outside the blocked hours uses the allowance`() = runTest(testDispatcher) {
        nowMillis = 2_000L
        repository.usage = IntervalUsage.NOT_STARTED
        localNow = LocalDateTime.of(2026, 10, 30, 16, 0)

        assertFalse(scheduledHandler.onEnterContent(currentDailyUsage = 0L))
    }

    @Test
    fun `a running session is blocked once a blocked hour starts`() = runTest(testDispatcher) {
        nowMillis = 2_500L
        repository.usage = IntervalUsage(startMillis = 500L, usageMillis = 0L)
        localNow = LocalDateTime.of(2026, 10, 30, 9, 0)

        val result = scheduledHandler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 2_000L)

        assertTrue(result is BlockingResult.BlockNow)
    }

    @Test
    fun `the next check comes no later than the next blocked hour`() = runTest(testDispatcher) {
        nowMillis = 2_500L
        repository.usage = IntervalUsage(startMillis = 500L, usageMillis = 0L)
        localNow = LocalDateTime.of(2026, 10, 30, 8, 59, 59)

        val result = scheduledHandler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 2_000L)

        assertEquals(1_000L, (result as BlockingResult.CheckLater).delayMillis)
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

    private class FakeBlockingConfigRepository : BlockingConfigRepository {

        var usage = IntervalUsage(startMillis = 1_000L, usageMillis = 0L)
        var recordedSession: Pair<Long, Long>? = null

        private fun config() = BlockingConfig(
            activeOption = BlockOption.IntervalTimer,
            settings = BlockingSettings(
                intervalAllowanceMillis = ALLOWANCE_MILLIS,
                intervalLengthMillis = INTERVAL_LENGTH_MILLIS,
            ),
            intervalUsage = usage,
        )

        override fun observeConfig(): Flow<BlockingConfig> = flowOf(config())

        override suspend fun getConfig(): BlockingConfig = config()

        override suspend fun setActiveOption(option: BlockOption) = Unit

        override suspend fun configureDailyLimit(limitMillis: Long) = Unit

        override suspend fun configureIntervalTimer(allowanceMillis: Long, intervalLengthMillis: Long, blockedHours: BlockedHours) = Unit

        override suspend fun recordIntervalUsage(sessionStartMillis: Long, sessionEndMillis: Long): IntervalUsage {
            recordedSession = sessionStartMillis to sessionEndMillis
            usage = usage.plusSession(sessionStartMillis, sessionEndMillis, INTERVAL_LENGTH_MILLIS)

            return usage
        }
    }

    private companion object {
        const val ALLOWANCE_MILLIS = 5_000L
        const val INTERVAL_LENGTH_MILLIS = 10_000L
    }
}
