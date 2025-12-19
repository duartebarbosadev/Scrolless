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

import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockingResult

/**
 * Interface for managing blocking logic for restricted content.
 */
interface BlockingManager {

    /**
     * Initializes the manager with a block option configuration.
     *
     * @param blockOption The blocking option to apply.
     */
    suspend fun init(blockOption: BlockOption)

    /**
     * Called when entering blocked content.
     * Checks usage and decides if the content should be immediately blocked.
     *
     * @return `true` if blocking is required immediately.
     */
    suspend fun onEnterBlockedContent(): Boolean

    /**
     * Called periodically to check if blocking should occur.
     * For example, to check if a time limit has been reached during the session.
     *
     * @param elapsedTime Time elapsed in the current session (milliseconds).
     * @return [com.scrolless.app.core.model.BlockingResult] indicating whether to block, continue, or check later.
     */
    suspend fun onPeriodicCheck(elapsedTime: Long): BlockingResult

    /**
     * Called when exiting blocked content.
     *
     * @param sessionTime Duration of the session in milliseconds.
     */
    fun onExitBlockedContent(sessionTime: Long)
}
