/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.room.converter

import androidx.room.TypeConverter
import java.time.Duration

class DurationConverter {
    @TypeConverter
    fun fromDuration(duration: Duration?): Long? = duration?.toMillis()

    @TypeConverter
    fun toDuration(millis: Long?): Duration? = millis?.let { Duration.ofMillis(it) }
}
