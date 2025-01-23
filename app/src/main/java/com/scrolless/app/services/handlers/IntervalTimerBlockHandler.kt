/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.services.handlers

import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.services.BlockOptionHandler

/**
 * Blocks if the interval usage time plus the current session's elapsed time
 * exceed a specified time limit within the given interval.
 *
 * The `UsageTracker` handles resetting usage after interval expiration.
 */
class IntervalTimerBlockHandler(
    private val timeLimit: Long,
    private val intervalLength: Long // TODO Implement interval length
) : BlockOptionHandler {

    var usageTimeThroughInterval: Long = 0L

    override fun onEnterContent(usageTracker: UsageTracker): Boolean {
        // If already exceeded interval usage, block immediately
        return usageTimeThroughInterval >= timeLimit
    }

    override fun onPeriodicCheck(usageTracker: UsageTracker, elapsedTime: Long): Boolean {
        // Check if adding the current session's elapsed time would exceed the interval limit
        val totalIntervalUsage = usageTimeThroughInterval + elapsedTime
        return totalIntervalUsage >= timeLimit
    }

    override fun onExitContent(usageTracker: UsageTracker, sessionTime: Long) {
        // Accumulate this session's time into the interval usage
        usageTimeThroughInterval += sessionTime
    }
}
