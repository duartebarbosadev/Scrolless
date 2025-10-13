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

import com.scrolless.app.core.model.BlockingResult
import timber.log.Timber

/**
 * Blocks if the interval usage time plus the current session's elapsed time
 * exceed a specified time limit within the given interval.
 */
class IntervalTimerBlockHandler(private val timeLimit: Long) : BlockOptionHandler {

    private var usageTimeThroughInterval: Long = 0L

    /**
     * Checks if interval usage already exceeds the limit on entry.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @return true if should block immediately.
     */
    override fun onEnterContent(currentDailyUsage: Long): Boolean {
        val shouldBlock = usageTimeThroughInterval >= timeLimit
        Timber.d("Interval.onEnter: intervalUsage=%d, limit=%d -> shouldBlock=%s", usageTimeThroughInterval, timeLimit, shouldBlock)
        // If already exceeded interval usage, block immediately
        return shouldBlock
    }

    /**
     * Checks if adding the session time would exceed the interval limit.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @param elapsedTime Time elapsed in current session in milliseconds.
     * @return [BlockingResult.BlockNow] if should block, [BlockingResult.Continue] otherwise.
     */
    override fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        // Check if adding the current session's elapsed time would exceed the interval limit
        val totalIntervalUsage = usageTimeThroughInterval + elapsedTime
        val shouldBlock = totalIntervalUsage >= timeLimit
        Timber.v(
            "Interval.onPeriodic: intervalUsage=%d + elapsed=%d, limit=%d -> shouldBlock=%s",
            usageTimeThroughInterval,
            elapsedTime,
            timeLimit,
            shouldBlock,
        )
        return if (shouldBlock) BlockingResult.BlockNow else BlockingResult.Continue
    }

    /**
     * Accumulates the session time into interval usage.
     *
     * @param sessionTime Duration of the session in milliseconds.
     */
    override fun onExitContent(sessionTime: Long) {
        // Accumulate this session's time into the interval usage
        usageTimeThroughInterval += sessionTime
        Timber.v("Interval.onExit: +session=%d -> intervalUsage=%d", sessionTime, usageTimeThroughInterval)
    }
}
