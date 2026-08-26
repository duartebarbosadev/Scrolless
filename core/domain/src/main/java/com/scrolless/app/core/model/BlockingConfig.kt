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
package com.scrolless.app.core.model

import androidx.compose.runtime.Immutable

/** The state of blocking right now, in one snapshot. */
@Immutable
data class BlockingConfig(
    val activeOption: BlockOption = BlockOption.NothingSelected,
    val settings: BlockingSettings = BlockingSettings(),
    val intervalUsage: IntervalUsage = IntervalUsage.NOT_STARTED,
)

/**
 * What every mode is configured with, kept even while another mode is selected so switching back
 * restores it. `0` means the mode was never configured.
 */
@Immutable
data class BlockingSettings(val dailyLimitMillis: Long = 0L, val intervalAllowanceMillis: Long = 0L, val intervalLengthMillis: Long = 0L)
