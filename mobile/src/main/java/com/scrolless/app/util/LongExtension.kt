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
package com.scrolless.app.util

import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

// Define named constants for use in your functions.
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60


private const val MILLISECONDS_PER_SECOND = 1000
private const val MILLISECONDS_PER_MINUTE = MILLISECONDS_PER_SECOND * SECONDS_PER_MINUTE // 60000
private const val MILLISECONDS_PER_HOUR = MILLISECONDS_PER_MINUTE * MINUTES_PER_HOUR // 3600000


/**
 * Formats a duration given in milliseconds into a compact time representation.
 *
 * The conversion uses hours, minutes, and seconds markers ("h", "m", "s"). Components that
 * evaluate to zero are omitted, except when all components are zero, then "0s" is appended.
 *
 * For example:
 * - An input of 3661000 will result in "1h1m1s".
 *
 * @receiver The duration in milliseconds.
 * @return A compact string representation of the duration.
 */
fun Long.formatTime(): String {
    val hours = this / MILLISECONDS_PER_HOUR
    val minutes = (this % MILLISECONDS_PER_HOUR) / MILLISECONDS_PER_MINUTE
    val seconds = (this % MILLISECONDS_PER_MINUTE) / MILLISECONDS_PER_SECOND

    return buildString {
        if (hours > 0) append("${hours}h")
        if (minutes > 0) append("${minutes}m")
        if (seconds > 0 || isEmpty()) append("${seconds}s")
    }
}

/**
 * Converts a duration given in milliseconds into a readable time string.
 *
 * If the duration contains one or more hours, the output format is "hh:mm:ss", otherwise it is
 * "mm:ss".
 *
 * For example:
 * - An input of 3661000 will yield "01:01:01".
 * - An input of 61000 will yield "01:01".
 *
 * @receiver The duration in milliseconds.
 * @return A time string formatted to "hh:mm:ss" or "mm:ss" based on the duration.
 */
fun Long.formatAsTime(): String {
    val duration = this.milliseconds
    val hours = duration.inWholeHours
    val minutes = (duration - hours.hours).inWholeMinutes
    val seconds = (duration - hours.hours - minutes.minutes).inWholeSeconds

    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
