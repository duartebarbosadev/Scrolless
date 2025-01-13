/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.room.converter

import androidx.room.TypeConverter
import com.scrolless.framework.extensions.fromJson
import com.scrolless.framework.extensions.toJson

class StringConverter {
    @TypeConverter
    fun toListOfStrings(stringValue: String): List<String>? = stringValue.fromJson()

    @TypeConverter
    fun fromListOfStrings(listOfString: List<String>?): String = listOfString.toJson()
}
