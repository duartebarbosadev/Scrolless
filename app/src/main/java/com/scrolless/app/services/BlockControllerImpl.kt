/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.services

import com.scrolless.app.features.home.BlockConfig
import com.scrolless.app.features.home.BlockOption
import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.services.handlers.BlockAllBlockHandler
import com.scrolless.app.services.handlers.DayLimitBlockHandler
import com.scrolless.app.services.handlers.IntervalTimerBlockHandler
import com.scrolless.app.services.handlers.NothingSelectedBlockHandler
import com.scrolless.app.services.handlers.TemporaryUnblockBlockHandler

/**
 * Implementation of [BlockController] that uses a [BlockOptionHandler] to handle blocking logic.
 *
 * @property usageTracker Tracks usage data and handles daily resets.
 */
class BlockControllerImpl(
    private val usageTracker: UsageTracker
) : BlockController {

    private lateinit var handler: BlockOptionHandler

    /**
     * Initializes the controller with a configuration.
     * Sets up the correct handler and refreshes usage data.
     *
     * @param blockConfig Configuration for blocking options.
     */
    override fun init(blockConfig: BlockConfig) {
        handler = createHandlerForConfig(blockConfig)
        usageTracker.load() // Make sure we have the latest data
        usageTracker.checkDailyReset() // In case day changed
        if (blockConfig.blockOption == BlockOption.IntervalTimer) {
            usageTracker.checkDailyReset() // or do any additional interval logic here
        }
    }

    /**
     * Chooses a [BlockOptionHandler] based on the [BlockConfig].
     *
     * @param config The blocking configuration.
     * @return A handler matching the configuration.
     */
    private fun createHandlerForConfig(config: BlockConfig): BlockOptionHandler =
        when (config.blockOption) {
            BlockOption.BlockAll -> BlockAllBlockHandler()
            BlockOption.DayLimit -> DayLimitBlockHandler(config.timeLimit)
            BlockOption.IntervalTimer -> IntervalTimerBlockHandler(
                config.timeLimit,
                config.intervalLength,
            )

            BlockOption.TemporaryUnblock -> TemporaryUnblockBlockHandler(config.timeLimit)
            BlockOption.NothingSelected -> NothingSelectedBlockHandler()
        }

    /**
     * Called when entering brain rot content.
     * Checks usage and decides if the content should be immediately blocked.
     *
     * @return `true` if blocking is required.
     */
    override fun onEnterBlockedContent(): Boolean {
        usageTracker.checkDailyReset()

        // If handler says block now, return true
        return handler.onEnterContent(usageTracker)
    }

    /**
     * Called periodically to perform any checks on the handler
     *  for example to check if any time limit has been reached.
     *
     * @param elapsedTime Time elapsed since the last check.
     * @return `true` if the content should remain blocked.
     */
    override fun onPeriodicCheck(elapsedTime: Long): Boolean =
        handler.onPeriodicCheck(usageTracker, elapsedTime)

    /**
     * Called when exiting brain rot content.
     *
     * @param sessionTime Duration of the session.
     */
    override fun onExitBlockedContent(sessionTime: Long) {
        handler.onExitContent(usageTracker, sessionTime)
    }
}
