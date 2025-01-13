/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.room.converter

import androidx.room.TypeConverter
import com.scrolless.framework.extensions.fromJsonList
import com.scrolless.framework.extensions.toJson

class IntConverter {
    @TypeConverter
    fun fromListOfInts(listOfInt: List<Int>?): String = listOfInt.toJson()

    @TypeConverter
    fun toListOfInts(stringValue: String): List<Int>? = stringValue.fromJsonList<Int>()
}
