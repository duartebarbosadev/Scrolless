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
package com.scrolless.app.core.data.repository

import com.scrolless.app.core.data.database.dao.SessionSegmentDao
import com.scrolless.app.core.data.database.model.SessionSegmentEntity
import com.scrolless.app.core.data.database.model.toSessionSegment
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.repository.SessionSegmentStore
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SessionSegmentStoreImpl @Inject constructor(private val sessionSegmentDao: SessionSegmentDao) : SessionSegmentStore {

    override fun getSessionSegment(date: LocalDate): Flow<List<SessionSegment>> {
        val nextDate = date.plusDays(1)
        return sessionSegmentDao.getSessionSegment(date, nextDate).map { entities ->
            entities.map { it.toSessionSegment() }
        }
    }

    override suspend fun addSessionSegment(sessionSegment: SessionSegment): Long {
        val entity = SessionSegmentEntity(
            app = sessionSegment.app,
            durationMillis = sessionSegment.durationMillis,
            startDateTime = sessionSegment.startDateTime,
        )
        return sessionSegmentDao.insert(entity)
    }

    override suspend fun updateSessionSegmentDuration(lastSessionId: Long, sessionTime: Long) {
        sessionSegmentDao.updateDuration(lastSessionId, sessionTime)
    }
}
