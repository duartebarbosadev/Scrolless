/*
 * Copyright (C) 2026 Scrolless
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

import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.data.database.dao.SessionSegmentDao
import com.scrolless.app.core.data.database.model.SessionSegmentEntity
import com.scrolless.app.core.data.database.model.toSessionSegment
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.model.usage.DailyUsageTotal
import com.scrolless.app.core.model.usage.calculateDailyTotals
import com.scrolless.app.core.repository.SessionSegmentStore
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SessionSegmentStoreImpl @Inject constructor(
    private val timeProvider: TimeProvider,
    private val sessionSegmentDao: SessionSegmentDao,
) : SessionSegmentStore {

    override fun observeTotalDuration(date: LocalDate): Flow<Long> = sessionSegmentDao.getTotalDuration(date, date.plusDays(1))

    override suspend fun getTodayTotalDurationSnapshot(): Long {
        val today = timeProvider.localDateNow()
        return sessionSegmentDao.getTotalDurationSnapshot(today, today.plusDays(1))
    }

    override fun getListSessionSegments(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<SessionSegment>> {
        val endDateExclusive = endDateInclusive.plusDays(1)
        return sessionSegmentDao.getSessionSegments(startDate, endDateExclusive).map { entities ->
            entities.map { it.toSessionSegment() }
        }
    }

    override fun getDailyUsageTotals(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<DailyUsageTotal>> =
        getListSessionSegments(startDate, endDateInclusive).map { sessionSegments ->
            sessionSegments.calculateDailyTotals(
                startDate = startDate,
                endDateInclusive = endDateInclusive,
            )
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

    override suspend fun replaceSessionSegmentsForDate(date: LocalDate, sessionSegments: List<SessionSegment>) {
        val nextDate = date.plusDays(1)
        val entities = sessionSegments.map { sessionSegment ->
            SessionSegmentEntity(
                app = sessionSegment.app,
                durationMillis = sessionSegment.durationMillis.coerceAtLeast(0L),
                startDateTime = sessionSegment.startDateTime,
            )
        }
        sessionSegmentDao.replaceSessionSegments(date = date, datePlusOneDay = nextDate, entities = entities)
    }
}
