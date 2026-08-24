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
package com.scrolless.app.core.repository

import com.scrolless.app.core.model.BlockOption
import kotlinx.coroutines.flow.Flow

/**
 * Stores the selected blocking option and the current interval window.
 */
interface BlockingConfigRepository {

    fun observeActiveOption(): Flow<BlockOption>

    /**
     * Used by the Home screen to restore interval settings while another option is selected.
     */
    fun observeSavedIntervalTimer(): Flow<BlockOption.IntervalTimer>

    /**
     * Selects [option] without clearing settings saved for other options.
     */
    suspend fun setActiveOption(option: BlockOption)

    suspend fun configureDailyLimit(limitMillis: Long)

    /**
     * Editing an active interval keeps its start time and watched time. For example, changing a
     * 30-minute window to 60 minutes expands the same window instead of resetting it. Configuring
     * interval mode while another option is selected starts a new window.
     */
    suspend fun configureIntervalTimer(allowanceMillis: Long, intervalLengthMillis: Long)

    suspend fun updateIntervalWindow(windowStartMillis: Long, usageMillis: Long)
}
