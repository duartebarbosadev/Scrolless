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
package com.scrolless.app.core.blocking

import com.scrolless.app.core.blocking.handler.BlockAllBlockHandler
import com.scrolless.app.core.blocking.handler.BlockOptionHandler
import com.scrolless.app.core.blocking.handler.DayLimitBlockHandler
import com.scrolless.app.core.blocking.handler.IntervalTimerBlockHandler
import com.scrolless.app.core.blocking.handler.IntervalTimerState
import com.scrolless.app.core.blocking.handler.NoBlockHandler
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.repository.SessionTracker
import com.scrolless.app.core.repository.UserSettingsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages blocking logic for restricted content based on configured [BlockOption].
 *
 * Uses a [BlockOptionHandler] strategy pattern to delegate blocking decisions
 * based on the active block configuration.
 */
@Singleton
class BlockingManagerImpl @Inject constructor(
    private val sessionTracker: SessionTracker,
    private val userSettingsStore: UserSettingsStore,
) : BlockingManager {

    private lateinit var handler: BlockOptionHandler
    private val persistenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Initializes the manager with a block option configuration.
     * Sets up the appropriate handler and ensures usage data is current.
     *
     * @param blockOption The blocking option to apply.
     */
    override suspend fun init(blockOption: BlockOption) {
        val timeLimit = userSettingsStore.getTimeLimit().first()
        val intervalLength = userSettingsStore.getIntervalLength().first()
        val intervalState = IntervalTimerState(
            windowStartMillis = userSettingsStore.getIntervalWindowStart().first(),
            usageMillis = userSettingsStore.getIntervalUsage().first(),
        )

        Timber.i(
            "init: option=%s, timeLimit=%d, intervalLength=%d, intervalState=%s",
            blockOption,
            timeLimit,
            intervalLength,
            intervalState,
        )
        handler = createHandlerForConfig(blockOption, timeLimit, intervalLength, intervalState)
        sessionTracker.checkDailyReset()
    }

    /**
     * Creates the appropriate [BlockOptionHandler] based on configuration.
     *
     * @param blockOption The blocking option type.
     * @param timeLimit Daily or interval time limit in milliseconds.
     * @param intervalLength Interval duration in milliseconds (for IntervalTimer).
     * @return A handler matching the configuration.
     */
    private fun createHandlerForConfig(
        blockOption: BlockOption,
        timeLimit: Long,
        intervalLength: Long,
        intervalState: IntervalTimerState,
    ): BlockOptionHandler = when (blockOption) {
        BlockOption.BlockAll -> BlockAllBlockHandler().also { Timber.d("Using BlockAll handler") }
        BlockOption.DailyLimit -> DayLimitBlockHandler(timeLimit).also { Timber.d("Using DayLimit handler (limit=%d)", timeLimit) }
        BlockOption.IntervalTimer ->
            IntervalTimerBlockHandler(
                allowanceMillis = timeLimit,
                intervalLengthMillis = intervalLength,
                initialState = intervalState,
                onStateChanged = { state ->
                    persistenceScope.launch {
                        userSettingsStore.updateIntervalState(state.windowStartMillis, state.usageMillis)
                    }
                },
            ).also { Timber.d("Using IntervalTimer handler (limit=%d, interval=%d)", timeLimit, intervalLength) }

        BlockOption.NothingSelected -> NoBlockHandler().also { Timber.d("Using NothingSelected handler") }
    }

    /**
     * Called when entering blocked content.
     * Checks usage and decides if the content should be immediately blocked.
     *
     * @return `true` if blocking is required immediately.
     */
    override suspend fun onEnterBlockedContent(): Boolean {
        sessionTracker.checkDailyReset()
        val currentDailyUsage = sessionTracker.getDailyUsage()
        val shouldBlock = handler.onEnterContent(currentDailyUsage)
        Timber.d("onEnterBlockedContent: daily=%d -> shouldBlock=%s", currentDailyUsage, shouldBlock)
        return shouldBlock
    }

    /**
     * Called periodically to check if blocking should occur.
     * For example, to check if a time limit has been reached during the session.
     *
     * @param elapsedTime Time elapsed in the current session (milliseconds).
     * @return [com.scrolless.app.core.model.BlockingResult] indicating whether to block, continue, or check later.
     */
    override suspend fun onPeriodicCheck(elapsedTime: Long): BlockingResult {
        val currentDailyUsage = sessionTracker.getDailyUsage()
        val result = handler.onPeriodicCheck(currentDailyUsage, elapsedTime)
        Timber.v("onPeriodicCheck: daily=%d, elapsed=%d -> result=%s", currentDailyUsage, elapsedTime, result)
        return result
    }

    /**
     * Called when exiting blocked content.
     *
     * @param sessionTime Duration of the session in milliseconds.
     */
    override fun onExitBlockedContent(sessionTime: Long) {
        Timber.d("onExitBlockedContent: session=%d", sessionTime)
        handler.onExitContent(sessionTime)
    }
}
