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
package com.scrolless.app.core.blocking.handler

import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.repository.BlockingConfigRepository
import timber.log.Timber

/**
 * Limits how long the user can watch during one interval window.
 *
 * The settings come from the manager, the usage is read from the repository on every check, so the
 * handler keeps no state of its own.
 */
class IntervalTimerBlockHandler(
    private val allowanceMillis: Long,
    private val intervalLengthMillis: Long,
    private val blockingConfigRepository: BlockingConfigRepository,
    private val timeProvider: TimeProvider,
) : BlockOptionHandler {

    private suspend fun usageAt(nowMillis: Long) =
        blockingConfigRepository.getConfig().intervalUsage.activeIntervalAt(nowMillis, intervalLengthMillis)

    override suspend fun shouldBlockContent(currentDailyUsage: Long): Boolean {
        val usage = usageAt(timeProvider.currentTimeInMillis()).usageMillis

        val shouldBlock = usage >= allowanceMillis
        Timber.d("IntervalTimer.shouldBlock: usage=%d/%d -> block=%s", usage, allowanceMillis, shouldBlock)
        return shouldBlock
    }

    /** Includes the session in progress, which is not saved yet, when checking the allowance. */
    override suspend fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        val now = timeProvider.currentTimeInMillis()
        val usage = usageAt(now)
            .plusSession(sessionStartMillis = now - elapsedTime, sessionEndMillis = now, lengthMillis = intervalLengthMillis)
            .usageMillis

        if (usage >= allowanceMillis) {
            Timber.v("IntervalTimer.onPeriodic: usage=%d/%d -> block", usage, allowanceMillis)
            return BlockingResult.BlockNow
        }

        Timber.v("IntervalTimer.onPeriodic: usage=%d/%d -> continue", usage, allowanceMillis)
        return BlockingResult.CheckLater(allowanceMillis - usage)
    }

    override suspend fun onExitContent(sessionStartMillis: Long, sessionEndMillis: Long) {
        if (sessionEndMillis <= sessionStartMillis) return

        val usage = blockingConfigRepository.recordIntervalUsage(sessionStartMillis, sessionEndMillis)
        Timber.v("IntervalTimer.onExit: session=%d, usage=%d", sessionEndMillis - sessionStartMillis, usage.usageMillis)
    }
}
