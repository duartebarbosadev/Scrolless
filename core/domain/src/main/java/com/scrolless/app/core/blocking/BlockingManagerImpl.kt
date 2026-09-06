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
package com.scrolless.app.core.blocking

import com.scrolless.app.core.blocking.handler.BlockAllBlockHandler
import com.scrolless.app.core.blocking.handler.BlockOptionHandler
import com.scrolless.app.core.blocking.handler.DayLimitBlockHandler
import com.scrolless.app.core.blocking.handler.IntervalTimerBlockHandler
import com.scrolless.app.core.blocking.handler.NoBlockHandler
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.model.BlockingSettings
import com.scrolless.app.core.repository.BlockingConfigRepository
import com.scrolless.app.core.repository.SessionTracker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Delegates viewing-session events to the handler for the selected [BlockOption].
 *
 * Handler changes and session events are processed one at a time.
 */
@Singleton
class BlockingManagerImpl @Inject constructor(
    private val sessionTracker: SessionTracker,
    private val blockingConfigRepository: BlockingConfigRepository,
    private val timeProvider: TimeProvider,
) : BlockingManager {

    private var handler: BlockOptionHandler = NoBlockHandler()

    // Settings changes, timer checks, and session exits arrive from different coroutines. Finish
    // one handler operation before another starts or replaces the handler.
    private val handlerMutex = Mutex()

    override suspend fun init(option: BlockOption, settings: BlockingSettings) = handlerMutex.withLock {
        Timber.i("Initializing blocking manager with %s", option)
        handler = createHandler(option, settings)
    }

    /**
     * Builds the handler that applies [option] during viewing sessions.
     *
     * A mode the user never configured has nothing to enforce, so it does not block.
     */
    private fun createHandler(option: BlockOption, settings: BlockingSettings): BlockOptionHandler = when (option) {
        BlockOption.BlockAll -> BlockAllBlockHandler(timeProvider)

        BlockOption.DailyLimit -> if (settings.dailyLimitMillis > 0L) {
            DayLimitBlockHandler(settings.dailyLimitMillis)
        } else {
            NoBlockHandler()
        }

        BlockOption.IntervalTimer -> if (settings.intervalAllowanceMillis > 0L && settings.intervalLengthMillis > 0L) {
            IntervalTimerBlockHandler(
                allowanceMillis = settings.intervalAllowanceMillis,
                intervalLengthMillis = settings.intervalLengthMillis,
                blockingConfigRepository = blockingConfigRepository,
                timeProvider = timeProvider,
            )
        } else {
            NoBlockHandler()
        }

        BlockOption.NothingSelected -> NoBlockHandler()
    }

    override suspend fun onEnterBlockedContent(): Boolean = handlerMutex.withLock {
        val currentDailyUsage = sessionTracker.getDailyUsage()
        val shouldBlock = handler.onEnterContent(currentDailyUsage)

        Timber.d("onEnterBlockedContent: daily=%d -> shouldBlock=%s", currentDailyUsage, shouldBlock)
        shouldBlock
    }

    override suspend fun shouldBlockContent(): Boolean = handlerMutex.withLock {
        handler.shouldBlockContent(sessionTracker.getDailyUsage())
    }

    override suspend fun onPeriodicCheck(elapsedTime: Long): BlockingResult = handlerMutex.withLock {
        val currentDailyUsage = sessionTracker.getDailyUsage()
        val result = handler.onPeriodicCheck(currentDailyUsage, elapsedTime)

        Timber.v("onPeriodicCheck: daily=%d, elapsed=%d -> result=%s", currentDailyUsage, elapsedTime, result)
        result
    }

    override suspend fun onExitBlockedContent(sessionStartMillis: Long, sessionEndMillis: Long) = handlerMutex.withLock {
        Timber.d("onExitBlockedContent: session=%d", sessionEndMillis - sessionStartMillis)
        handler.onExitContent(sessionStartMillis, sessionEndMillis)
    }
}
