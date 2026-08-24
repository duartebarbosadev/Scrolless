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

import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.data.database.model.savedIntervalTimer
import com.scrolless.app.core.data.database.model.toActiveOption
import com.scrolless.app.core.data.database.model.toBlockOptionType
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.IntervalTimerWindow
import com.scrolless.app.core.model.withConfig
import com.scrolless.app.core.repository.BlockingConfigRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Stores blocking settings in the single `user_settings` row.
 *
 * Writes run one at a time so a settings change cannot overwrite interval usage saved by the
 * accessibility service.
 */
class BlockingConfigRepositoryImpl @Inject constructor(private val userSettingsDao: UserSettingsDao) : BlockingConfigRepository {

    // Prevent a Home screen edit and an accessibility-service usage save from overlapping.
    private val writeMutex = Mutex()

    override fun observeActiveOption(): Flow<BlockOption> = userSettingsDao.observeBlockingConfig()
        .map { it.toActiveOption() }
        .distinctUntilChanged()

    override fun observeSavedIntervalTimer(): Flow<BlockOption.IntervalTimer> = userSettingsDao.observeBlockingConfig()
        .map { it.savedIntervalTimer() }
        .distinctUntilChanged()

    override suspend fun setActiveOption(option: BlockOption) = writeMutex.withLock {
        when (option) {
            BlockOption.BlockAll,
            BlockOption.NothingSelected,
            -> userSettingsDao.setActiveBlockOption(option.toBlockOptionType())

            is BlockOption.DailyLimit -> persist(option)

            is BlockOption.IntervalTimer -> persist(option)
        }
    }

    override suspend fun configureDailyLimit(limitMillis: Long) = writeMutex.withLock {
        persist(BlockOption.DailyLimit(limitMillis = limitMillis.coerceAtLeast(0L)))
    }

    override suspend fun configureIntervalTimer(allowanceMillis: Long, intervalLengthMillis: Long) = writeMutex.withLock {
        val current = observeActiveOption().first()
        val updated = if (current is BlockOption.IntervalTimer) {
            current.withConfig(
                allowanceMillis = allowanceMillis,
                intervalLengthMillis = intervalLengthMillis,
            )
        } else {
            BlockOption.IntervalTimer(
                allowanceMillis = allowanceMillis.coerceAtLeast(0L),
                window = IntervalTimerWindow(
                    startMillis = 0L,
                    lengthMillis = intervalLengthMillis,
                    usageMillis = 0L,
                ),
            )
        }
        persist(updated)
    }

    override suspend fun updateIntervalWindow(windowStartMillis: Long, usageMillis: Long) = writeMutex.withLock {
        userSettingsDao.updateIntervalState(
            windowStart = windowStartMillis,
            usage = usageMillis.coerceAtLeast(0L),
        )
    }

    /**
     * Saves a daily limit while keeping the last interval window available for later.
     */
    private suspend fun persist(option: BlockOption.DailyLimit) {
        val savedInterval = observeSavedIntervalTimer().first()
        persist(option = option, interval = savedInterval)
    }

    private suspend fun persist(option: BlockOption.IntervalTimer) {
        userSettingsDao.updateBlockingConfig(
            activeOption = option.toBlockOptionType(),
            limitMillis = option.allowanceMillis,
            intervalLengthMillis = option.window.lengthMillis,
            intervalWindowStartMillis = option.window.startMillis,
            intervalUsageMillis = option.window.usageMillis,
        )
    }

    /**
     * Daily and interval modes currently save their allowance in the same database column.
     */
    private suspend fun persist(option: BlockOption.DailyLimit, interval: BlockOption.IntervalTimer) {
        userSettingsDao.updateBlockingConfig(
            activeOption = option.toBlockOptionType(),
            limitMillis = option.limitMillis,
            intervalLengthMillis = interval.window.lengthMillis,
            intervalWindowStartMillis = interval.window.startMillis,
            intervalUsageMillis = interval.window.usageMillis,
        )
    }
}
