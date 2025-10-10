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
package com.scrolless.app.core.blocking.handler

import com.scrolless.app.core.blocking.time.SystemTimeProvider
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.model.BlockingResult
import timber.log.Timber

/**
 * Immediately blocks any blocked content without considering time limits.
 */
class BlockAllBlockHandler(private val timeProvider: TimeProvider = SystemTimeProvider) : BlockOptionHandler {

    private var lastBlockTime = 0L
    private var blockAttempts = 0

    override fun onEnterContent(currentDailyUsage: Long): Boolean {
        Timber.d("BlockAll.onEnterContent: daily=%d -> block", currentDailyUsage)
        // Always block immediately.

        lastBlockTime = timeProvider.currentTimeInMillis()
        return true
    }

    /**
     * Always blocks on periodic check, but ignores rapid re-blocks to prevent spam.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @param elapsedTime Time elapsed in current session in milliseconds.
     * @return [BlockingResult.BlockNow] to block, or [BlockingResult.Continue] for rapid re-block.
     */
    override fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult = runSafe {

        lastBlockTime = timeProvider.currentTimeInMillis()
        Timber.v("BlockAll.onPeriodicCheck: daily=%d, elapsed=%d -> blocking now", currentDailyUsage, elapsedTime)
        return@runSafe BlockingResult.BlockNow
    }

    private fun runSafe(function: () -> BlockingResult): BlockingResult {

        // If the third block was within 1.2 seconds, and this is not the first block attempt,
        //  ignore to prevent spam back key
        val hasBlockedTooManyTimes = (++blockAttempts > 2) && ((timeProvider.currentTimeInMillis() - lastBlockTime) < 1200)

        if (hasBlockedTooManyTimes) {
            Timber.w("BlockAll.onPeriodicCheck: Too many blocks, ignoring to prevent infinite loop")
            return BlockingResult.Continue
        }
        return function()
    }

    override fun onExitContent(sessionTime: Long) {
        Timber.v("BlockAll.onExitContent: session=%d", sessionTime)
        blockAttempts = 0
        lastBlockTime = 0L
    }
}
