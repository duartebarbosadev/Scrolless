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
package com.scrolless.app.core.data.database.model

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Entity for tracking usage per app per day.
 * Composite primary key of app name and date allows tracking historical data.
 */
@Entity(
    tableName = "app_usage",
    primaryKeys = ["app_name", "date"],
    indices = [
        Index("app_name"),
        Index("date"),
    ],
)
@Immutable
data class AppUsage(
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "date") val date: String, // ISO date string (YYYY-MM-DD)
    @ColumnInfo(name = "usage_millis") val usageMillis: Long = 0L,
)
