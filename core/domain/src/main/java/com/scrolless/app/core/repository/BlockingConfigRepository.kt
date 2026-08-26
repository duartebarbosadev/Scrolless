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
import com.scrolless.app.core.model.BlockingConfig
import com.scrolless.app.core.model.UsageWindow
import kotlinx.coroutines.flow.Flow

/**
 * Single owner of the blocking settings and of the interval usage they produce.
 */
interface BlockingConfigRepository {

    fun observeConfig(): Flow<BlockingConfig>

    /** Emits only when the selected mode or its settings change, never when usage changes. */
    fun observeActiveOption(): Flow<BlockOption>

    suspend fun getConfig(): BlockingConfig

    /** Selects [option] without touching the settings or usage of the other modes. */
    suspend fun setActiveOption(option: BlockOption)

    /** Saves the daily limit and selects daily limit mode. */
    suspend fun configureDailyLimit(limitMillis: Long)

    /** Saves the interval settings and selects interval mode, keeping the running window. */
    suspend fun configureIntervalTimer(allowanceMillis: Long, intervalLengthMillis: Long)

    /**
     * Returns the interval window containing [nowMillis], advancing and saving it when the
     * previous window has ended.
     */
    suspend fun getCurrentIntervalWindow(nowMillis: Long): UsageWindow

    /**
     * Adds one finished viewing session to the interval usage and returns the updated window.
     *
     * Callers pass timestamps rather than a window, so window rollover always uses the settings
     * that are saved right now.
     */
    suspend fun recordIntervalUsage(sessionStartMillis: Long, sessionEndMillis: Long): UsageWindow
}
