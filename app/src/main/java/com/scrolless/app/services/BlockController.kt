/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.services

import com.scrolless.app.features.home.BlockConfig
import com.scrolless.app.features.home.BlockOption
import com.scrolless.app.provider.AppProvider
import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.services.handlers.BlockAllBlockHandler
import com.scrolless.app.services.handlers.DayLimitBlockHandler
import com.scrolless.app.services.handlers.IntervalTimerBlockHandler
import com.scrolless.app.services.handlers.NothingSelectedBlockHandler
import com.scrolless.app.services.handlers.TemporaryUnblockBlockHandler

class BlockController(
    private val appProvider: AppProvider,
    private val usageTracker: UsageTracker
) {

    private var handler: BlockOptionHandler = createHandlerForConfig(appProvider.blockConfig)

    /**
     * Updates the block option, creating a new handler and performing any necessary resets.
     */
    fun setBlockConfigOption(newOption: BlockConfig) {
        handler = createHandlerForConfig(newOption)
        initializeForOption() // Re-run initialization logic if needed
    }

    private fun createHandlerForConfig(config: BlockConfig): BlockOptionHandler = when (config.blockOption) {
        BlockOption.BlockAll -> BlockAllBlockHandler()
        BlockOption.DayLimit -> DayLimitBlockHandler(config.timeLimit)
        BlockOption.IntervalTimer -> IntervalTimerBlockHandler(
            config.timeLimit,
            config.intervalLength,
        )

        BlockOption.TemporaryUnblock -> TemporaryUnblockBlockHandler(config.timeLimit)
        BlockOption.NothingSelected -> NothingSelectedBlockHandler()
    }

    fun onEnterBlockedContent(): Boolean {
        usageTracker.checkDailyReset()

        // If handler says block now, return true
        return handler.onEnterContent(usageTracker)
    }

    fun onPeriodicCheck(elapsedTime: Long): Boolean = handler.onPeriodicCheck(usageTracker, elapsedTime)

    fun onExitBlockedContent(sessionTime: Long) {
        handler.onExitContent(usageTracker, sessionTime)
    }

    fun initializeForOption() {
        usageTracker.load() // Make sure we have the latest data
        usageTracker.checkDailyReset() // In case day changed
        if (appProvider.blockConfig.blockOption == BlockOption.IntervalTimer) {
            usageTracker.checkDailyReset() // or do any additional interval logic here
        }
    }
}
