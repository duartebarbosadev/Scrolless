package com.scrolless.app.services.handlers

import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.services.BlockOptionHandler

/**
 * Immediately blocks any blocked content without considering time limits.
 */
class BlockAllBlockHandler : BlockOptionHandler {
    override fun onEnterContent(usageTracker: UsageTracker): Boolean {
        // Always block immediately.
        return true
    }

    override fun onPeriodicCheck(usageTracker: UsageTracker, elapsedTime: Long): Boolean {
        // Already blocked on enter, so no periodic check needed.
        return false
    }

    override fun onExitContent(usageTracker: UsageTracker, sessionTime: Long) {
        // No usage tracking needed for BlockAll.
    }
}