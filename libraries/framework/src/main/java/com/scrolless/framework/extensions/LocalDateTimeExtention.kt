/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun LocalDateTime.isBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime
): Boolean = this in startDate..endDate

fun LocalDateTime.formatToDefaultPattern(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return this.format(formatter)
}

fun LocalDateTime.formatToDateTimeWithSec(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return this.format(formatter)
}

fun LocalDateTime.convertToMillis(): Long {
    val instant = this.atZone(ZoneId.systemDefault()).toInstant()
    return instant.toEpochMilli()
}
