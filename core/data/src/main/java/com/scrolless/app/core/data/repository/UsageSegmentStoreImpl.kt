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

import com.scrolless.app.core.data.database.dao.UsageSegmentDao
import com.scrolless.app.core.data.database.model.UsageSegmentEntity
import com.scrolless.app.core.data.database.model.toUsageSegment
import com.scrolless.app.core.model.UsageSegment
import com.scrolless.app.core.repository.UsageSegmentStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class UsageSegmentStoreImpl(private val usageSegmentDao: UsageSegmentDao) : UsageSegmentStore {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _usageSegmentEntityToday = MutableStateFlow<List<UsageSegment>>(emptyList())

    init {

        coroutineScope.launch {
            usageSegmentDao.getUsageSegment(LocalDate.now()).collect { usageSegment ->
                _usageSegmentEntityToday.value = usageSegment.map { it.toUsageSegment() }
            }
        }
    }
    override fun getUsageSegment(date: LocalDate): Flow<List<UsageSegment>> {
        return _usageSegmentEntityToday
    }

    override fun addUsageSegment(usageSegment: UsageSegment) {
        coroutineScope.launch {
            val entity = UsageSegmentEntity(
                app = usageSegment.app,
                durationMillis = usageSegment.durationMillis,
                startDateTime = usageSegment.startDateTime,
            )
            usageSegmentDao.insert(entity)
        }
    }
}
