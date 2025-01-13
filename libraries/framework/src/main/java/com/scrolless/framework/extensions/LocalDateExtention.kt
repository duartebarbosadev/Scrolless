/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun LocalDate.isBetween(
    startDate: LocalDate,
    endDate: LocalDate
): Boolean = this in startDate..endDate

fun LocalDate.convertToMillis(): Long = this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun LocalDate.formatToDefaultPattern(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)

fun LocalDate.getReadableTitle(): String {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
    val dayFormatter = DateTimeFormatter.ofPattern("d", Locale.ENGLISH)
    val dayOfWeek = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    val month = format(monthFormatter)
    val day = format(dayFormatter)

    if (year == LocalDate.now().year) {
        return "$month $day ($dayOfWeek)"
    }
    return "$year, $month $day ($dayOfWeek)"
}

fun Long.fromTimeMillis(): LocalDate =
    Instant
        .ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
