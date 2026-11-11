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
import com.scrolless.app.core.model.BlockedHours
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.repository.BlockingConfigRepository
import timber.log.Timber

/**
 * Limits how long the user can watch during one interval window, and blocks everything during
 * [blockedHours].
 *
 * The settings come from the manager, the usage is read from the repository on every check, so the
 * handler keeps no state of its own.
 */
class IntervalTimerBlockHandler(
    private val allowanceMillis: Long,
    private val intervalLengthMillis: Long,
    private val blockedHours: BlockedHours,
    private val blockingConfigRepository: BlockingConfigRepository,
    private val timeProvider: TimeProvider,
) : BlockOptionHandler {

    private suspend fun usageAt(nowMillis: Long) =
        blockingConfigRepository.getConfig().intervalUsage.activeIntervalAt(nowMillis, intervalLengthMillis)

    private fun isBlockedHour(): Boolean = !blockedHours.isEmpty && blockedHours.isBlockedAt(timeProvider.localDateTimeNow())

    override suspend fun shouldBlockContent(currentDailyUsage: Long): Boolean {
        if (isBlockedHour()) {
            Timber.d("IntervalTimer.shouldBlock: blocked hour -> block=true")
            return true
        }

        val usage = usageAt(timeProvider.currentTimeInMillis()).usageMillis

        val shouldBlock = usage >= allowanceMillis
        Timber.d("IntervalTimer.shouldBlock: usage=%d/%d -> block=%s", usage, allowanceMillis, shouldBlock)
        return shouldBlock
    }

    /**
     * Includes the session in progress, which is not saved yet, when checking the allowance.
     *
     * The next check comes no later than the start of the next blocked hour, so a session that
     * runs into one is closed on time.
     */
    override suspend fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        if (isBlockedHour()) {
            Timber.v("IntervalTimer.onPeriodic: blocked hour -> block")
            return BlockingResult.BlockNow
        }

        val now = timeProvider.currentTimeInMillis()
        val usage = blockingConfigRepository.getConfig().intervalUsage
            .plusSession(sessionStartMillis = now - elapsedTime, sessionEndMillis = now, lengthMillis = intervalLengthMillis)
            .usageMillis

        if (usage >= allowanceMillis) {
            Timber.v("IntervalTimer.onPeriodic: usage=%d/%d -> block", usage, allowanceMillis)
            return BlockingResult.BlockNow
        }

        val untilBlockedHour = if (blockedHours.isEmpty) null else blockedHours.millisUntilNextBlocked(timeProvider.localDateTimeNow())
        val checkInMillis = minOf(allowanceMillis - usage, untilBlockedHour ?: Long.MAX_VALUE)
        Timber.v("IntervalTimer.onPeriodic: usage=%d/%d -> check in %d", usage, allowanceMillis, checkInMillis)
        return BlockingResult.CheckLater(checkInMillis)
    }

    override suspend fun onExitContent(sessionStartMillis: Long, sessionEndMillis: Long) {
        if (sessionEndMillis <= sessionStartMillis) return

        val usage = blockingConfigRepository.recordIntervalUsage(sessionStartMillis, sessionEndMillis)
        Timber.v("IntervalTimer.onExit: session=%d, usage=%d", sessionEndMillis - sessionStartMillis, usage.usageMillis)
    }
}
