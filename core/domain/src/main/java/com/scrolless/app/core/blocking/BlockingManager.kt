/*
 * Copyright (C) 2026 Scrolless
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
import com.scrolless.app.core.model.BlockingSettings

/**
 * Runs blocking checks during a viewing session.
 *
 * Call [init] when the blocking option changes. For each viewing session, call
 * [onEnterBlockedContent], then [onPeriodicCheck] while the session is active, and finally
 * [onExitBlockedContent].
 */
interface BlockingManager {

    /**
     * Initializes the manager with the selected mode and the settings that enforce it.
     */
    suspend fun init(option: BlockOption, settings: BlockingSettings)

    /**
     * Starts a viewing session.
     *
     * @return `true` when the content should be closed immediately.
     */
    suspend fun onEnterBlockedContent(): Boolean

    /** Checks whether covered content must remain blocked, without opening a viewing session. */
    suspend fun shouldBlockContent(): Boolean

    /**
     * Checks the active viewing session.
     *
     * @param elapsedTime Time since the session started, in milliseconds.
     */
    suspend fun onPeriodicCheck(elapsedTime: Long): BlockingResult

    /**
     * Finishes a viewing session.
     *
     * @param sessionStartMillis Time when the session started.
     * @param sessionEndMillis Time when the session ended.
     */
    suspend fun onExitBlockedContent(sessionStartMillis: Long, sessionEndMillis: Long)
}
