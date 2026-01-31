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
import com.scrolless.app.core.data.database.model.toUsageSegment
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.repository.SessionSegmentStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class SessionSegmentStoreImpl(private val sessionSegmentDao: SessionSegmentDao) : SessionSegmentStore {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _sessionSegmentEntityToday = MutableStateFlow<List<SessionSegment>>(emptyList())

    init {

        coroutineScope.launch {
            sessionSegmentDao.getUsageSegment(LocalDate.now()).collect { usageSegment ->
                _sessionSegmentEntityToday.value = usageSegment.map { it.toUsageSegment() }
            }
        }
    }
    override fun getUsageSegment(date: LocalDate): Flow<List<SessionSegment>> {
        return _sessionSegmentEntityToday
    }

    override fun addUsageSegment(sessionSegment: SessionSegment) {
        coroutineScope.launch {
            val entity = SessionSegmentEntity(
                app = sessionSegment.app,
                durationMillis = sessionSegment.durationMillis,
                startDateTime = sessionSegment.startDateTime,
            )
            sessionSegmentDao.insert(entity)
        }
    }
}
