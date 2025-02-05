/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import java.time.Duration
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Long.formatDurationFromSeconds(): String {
    val seconds = this % 60
    val minutes = (this / 60) % 60
    val hours = (this / (60 * 60)) % 24
    val days = this / (60 * 60 * 24)

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

fun Long.formatTime(): String {
    val hours = this / (1000 * 60 * 60)
    val minutes = (this % (1000 * 60 * 60)) / (1000 * 60)
    val seconds = (this % (1000 * 60)) / 1000

    return buildString {
        if (hours > 0) append("${hours}h")
        if (minutes > 0) append("${minutes}m")
        if (seconds > 0 || isEmpty()) append("${seconds}s")
    }
}


fun Long.getReadableTime(): String {
    val duration = this.milliseconds
    val hours = duration.inWholeHours
    val minutes = (duration - hours.hours).inWholeMinutes
    val seconds = (duration - hours.hours - minutes.minutes).inWholeSeconds

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}


fun Int.formatDurationFromSeconds(): String {
    val seconds = this % 60
    val minutes = (this / 60) % 60
    val hours = (this / (60 * 60)) % 24
    val days = this / (60 * 60 * 24)

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
