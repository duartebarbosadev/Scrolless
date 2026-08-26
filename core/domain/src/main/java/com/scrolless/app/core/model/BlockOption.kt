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

/**
 * The blocking mode the user selected, with the settings needed to enforce it.
 *
 * This only describes configuration. How much the user already watched lives in
 * [IntervalUsageWindow] and in the session tracker, so selecting a mode never carries usage.
 */
sealed interface BlockOption {

    /** Closes blocked content as soon as it is opened. */
    data object BlockAll : BlockOption

    /** Allows [limitMillis] of blocked content per day. */
    data class DailyLimit(val limitMillis: Long) : BlockOption

    /** Allows [allowanceMillis] of blocked content per [intervalLengthMillis] window. */
    data class IntervalTimer(val allowanceMillis: Long, val intervalLengthMillis: Long) : BlockOption

    /** Nothing is blocked. */
    data object NothingSelected : BlockOption
}
