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
package com.scrolless.app.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.scrolless.app.core.data.database.model.SessionSegmentEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * [androidx.room.Room] DAO for [SessionSegmentEntity] related operations.
 */
@Dao
abstract class SessionSegmentDao : BaseDao<SessionSegmentEntity> {

    @Query("SELECT * FROM session_segments WHERE startDateTime >= :date AND startDateTime < :datePlusOneDay")
    abstract fun getUsageSegment(date: LocalDate, datePlusOneDay : LocalDate?): Flow<List<SessionSegmentEntity>>
}
