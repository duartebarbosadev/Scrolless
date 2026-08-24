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
import com.scrolless.app.core.blocking.handler.IntervalTimerState
import com.scrolless.app.core.blocking.handler.NoBlockHandler
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockingResult
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

    private lateinit var handler: BlockOptionHandler

    // Settings changes, timer checks, and session exits can arrive from different coroutines.
    // Finish one handler operation before another starts or replaces the handler.
    private val handlerMutex = Mutex()

    /**
     * Initializes the manager with a block option configuration.
     *
     * @param option The blocking option to apply.
     */
    override suspend fun init(option: BlockOption) = handlerMutex.withLock {
        Timber.i("Initializing blocking manager with %s", option)
        handler = createHandler(option)
    }

    private fun createHandler(option: BlockOption): BlockOptionHandler = when (option) {
        BlockOption.BlockAll -> BlockAllBlockHandler(timeProvider).also { Timber.d("Using BlockAll handler") }

        is BlockOption.DailyLimit -> DayLimitBlockHandler(option.limitMillis)
            .also { Timber.d("Using DayLimit handler (limit=%d)", option.limitMillis) }

        is BlockOption.IntervalTimer -> {
            val intervalState = IntervalTimerState(
                windowStartMillis = option.window.startMillis,
                usageMillis = option.window.usageMillis,
            )
            IntervalTimerBlockHandler(
                allowanceMillis = option.allowanceMillis,
                intervalLengthMillis = option.window.lengthMillis,
                initialState = intervalState,
                saveState = { state ->
                    // Await this write so the overlay cannot read the previous window's values.
                    blockingConfigRepository.updateIntervalWindow(
                        windowStartMillis = state.windowStartMillis,
                        usageMillis = state.usageMillis,
                    )
                },
            ).also {
                Timber.d(
                    "Using IntervalTimer handler (limit=%d, interval=%d)",
                    option.allowanceMillis,
                    option.window.lengthMillis,
                )
            }
        }

        BlockOption.NothingSelected -> NoBlockHandler().also { Timber.d("Using NothingSelected handler") }
    }

    /**
     * Checks current usage when the user enters blocked content.
     *
     * @return `true` when the content should be closed immediately.
     */
    override suspend fun onEnterBlockedContent(): Boolean {
        return handlerMutex.withLock {
            val currentDailyUsage = sessionTracker.getDailyUsage()
            val shouldBlock = handler.onEnterContent(currentDailyUsage)

            Timber.d("onEnterBlockedContent: daily=%d -> shouldBlock=%s", currentDailyUsage, shouldBlock)
            shouldBlock
        }
    }

    /**
     * Checks whether the active handler allows the viewing session to continue.
     *
     * @param elapsedTime Time since the session started, in milliseconds.
     * @return The next action for the accessibility service.
     */
    override suspend fun onPeriodicCheck(elapsedTime: Long): BlockingResult {
        return handlerMutex.withLock {
            val currentDailyUsage = sessionTracker.getDailyUsage()
            val result = handler.onPeriodicCheck(currentDailyUsage, elapsedTime)

            Timber.v("onPeriodicCheck: daily=%d, elapsed=%d -> result=%s", currentDailyUsage, elapsedTime, result)
            result
        }
    }

    /**
     * Gives the completed session duration to the active handler so it can save usage.
     *
     * @param sessionTime Total session duration, in milliseconds.
     */
    override suspend fun onExitBlockedContent(sessionTime: Long) = handlerMutex.withLock {
        Timber.d("onExitBlockedContent: session=%d", sessionTime)
        handler.onExitContent(sessionTime)
    }
}
