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
package com.scrolless.app.core.blocking.handler

import com.scrolless.app.core.model.BlockingResult
import timber.log.Timber

/**
 * Allows a single temporary "unblock" session up to `timeLimit`.
 * Once the user leaves blocked content, the unblock is considered used
 * and cannot be reused. If the user attempts to re-enter blocked content
 * after using the temporary unblock, they are immediately blocked.
 */
class TemporaryUnblockBlockHandler(private val timeLimit: Long) : BlockOptionHandler {
    /**
     * Placeholder implementation: always blocks on entry.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @return true, block immediately.
     */
    override fun onEnterContent(currentDailyUsage: Long): Boolean {
        Timber.w("TemporaryUnblock.onEnter: placeholder implementation -> block (limit=%d)", timeLimit)
        return true
    }

    /**
     * Placeholder implementation: always blocks on periodic check.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @param elapsedTime Time elapsed in current session in milliseconds.
     * @return [BlockingResult.BlockNow], block now.
     */
    override fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        Timber.w("TemporaryUnblock.onPeriodic: placeholder implementation -> block")
        return BlockingResult.BlockNow
    }

    /**
     * Placeholder implementation.
     *
     * @param sessionTime Duration of the session in milliseconds.
     */
    override fun onExitContent(sessionTime: Long) {
        Timber.w("TemporaryUnblock.onExit: placeholder implementation")
        return
    }
}
