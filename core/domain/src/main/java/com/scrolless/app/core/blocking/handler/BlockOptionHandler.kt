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

/**
 * Strategy interface for different blocking behaviors.
 * Handlers receive usage data as parameters instead of accessing UsageTracker directly.
 */
interface BlockOptionHandler {

    /**
     * Called when user enters content.
     * Should return true if should immediately block, false otherwise.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     */
    fun onEnterContent(currentDailyUsage: Long): Boolean

    /**
     * Called periodically while user remains in blocked content.
     * Should return the result of the blocking check.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @param elapsedTime Time elapsed in current session in milliseconds.
     *
     * @return [BlockingResult] indicating whether to block, continue, or check later.
     */
    fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult

    /**
     * Called when user exits blocked content to finalize any usage calculations.
     *
     * @param sessionTime Duration of the session in milliseconds.
     */
    fun onExitContent(sessionTime: Long)
}
