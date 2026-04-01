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

import com.scrolless.app.core.data.database.dao.SessionSegmentDao
import com.scrolless.app.core.data.database.model.SessionSegmentEntity
import com.scrolless.app.core.data.database.model.toSessionSegment
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.repository.SessionSegmentStore
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class SessionSegmentStoreImpl @Inject constructor(private val sessionSegmentDao: SessionSegmentDao) : SessionSegmentStore {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _totalDurationForToday = MutableStateFlow(0L)
    private val currentDay = MutableStateFlow(LocalDate.now())

    init {
        coroutineScope.launch {
            while (isActive) {
                val today = LocalDate.now()
                if (today != currentDay.value) {
                    currentDay.value = today
                }
                val zoneId = ZoneId.systemDefault()
                val nextMidnight = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val delayMillis = (nextMidnight - System.currentTimeMillis()).coerceAtLeast(1L)
                delay(delayMillis)
            }
        }

        coroutineScope.launch {
            currentDay.collectLatest { date ->
                sessionSegmentDao.getTotalDuration(date, date.plusDays(1)).collect { duration ->
                    _totalDurationForToday.value = duration
                }
            }
        }
    }
    override fun getTotalDurationForToday(): Flow<Long> {
        return _totalDurationForToday
    }

    override fun getListSessionSegments(date: LocalDate): Flow<List<SessionSegment>> {
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
