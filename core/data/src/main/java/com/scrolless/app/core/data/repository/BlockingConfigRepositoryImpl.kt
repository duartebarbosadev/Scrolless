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
import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.data.database.model.toBlockingConfig
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockingConfig
import com.scrolless.app.core.model.IntervalUsage
import com.scrolless.app.core.repository.BlockingConfigRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Stores blocking settings and interval usage in the single `user_settings` row.
 *
 * Settings and usage live in separate columns, so editing the settings can expand the current
 * interval without replacing its start time or watched time.
 */
class BlockingConfigRepositoryImpl @Inject constructor(
    private val userSettingsDao: UserSettingsDao,
    private val timeProvider: TimeProvider,
) : BlockingConfigRepository {

    // Recording usage reads the current window before it writes the new one. Keep that pair
    // together when a settings edit arrives at the same time.
    private val writeMutex = Mutex()

    override fun observeConfig(): Flow<BlockingConfig> = userSettingsDao.observeUserSettings()
        .map { it.toBlockingConfig() }
        .distinctUntilChanged()

    override suspend fun getConfig(): BlockingConfig = userSettingsDao.getUserSettings().toBlockingConfig()

    override suspend fun setActiveOption(option: BlockOption) = writeMutex.withLock {
        userSettingsDao.setActiveBlockOption(option)
    }

    override suspend fun configureDailyLimit(limitMillis: Long) = writeMutex.withLock {
        require(limitMillis > 0L) { "Daily limit must be greater than zero" }
        userSettingsDao.configureDailyLimit(limitMillis)
    }

    override suspend fun configureIntervalTimer(allowanceMillis: Long, intervalLengthMillis: Long) = writeMutex.withLock {
        require(allowanceMillis > 0L) { "Interval allowance must be greater than zero" }
        require(intervalLengthMillis > 0L) { "Interval length must be greater than zero" }

        // Roll the window forward with the length it was recorded under before storing the new
        // one, otherwise lengthening the interval revives an expired window and its usage.
        val config = getConfig()
        val current = config.intervalUsage.currentAt(timeProvider.currentTimeInMillis(), config.settings.intervalLengthMillis)

        userSettingsDao.configureIntervalTimer(
            allowanceMillis = allowanceMillis,
            intervalLengthMillis = intervalLengthMillis,
            windowStart = current.startMillis,
            usage = current.usageMillis,
        )
    }

    override suspend fun recordIntervalUsage(sessionStartMillis: Long, sessionEndMillis: Long): IntervalUsage = writeMutex.withLock {
        val config = getConfig()
        val updated = config.intervalUsage.plusSession(sessionStartMillis, sessionEndMillis, config.settings.intervalLengthMillis)
        userSettingsDao.updateIntervalState(windowStart = updated.startMillis, usage = updated.usageMillis)

        updated
    }
}
