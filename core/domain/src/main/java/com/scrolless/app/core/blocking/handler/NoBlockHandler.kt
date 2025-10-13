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
 * No blocking or tracking is performed. The user is free to use the content without restrictions.
 */
class NoBlockHandler : BlockOptionHandler {
    /**
     * Never blocks on entry.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @return false, never block.
     */
    override fun onEnterContent(currentDailyUsage: Long): Boolean {
        Timber.v("NothingSelected.onEnter: daily=%d -> allow", currentDailyUsage)
        // Do not block
        return false
    }

    /**
     * Never blocks on periodic check.
     *
     * @param currentDailyUsage Current daily usage in milliseconds.
     * @param elapsedTime Time elapsed in current session in milliseconds.
     * @return [BlockingResult.Continue], never block.
     */
    override fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        // No blocking on periodic check
        Timber.v("NothingSelected.onPeriodic: daily=%d, elapsed=%d -> allow", currentDailyUsage, elapsedTime)
        return BlockingResult.Continue
    }

    /**
     * No usage tracking or blocking.
     *
     * @param sessionTime Duration of the session in milliseconds.
     */
    override fun onExitContent(sessionTime: Long) {
        Timber.v("NothingSelected.onExit: session=%d", sessionTime)
        // No usage tracking or blocking
    }
}
