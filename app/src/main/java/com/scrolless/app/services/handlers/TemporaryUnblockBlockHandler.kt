package com.scrolless.app.services.handlers

import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.services.BlockOptionHandler


/**
 * Allows a single temporary "unblock" session up to `timeLimit`.
 * Once the user leaves blocked content, the unblock is considered used
 * and cannot be reused. If the user attempts to re-enter blocked content
 * after using the temporary unblock, they are immediately blocked.
 */
class TemporaryUnblockBlockHandler(
    private val timeLimit: Long
) : BlockOptionHandler {

    // TODO Implement temporary unblock, after time is up, go back to the original handler

    override fun onEnterContent(usageTracker: UsageTracker): Boolean {
        // If temporary unblock is already used, block immediately

        return false
    }

    override fun onPeriodicCheck(usageTracker: UsageTracker, elapsedTime: Long): Boolean {
        // If the temporary unblock is not used yet but elapsed time exceeds limit, block now
        //return (!usageTracker.isTemporaryUnblockUsed() && elapsedTime >= timeLimit)
        return false
    }

    override fun onExitContent(usageTracker: UsageTracker, sessionTime: Long) {
        // After exiting, temporary unblock is considered used
        //usageTracker.markTemporaryUnblockUsed()

    }
}