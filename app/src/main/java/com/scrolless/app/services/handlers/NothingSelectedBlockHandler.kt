package com.scrolless.app.services.handlers

import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.services.BlockOptionHandler

/**
 * No blocking or tracking is performed. The user is free to use the content without restrictions.
 */
class NothingSelectedBlockHandler :
    BlockOptionHandler { // TODO IS this necessary? maybe just remove it and early stop if its nothing selected
    override fun onEnterContent(usageTracker: UsageTracker): Boolean {
        // Do not block
        return false
    }

    override fun onPeriodicCheck(usageTracker: UsageTracker, elapsedTime: Long): Boolean {
        // No blocking on periodic check
        return false
    }

    override fun onExitContent(usageTracker: UsageTracker, sessionTime: Long) {
        // No usage tracking or blocking
    }
}