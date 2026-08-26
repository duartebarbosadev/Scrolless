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
 * The blocking mode the user selected. The values it needs to enforce live in [BlockingSettings].
 */
enum class BlockOption {

    /** Closes blocked content as soon as it is opened. */
    BlockAll,

    /** Allows [BlockingSettings.dailyLimitMillis] of blocked content per day. */
    DailyLimit,

    /** Allows [BlockingSettings.intervalAllowanceMillis] per [BlockingSettings.intervalLengthMillis]. */
    IntervalTimer,

    /** Nothing is blocked. */
    NothingSelected,
}
