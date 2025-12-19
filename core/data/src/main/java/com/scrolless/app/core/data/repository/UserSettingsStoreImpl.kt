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

import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.repository.UserSettingsStore
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * A data repository implementation for [UserSettingsStore].
 */
class UserSettingsStoreImpl(private val userSettingsDao: UserSettingsDao) : UserSettingsStore {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _activeBlockOption = MutableStateFlow(BlockOption.NothingSelected)
    private val _timeLimit = MutableStateFlow(0L)
    private val _intervalLength = MutableStateFlow(0L)
    private val _intervalWindowStart = MutableStateFlow(0L)
    private val _intervalUsage = MutableStateFlow(0L)
    private val _timerOverlayEnabled = MutableStateFlow(false)
    private val _lastResetDay = MutableStateFlow(LocalDate.now())
    private val _totalDailyUsage = MutableStateFlow(0L)
    private val _reelsDailyUsage = MutableStateFlow(0L)
    private val _shortsDailyUsage = MutableStateFlow(0L)
    private val _tiktokDailyUsage = MutableStateFlow(0L)
    private val _timerOverlayPositionY = MutableStateFlow(0)
    private val _timerOverlayPositionX = MutableStateFlow(0)
    private val _waitingForAccessibility = MutableStateFlow(false)
    private val _hasSeenAccessibilityExplainer = MutableStateFlow(false)
    private val _pauseUntil = MutableStateFlow(0L)

    init {
        coroutineScope.launch {
            userSettingsDao.getActiveBlockOption().collect { _activeBlockOption.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTimeLimit().collect { _timeLimit.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getIntervalLength().collect { _intervalLength.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getIntervalWindowStart().collect { _intervalWindowStart.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getIntervalUsage().collect { _intervalUsage.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTimerOverlayEnabled().collect { _timerOverlayEnabled.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getLastResetDay().collect { _lastResetDay.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTotalDailyUsage().collect { _totalDailyUsage.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getReelsDailyUsage().collect { _reelsDailyUsage.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getShortsDailyUsage().collect { _shortsDailyUsage.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTiktokDailyUsage().collect { _tiktokDailyUsage.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTimerOverlayPositionY().collect { _timerOverlayPositionY.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTimerOverlayPositionX().collect { _timerOverlayPositionX.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getWaitingForAccessibility().collect { _waitingForAccessibility.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getHasSeenAccessibilityExplainer().collect { _hasSeenAccessibilityExplainer.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getPauseUntil().collect { _pauseUntil.value = it }
        }
    }

    override fun getActiveBlockOption(): Flow<BlockOption> = _activeBlockOption

    override suspend fun setActiveBlockOption(blockOption: BlockOption) {
        userSettingsDao.setActiveBlockOption(blockOption)
    }

    override fun getTimeLimit(): Flow<Long> = _timeLimit

    override suspend fun setTimeLimit(timeLimit: Long) {
        userSettingsDao.setTimeLimit(timeLimit)
    }

    override suspend fun setIntervalLength(intervalLength: Long) {
        userSettingsDao.setIntervalLength(intervalLength)
    }

    override fun getIntervalLength(): Flow<Long> = _intervalLength

    override fun getIntervalWindowStart(): Flow<Long> = _intervalWindowStart

    override suspend fun setIntervalWindowStart(windowStart: Long) {
        userSettingsDao.setIntervalWindowStart(windowStart)
        _intervalWindowStart.value = windowStart
    }

    override fun getIntervalUsage(): Flow<Long> = _intervalUsage

    override suspend fun setIntervalUsage(usage: Long) {
        userSettingsDao.setIntervalUsage(usage)
        _intervalUsage.value = usage
    }

    override suspend fun updateIntervalState(windowStart: Long, usage: Long) {
        userSettingsDao.updateIntervalState(windowStart, usage)
        _intervalWindowStart.value = windowStart
        _intervalUsage.value = usage
    }

    override suspend fun setTimerOverlayToggle(enabled: Boolean) {
        userSettingsDao.setTimerOverlayEnabled(enabled)
    }

    override fun getTimerOverlayEnabled(): Flow<Boolean> = _timerOverlayEnabled

    override fun getLastResetDay(): Flow<LocalDate> = _lastResetDay

    override suspend fun setLastResetDay(date: LocalDate) {
        userSettingsDao.setLastResetDay(date)
    }

    override suspend fun updateTotalDailyUsage(totalDailyUsage: Long) {
        _totalDailyUsage.value = totalDailyUsage
        userSettingsDao.updateTotalDailyUsage(totalDailyUsage)
    }

    override fun getTotalDailyUsage(): Flow<Long> = _totalDailyUsage

    override fun getReelsDailyUsage(): Flow<Long> = _reelsDailyUsage

    override fun getShortsDailyUsage(): Flow<Long> = _shortsDailyUsage

    override fun getTiktokDailyUsage(): Flow<Long> = _tiktokDailyUsage

    override suspend fun updateReelsDailyUsage(usage: Long) {
        _reelsDailyUsage.value = usage
        userSettingsDao.updateReelsDailyUsage(usage)
    }

    override suspend fun updateShortsDailyUsage(usage: Long) {
        _shortsDailyUsage.value = usage
        userSettingsDao.updateShortsDailyUsage(usage)
    }

    override suspend fun updateTiktokDailyUsage(usage: Long) {
        _tiktokDailyUsage.value = usage
        userSettingsDao.updateTiktokDailyUsage(usage)
    }

    override suspend fun resetAllDailyUsage() {
        userSettingsDao.resetAllDailyUsage()
        _totalDailyUsage.value = 0L
        _reelsDailyUsage.value = 0L
        _shortsDailyUsage.value = 0L
        _tiktokDailyUsage.value = 0L
    }

    override fun getTimerOverlayPositionY(): Flow<Int> = _timerOverlayPositionY

    override suspend fun setTimerOverlayPositionY(positionY: Int) {
        userSettingsDao.setTimerOverlayPositionY(positionY)
    }

    override fun getTimerOverlayPositionX(): Flow<Int> = _timerOverlayPositionX

    override suspend fun setTimerOverlayPositionX(positionX: Int) {
        userSettingsDao.setTimerOverlayPositionX(positionX)
    }

    override fun getWaitingForAccessibility(): Flow<Boolean> = _waitingForAccessibility

    override suspend fun setWaitingForAccessibility(waiting: Boolean) {
        userSettingsDao.setWaitingForAccessibility(waiting)
    }

    override fun getHasSeenAccessibilityExplainer(): Flow<Boolean> = _hasSeenAccessibilityExplainer

    override suspend fun setHasSeenAccessibilityExplainer(seen: Boolean) {
        userSettingsDao.setHasSeenAccessibilityExplainer(seen)
    }

    override fun getPauseUntil(): Flow<Long> = _pauseUntil

    override suspend fun setPauseUntil(pauseUntil: Long) {
        userSettingsDao.setPauseUntil(pauseUntil)
    }
}
