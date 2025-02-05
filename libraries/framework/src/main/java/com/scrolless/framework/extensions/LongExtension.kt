package com.scrolless.framework.extensions

import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Define named constants for use in your functions.
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24

private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR // 3600
private const val SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY        // 86400

private const val MILLISECONDS_PER_SECOND = 1000
private const val MILLISECONDS_PER_MINUTE = MILLISECONDS_PER_SECOND * SECONDS_PER_MINUTE // 60000
private const val MILLISECONDS_PER_HOUR = MILLISECONDS_PER_MINUTE * MINUTES_PER_HOUR     // 3600000

/**
 * Formats a duration, given in seconds, into a human-readable string.
 *
 * The function converts a duration (expressed as seconds) into a formatted string that shows
 * days, hours, minutes, and seconds. Components with a zero value are omitted.
 *
 * For example:
 * - For an input of 3661, the output will be "1 hours 1 minutes 1 seconds".
 * - For an input of 86400, the output will be "1 days".
 *
 * @receiver The duration in seconds.
 * @return A formatted string representing the duration.
 */
fun Long.formatDurationFromSeconds(): String {
    val seconds = this % SECONDS_PER_MINUTE
    val minutes = (this / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR
    val hours = (this / SECONDS_PER_HOUR) % HOURS_PER_DAY
    val days = this / SECONDS_PER_DAY

    val formattedDuration = StringBuilder()

    if (days > 0) {
        formattedDuration.append("$days days ")
    }

    if (hours > 0) {
        formattedDuration.append("$hours hours ")
    }

    if (minutes > 0) {
        formattedDuration.append("$minutes minutes ")
    }

    if (seconds > 0) {
        formattedDuration.append("$seconds seconds")
    }

    return formattedDuration.toString().trim()
}

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
fun Long.getReadableTime(): String {
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

/**
 * Formats a duration, given in seconds as an integer, into a human-readable string.
 *
 * This function performs the same conversion as [Long.formatDurationFromSeconds] but accepts
 * the duration as an [Int]. It converts the given seconds into days, hours, minutes, and seconds,
 * omitting components with a zero value.
 *
 * @receiver The duration in seconds.
 * @return A formatted string representing the duration.
 */
fun Int.formatDurationFromSeconds(): String {
    val seconds = this % SECONDS_PER_MINUTE
    val minutes = (this / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR
    val hours = (this / SECONDS_PER_HOUR) % HOURS_PER_DAY
    val days = this / SECONDS_PER_DAY

    val formattedDuration = StringBuilder()

    if (days > 0) {
        formattedDuration.append("$days days")
    }

    if (hours > 0) {
        formattedDuration.append("$hours hours")
    }

    if (minutes > 0) {
        formattedDuration.append("$minutes minutes")
    }

    if (seconds > 0) {
        formattedDuration.append("$seconds seconds")
    }

    return formattedDuration.toString().trim()
}