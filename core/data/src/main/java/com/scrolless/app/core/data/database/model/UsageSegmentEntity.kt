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
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.UsageSegment
import java.time.LocalDateTime

@Entity(
    tableName = "usage_segments",
)
@Immutable
data class UsageSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val app: BlockableApp,
    val durationMillis: Long,
    val startDateTime: LocalDateTime,
)

fun UsageSegmentEntity.toUsageSegment(): UsageSegment {
    return UsageSegment(
        app = this.app,
        durationMillis = this.durationMillis,
        startDateTime = this.startDateTime,
    )
}
