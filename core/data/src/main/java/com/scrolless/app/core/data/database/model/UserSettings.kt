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
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_settings",
    indices = [
        Index("id", unique = true),
    ],
)
@Immutable
data class UserSettings(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = 1, // Single row for settings
    @ColumnInfo(name = "active_block_option") val activeBlockOption: BlockOption,
    @ColumnInfo(name = "time_limit") val timeLimit: Long,
    @ColumnInfo(name = "interval_length") val intervalLength: Long,
    @ColumnInfo(name = "interval_window_start_at") val intervalWindowStartAt: Long = 0L,
    @ColumnInfo(name = "interval_usage") val intervalUsage: Long = 0L,
    @ColumnInfo(name = "timer_overlay_enabled") val timerOverlayEnabled: Boolean,
    @ColumnInfo(name = "last_reset_day") val lastResetDay: String, // Store as ISO date string
    @ColumnInfo(name = "total_daily_usage") val totalDailyUsage: Long,
    @ColumnInfo(name = "timer_overlay_x") val timerOverlayX: Int = 0,
    @ColumnInfo(name = "timer_overlay_y") val timerOverlayY: Int = 100,
    @ColumnInfo(name = "waiting_for_accessibility") val waitingForAccessibility: Boolean = false,
    @ColumnInfo(name = "has_seen_accessibility_explainer") val hasSeenAccessibilityExplainer: Boolean = false,
    @ColumnInfo(name = "pause_until_at") val pauseUntilAt: Long = 0L,
)
