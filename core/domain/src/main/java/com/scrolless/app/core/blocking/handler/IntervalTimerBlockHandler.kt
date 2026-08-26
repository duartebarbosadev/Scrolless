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
 * The handler only decides when to block. The repository owns the window and its usage, so this
 * class keeps no state of its own and always reads the values that are saved right now.
 */
class IntervalTimerBlockHandler(
    private val allowanceMillis: Long,
    private val blockingConfigRepository: BlockingConfigRepository,
    private val timeProvider: TimeProvider,
) : BlockOptionHandler {

    override suspend fun onEnterContent(currentDailyUsage: Long): Boolean {
        val window = blockingConfigRepository.getCurrentIntervalWindow(timeProvider.currentTimeInMillis())

        val shouldBlock = window.usageMillis >= allowanceMillis
        Timber.d("IntervalTimer.onEnter: usage=%d/%d -> block=%s", window.usageMillis, allowanceMillis, shouldBlock)
        return shouldBlock
    }

    /**
     * Includes the session in progress, which is not saved yet, when checking the allowance.
     */
    override suspend fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        val now = timeProvider.currentTimeInMillis()
        val window = blockingConfigRepository.getCurrentIntervalWindow(now)
        val usage = window.plusSession(sessionStartMillis = now - elapsedTime, sessionEndMillis = now).usageMillis

        if (usage >= allowanceMillis) {
            Timber.v("IntervalTimer.onPeriodic: usage=%d/%d -> block", usage, allowanceMillis)
            return BlockingResult.BlockNow
        }

        Timber.v("IntervalTimer.onPeriodic: usage=%d/%d -> continue", usage, allowanceMillis)
        return BlockingResult.CheckLater(allowanceMillis - usage)
    }

    override suspend fun onExitContent(sessionStartMillis: Long, sessionEndMillis: Long) {
        if (sessionEndMillis <= sessionStartMillis) return

        val window = blockingConfigRepository.recordIntervalUsage(sessionStartMillis, sessionEndMillis)
        Timber.v("IntervalTimer.onExit: session=%d, usage=%d", sessionEndMillis - sessionStartMillis, window.usageMillis)
    }
}
