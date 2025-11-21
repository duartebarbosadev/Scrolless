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
package com.scrolless.app.core.domain.handler

import com.scrolless.app.core.blocking.handler.BlockOptionHandler
import com.scrolless.app.core.blocking.handler.IntervalTimerBlockHandler
import com.scrolless.app.core.blocking.handler.IntervalTimerState
import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.model.BlockingResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntervalTimerBlockHandlerTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()
    private var nowMillis = 0L
    private val stateChanges = mutableListOf<IntervalTimerState>()

    private fun createHandler(
        allowanceMillis: Long = ALLOWANCE_MILLIS,
        intervalLengthMillis: Long = INTERVAL_LENGTH_MILLIS,
        initialState: IntervalTimerState = IntervalTimerState(windowStartMillis = NOW_START, usageMillis = 0L),
    ): BlockOptionHandler {
        stateChanges.clear()
        return IntervalTimerBlockHandler(
            allowanceMillis = allowanceMillis,
            intervalLengthMillis = intervalLengthMillis,
            initialState = initialState,
            onStateChanged = { stateChanges.add(it) },
            currentTimeProvider = { nowMillis },
        )
    }

    @Test
    fun onEnterContent_whenUnderAllowance_doesNotBlock() = runTest(testDispatcher) {
        nowMillis = 1_000L
        val handler = createHandler(
            initialState = IntervalTimerState(windowStartMillis = 1_000L, usageMillis = 500L),
        )

        val didBlock = handler.onEnterContent(0L)

        assert(!didBlock)
        assert(stateChanges.isEmpty())
    }

    @Test
    fun onPeriodicCheck_whenProjectedUsageExceedsLimit_blocksAndClampsUsage() = runTest(testDispatcher) {
        nowMillis = 1_000L
        val handler = createHandler(
            initialState = IntervalTimerState(windowStartMillis = 1_000L, usageMillis = 2_000L),
        )
        handler.onEnterContent(0L)

        nowMillis = 2_000L
        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 3_500L)

        assert(result is BlockingResult.BlockNow)
        assertEquals(ALLOWANCE_MILLIS, stateChanges.last().usageMillis)
    }

    @Test
    fun onPeriodicCheck_whenAllowanceRemaining_returnsCheckLaterWithRemainingTime() = runTest(testDispatcher) {
        nowMillis = 500L
        val handler = createHandler(
            initialState = IntervalTimerState(windowStartMillis = 500L, usageMillis = 1_000L),
        )
        handler.onEnterContent(0L)

        nowMillis = 1_000L
        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 2_000L)

        assert(result is BlockingResult.CheckLater)
        val nextCheck = result as BlockingResult.CheckLater
        assertEquals(2_000L, nextCheck.delayMillis)
        assert(stateChanges.isEmpty())
    }

    @Test
    fun onExitContent_whenSessionCompletes_updatesUsageAndState() = runTest(testDispatcher) {
        nowMillis = 2_000L
        val handler = createHandler(
            initialState = IntervalTimerState(windowStartMillis = 2_000L, usageMillis = 1_500L),
        )
        handler.onEnterContent(0L)

        nowMillis = 3_000L
        handler.onExitContent(sessionTime = 2_000L)

        assertEquals(3_500L, stateChanges.last().usageMillis)
    }

    @Test
    fun onEnterContent_whenIntervalElapsed_resetsWindowAndUsage() = runTest(testDispatcher) {
        nowMillis = 1_000L
        val handler = createHandler(
            initialState = IntervalTimerState(windowStartMillis = 1_000L, usageMillis = 4_000L),
            intervalLengthMillis = 2_000L,
        )

        nowMillis = 5_000L
        val didBlock = handler.onEnterContent(0L)

        assert(!didBlock)
        val refreshedState = stateChanges.last()
        assertEquals(5_000L, refreshedState.windowStartMillis)
        assertEquals(0L, refreshedState.usageMillis)
    }

    companion object {
        private const val ALLOWANCE_MILLIS = 5_000L
        private const val INTERVAL_LENGTH_MILLIS = 10_000L
        private const val NOW_START = 0L
    }
}
