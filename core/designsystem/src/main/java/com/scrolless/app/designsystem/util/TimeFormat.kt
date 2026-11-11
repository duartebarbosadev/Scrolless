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
package com.scrolless.app.designsystem.util

import android.content.Context
import android.text.format.DateUtils

/**
 * Formats this number of minutes into a compact hour/minute label like "1h 5m" or "45m".
 * Negative values are coerced to 0.
 */
fun Int.formatMinutes(): String {
    val total = this.coerceAtLeast(0)
    val hours = total / 60
    val remainingMinutes = total % 60
    return buildString {
        if (hours > 0) {
            append(hours)
            append("h")
        }
        if (remainingMinutes > 0 || hours == 0) {
            if (isNotEmpty()) append(" ")
            append(remainingMinutes)
            append("m")
        }
    }
}

/** Formats [millis] as a time, adding the weekday when it isn't today. */
fun formatTimeWithDay(context: Context, millis: Long): String {
    val flags = if (DateUtils.isToday(millis)) {
        DateUtils.FORMAT_SHOW_TIME
    } else {
        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
    }
    return DateUtils.formatDateTime(context, millis, flags)
}
