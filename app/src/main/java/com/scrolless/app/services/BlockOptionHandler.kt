package com.scrolless.app.services

import com.scrolless.app.provider.UsageTracker

interface BlockOptionHandler {

    /**
     * Called when user enters content.
     * Should return true if should immediately block, false otherwise.
     */
    fun onEnterContent(usageTracker: UsageTracker): Boolean

    /**
     * Called periodically while user remains in blocked content.
     * Should return true if should block now, false otherwise.
     */
    fun onPeriodicCheck(
        usageTracker: UsageTracker,
        elapsedTime: Long
    ): Boolean // TODO (maybe?)REMOVE Make it the responsibility of the handler to decide if to block

    /**
     * Called when user exits blocked content to finalize any usage calculations.
     */
    fun onExitContent(usageTracker: UsageTracker, sessionTime: Long)
}
