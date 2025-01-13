/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

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
