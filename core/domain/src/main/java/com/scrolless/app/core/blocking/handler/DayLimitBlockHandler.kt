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
 * Blocks content when daily usage exceeds the configured time limit.
 *
 * @param timeLimit Daily time limit in milliseconds.
 */
class DayLimitBlockHandler(private val timeLimit: Long) : BlockOptionHandler {

    /**
     * Checks if daily usage already exceeds the limit on entry.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @return true if should block immediately.
     */
    override fun onEnterContent(currentDailyUsage: Long): Boolean {

        val shouldBlock = currentDailyUsage >= timeLimit
        Timber.d("DayLimit.onEnter: daily=%d, limit=%d -> shouldBlock=%s", currentDailyUsage, timeLimit, shouldBlock)
        // If already exceeded, block immediately
        return shouldBlock
    }

    /**
     * Checks if the session would exceed the daily limit.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @param elapsedTime Time elapsed in current session in milliseconds.
     * @return [BlockingResult.BlockNow] if should block, [BlockingResult.Continue] otherwise.
     */
    override fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        val willExceed = (currentDailyUsage + elapsedTime) >= timeLimit

        // Check if crossing daily limit
        return if (willExceed) {
            Timber.v("DayLimit.onPeriodic exceeded: daily=%d + elapsed=%d, limit=%d", currentDailyUsage, elapsedTime, timeLimit)
            BlockingResult.BlockNow
        } else {
            val nextCheckTime = timeLimit - (currentDailyUsage + elapsedTime)
            Timber.v(
                "DayLimit.onPeriodic: daily=%d + elapsed=%d, limit=%d -> willExceed=no continue for %d ms",
                currentDailyUsage,
                elapsedTime,
                timeLimit,
                nextCheckTime,
            )
            BlockingResult.CheckLater(nextCheckTime)
        }
    }

    /**
     * No additional logic needed on exit.
     *
     * @param sessionTime Duration of the session in milliseconds.
     */
    override fun onExitContent(sessionTime: Long) {
        Timber.v("DayLimit.onExit: session=%d", sessionTime)
    }
}
