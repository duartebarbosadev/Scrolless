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
package com.scrolless.app.core.repository

import com.scrolless.app.core.model.SessionSegment
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface SessionSegmentStore {
    fun getTotalDurationForToday(): Flow<Long>

    fun getListSessionSegments(date: LocalDate): Flow<List<SessionSegment>>

    suspend fun addSessionSegment(sessionSegment: SessionSegment): Long

    suspend fun updateSessionSegmentDuration(lastSessionId: Long, sessionTime: Long)
}
