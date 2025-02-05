/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.services

import com.scrolless.app.features.home.BlockConfig

interface BlockController {

    /**
     * Initializes the controller with a configuration.
     *
     * @param blockConfig Configuration for blocking options.
     */
    fun init(blockConfig: BlockConfig)

    /**
     * Called when entering brain rot content.
     *
     * @return `true` if blocking is required.
     */
    fun onEnterBlockedContent(): Boolean

    /**
     * Called periodically to perform any checks on the handler
     *  for example to check if any time limit has been reached.
     *
     * @param elapsedTime Time elapsed since the last check.
     * @return `true` if the content should remain blocked.
     */
    fun onPeriodicCheck(elapsedTime: Long): Boolean

    /**
     * Called when exiting brain rot content.
     *
     * @param sessionTime Duration of the session.
     */
    fun onExitBlockedContent(sessionTime: Long)
}
