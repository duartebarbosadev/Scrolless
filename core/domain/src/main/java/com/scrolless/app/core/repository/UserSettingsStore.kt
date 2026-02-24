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

import com.scrolless.app.core.model.BlockOption
import kotlinx.coroutines.flow.Flow

interface UserSettingsStore {

    fun getActiveBlockOption(): Flow<BlockOption>
    suspend fun setActiveBlockOption(blockOption: BlockOption)

    fun getTimeLimit(): Flow<Long>
    suspend fun setTimeLimit(timeLimit: Long)

    suspend fun setIntervalLength(intervalLength: Long)
    fun getIntervalLength(): Flow<Long>

    fun getIntervalWindowStart(): Flow<Long>
    suspend fun setIntervalWindowStart(windowStart: Long)
    fun getIntervalUsage(): Flow<Long>
    suspend fun setIntervalUsage(usage: Long)
    suspend fun updateIntervalState(windowStart: Long, usage: Long)

    suspend fun setTimerOverlayToggle(enabled: Boolean)
    fun getTimerOverlayEnabled(): Flow<Boolean>

    fun getTimerOverlayPositionY(): Flow<Int>
    suspend fun setTimerOverlayPositionY(positionY: Int)

    fun getTimerOverlayPositionX(): Flow<Int>
    suspend fun setTimerOverlayPositionX(positionX: Int)

    fun getWaitingForAccessibility(): Flow<Boolean>
    suspend fun setWaitingForAccessibility(waiting: Boolean)

    fun getHasSeenAccessibilityExplainer(): Flow<Boolean>
    suspend fun setHasSeenAccessibilityExplainer(seen: Boolean)

    fun getPauseUntil(): Flow<Long>
    suspend fun setPauseUntil(pauseUntil: Long)

    fun getFirstLaunchAt(): Flow<Long>
    suspend fun setFirstLaunchAt(firstLaunchAt: Long)

    fun getHasSeenReviewPrompt(): Flow<Boolean>
    suspend fun setHasSeenReviewPrompt(seen: Boolean)

    fun getReviewPromptAttemptCount(): Flow<Int>
    suspend fun setReviewPromptAttemptCount(count: Int)
    fun getReviewPromptLastAttemptAt(): Flow<Long>
    suspend fun setReviewPromptLastAttemptAt(timestamp: Long)
}

suspend fun UserSettingsStore.setTimerOverlayPosition(positionX: Int, positionY: Int) {
    setTimerOverlayPositionX(positionX)
    setTimerOverlayPositionY(positionY)
}
