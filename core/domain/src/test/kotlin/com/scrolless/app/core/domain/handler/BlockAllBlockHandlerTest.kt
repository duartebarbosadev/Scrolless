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

import com.scrolless.app.core.blocking.handler.BlockAllBlockHandler
import com.scrolless.app.core.blocking.handler.BlockOptionHandler
import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.domain.utils.TestSchedulerTimeProvider
import com.scrolless.app.core.model.BlockingResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlockAllBlockHandlerTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()
    private val blockAllBlockHandler: BlockOptionHandler =
        BlockAllBlockHandler(TestSchedulerTimeProvider(testDispatcher.scheduler))

    @Test
    fun whenEnterContent_alwaysBlocks() = runTest(testDispatcher) {
        val didBlock = blockAllBlockHandler.onEnterContent(0L)
        assert(didBlock)
    }

    @Test
    fun whenPeriodicCheck_alwaysBlocks() = runTest(testDispatcher) {
        val result = blockAllBlockHandler.onPeriodicCheck(0L, 0L)
        assert(result is BlockingResult.BlockNow)
    }

    @Test
    fun whenExitContent_resetsState() = runTest(testDispatcher) {
        blockAllBlockHandler.onExitContent(0L)
        // No state to verify directly, but ensure no exceptions occur
    }

    @Test
    fun whenSpamPeriodicCheck_ignoresRapidReblocks() = runTest(testDispatcher) {

        blockAllBlockHandler.onEnterContent(0L)
        val firstCheck = blockAllBlockHandler.onPeriodicCheck(0L, 0L)
        assert(firstCheck is BlockingResult.BlockNow)

        // Simulate rapid re-check
        val rapidCheck1 = blockAllBlockHandler.onPeriodicCheck(0L, 0L)
        assert(rapidCheck1 is BlockingResult.BlockNow)

        // Simulate rapid re-check
        val rapidCheck2 = blockAllBlockHandler.onPeriodicCheck(0L, 0L)
        assert(rapidCheck2 is BlockingResult.Continue)

        // Simulate rapid re-check
        val rapidCheck3 = blockAllBlockHandler.onPeriodicCheck(0L, 0L)
        assert(rapidCheck3 is BlockingResult.Continue)

        // After some time, it should block again
        delay(1500)
        val laterCheck = blockAllBlockHandler.onPeriodicCheck(0L, 0L)
        assert(laterCheck is BlockingResult.BlockNow)
    }
}
