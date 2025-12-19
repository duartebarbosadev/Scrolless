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
package com.scrolless.app.core.repository

import com.scrolless.app.core.model.BlockableApp

interface UsageTracker {

    /**
     * Get the current total daily usage in milliseconds.
     */
    fun getDailyUsage(): Long

    /**
     * Get usage for a specific app in milliseconds.
     */
    fun getAppDailyUsage(app: BlockableApp): Long

    /**
     * Add session time to daily usage and persist immediately.
     * Also updates per-app usage if app is provided.
     * Thread-safe and non-blocking.
     *
     * @param sessionTime Duration of the session in milliseconds
     * @param app The app the session was on (null for legacy/unknown)
     */
    suspend fun addToDailyUsage(sessionTime: Long, app: BlockableApp? = null)

    /**
     * Check if a daily reset is needed and perform it if necessary.
     * Safe to call multiple times - only resets once per day.
     */
    suspend fun checkDailyReset()
}
