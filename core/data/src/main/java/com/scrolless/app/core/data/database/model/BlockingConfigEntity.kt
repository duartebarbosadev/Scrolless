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
package com.scrolless.app.core.data.database.model

import androidx.room.ColumnInfo
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.IntervalTimerWindow

/**
 * Blocking values read together before they are converted to one [BlockOption].
 */
data class BlockingConfigEntity(
    @ColumnInfo(name = "active_block_option") val activeOption: BlockOptionType,
    @ColumnInfo(name = "time_limit") val limitMillis: Long,
    @ColumnInfo(name = "interval_length") val intervalLengthMillis: Long,
    @ColumnInfo(name = "interval_window_start_at") val intervalWindowStartMillis: Long,
    @ColumnInfo(name = "interval_usage") val intervalUsageMillis: Long,
)

fun BlockingConfigEntity.toActiveOption(): BlockOption = when (activeOption) {
    BlockOptionType.BlockAll -> BlockOption.BlockAll
    BlockOptionType.DailyLimit -> savedDailyLimit()
    BlockOptionType.IntervalTimer -> savedIntervalTimer()
    BlockOptionType.NothingSelected -> BlockOption.NothingSelected
}

fun BlockingConfigEntity.savedDailyLimit(): BlockOption.DailyLimit = BlockOption.DailyLimit(limitMillis = limitMillis.coerceAtLeast(0L))

fun BlockingConfigEntity.savedIntervalTimer(): BlockOption.IntervalTimer = BlockOption.IntervalTimer(
    allowanceMillis = limitMillis.coerceAtLeast(0L),
    window = IntervalTimerWindow(
        startMillis = intervalWindowStartMillis,
        lengthMillis = intervalLengthMillis,
        usageMillis = intervalUsageMillis.coerceAtLeast(0L),
    ),
)
