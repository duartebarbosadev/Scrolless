package com.scrolless.app.services.handlers

import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.services.BlockOptionHandler

class DayLimitBlockHandler(private val timeLimit: Long) : BlockOptionHandler {
    override fun onEnterContent(usageTracker: UsageTracker): Boolean {
        // If already exceeded, block immediately
        return usageTracker.getDailyUsage() >= timeLimit
    }

    override fun onPeriodicCheck(usageTracker: UsageTracker, elapsedTime: Long): Boolean {
        // Check if crossing daily limit
        return (usageTracker.getDailyUsage() + elapsedTime) >= timeLimit
    }

    override fun onExitContent(usageTracker: UsageTracker, sessionTime: Long) {
        usageTracker.addToDailyUsage(sessionTime)
    }
}
