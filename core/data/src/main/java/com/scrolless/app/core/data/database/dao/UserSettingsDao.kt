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
package com.scrolless.app.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.scrolless.app.core.data.database.model.UserSettingsEntity
import com.scrolless.app.core.model.BlockOption
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * [androidx.room.Room] DAO for [UserSettingsEntity] related operations.
 */
@Dao
abstract class UserSettingsDao : BaseDao<UserSettingsEntity> {

    @Query("SELECT active_block_option FROM user_settings WHERE id = 1")
    abstract fun getActiveBlockOption(): Flow<BlockOption>

    @Query("UPDATE user_settings SET active_block_option = :blockOption WHERE id = 1")
    abstract suspend fun setActiveBlockOption(blockOption: BlockOption)

    @Query("SELECT time_limit FROM user_settings WHERE id = 1")
    abstract fun getTimeLimit(): Flow<Long>

    @Query("UPDATE user_settings SET time_limit = :timeLimit WHERE id = 1")
    abstract suspend fun setTimeLimit(timeLimit: Long)

    @Query("SELECT interval_length FROM user_settings WHERE id = 1")
    abstract fun getIntervalLength(): Flow<Long>

    @Query("UPDATE user_settings SET interval_length = :intervalLength WHERE id = 1")
    abstract suspend fun setIntervalLength(intervalLength: Long)

    @Query("SELECT interval_window_start_at FROM user_settings WHERE id = 1")
    abstract fun getIntervalWindowStart(): Flow<Long>

    @Query("UPDATE user_settings SET interval_window_start_at = :windowStart WHERE id = 1")
    abstract suspend fun setIntervalWindowStart(windowStart: Long)

    @Query("SELECT interval_usage FROM user_settings WHERE id = 1")
    abstract fun getIntervalUsage(): Flow<Long>

    @Query("UPDATE user_settings SET interval_usage = :usage WHERE id = 1")
    abstract suspend fun setIntervalUsage(usage: Long)

    @Query("UPDATE user_settings SET interval_window_start_at = :windowStart, interval_usage = :usage WHERE id = 1")
    abstract suspend fun updateIntervalState(windowStart: Long, usage: Long)

    @Query("SELECT timer_overlay_enabled FROM user_settings WHERE id = 1")
    abstract fun getTimerOverlayEnabled(): Flow<Boolean>

    @Query("UPDATE user_settings SET timer_overlay_enabled = :enabled WHERE id = 1")
    abstract suspend fun setTimerOverlayEnabled(enabled: Boolean)

    @Query("SELECT last_reset_day FROM user_settings WHERE id = 1")
    abstract fun getLastResetDay(): Flow<LocalDate>

    @Query("UPDATE user_settings SET last_reset_day = :date WHERE id = 1")
    abstract suspend fun setLastResetDay(date: LocalDate)

    @Query("SELECT total_daily_usage FROM user_settings WHERE id = 1")
    abstract fun getTotalDailyUsage(): Flow<Long>

    @Query("UPDATE user_settings SET total_daily_usage = :usage WHERE id = 1")
    abstract suspend fun updateTotalDailyUsage(usage: Long)

    // Per-app usage getters
    @Query("SELECT reels_daily_usage FROM user_settings WHERE id = 1")
    abstract fun getReelsDailyUsage(): Flow<Long>

    @Query("SELECT shorts_daily_usage FROM user_settings WHERE id = 1")
    abstract fun getShortsDailyUsage(): Flow<Long>

    @Query("SELECT tiktok_daily_usage FROM user_settings WHERE id = 1")
    abstract fun getTiktokDailyUsage(): Flow<Long>

    // Per-app usage setters
    @Query("UPDATE user_settings SET reels_daily_usage = :usage WHERE id = 1")
    abstract suspend fun updateReelsDailyUsage(usage: Long)

    @Query("UPDATE user_settings SET shorts_daily_usage = :usage WHERE id = 1")
    abstract suspend fun updateShortsDailyUsage(usage: Long)

    @Query("UPDATE user_settings SET tiktok_daily_usage = :usage WHERE id = 1")
    abstract suspend fun updateTiktokDailyUsage(usage: Long)

    // Reset all daily usage (for daily reset)
    @Query(
        """
        UPDATE user_settings SET
            total_daily_usage = 0,
            reels_daily_usage = 0,
            shorts_daily_usage = 0,
            tiktok_daily_usage = 0
        WHERE id = 1
        """,
    )
    abstract suspend fun resetAllDailyUsage()

    @Query("SELECT timer_overlay_x FROM user_settings WHERE id = 1")
    abstract fun getTimerOverlayPositionX(): Flow<Int>

    @Query("SELECT timer_overlay_y FROM user_settings WHERE id = 1")
    abstract fun getTimerOverlayPositionY(): Flow<Int>

    @Query("UPDATE user_settings SET timer_overlay_x = :x WHERE id = 1")
    abstract suspend fun setTimerOverlayPositionX(x: Int)

    @Query("UPDATE user_settings SET timer_overlay_y = :y WHERE id = 1")
    abstract suspend fun setTimerOverlayPositionY(y: Int)

    @Query("SELECT waiting_for_accessibility FROM user_settings WHERE id = 1")
    abstract fun getWaitingForAccessibility(): Flow<Boolean>

    @Query("UPDATE user_settings SET waiting_for_accessibility = :waiting WHERE id = 1")
    abstract suspend fun setWaitingForAccessibility(waiting: Boolean)

    @Query("SELECT has_seen_accessibility_explainer FROM user_settings WHERE id = 1")
    abstract fun getHasSeenAccessibilityExplainer(): Flow<Boolean>

    @Query("UPDATE user_settings SET has_seen_accessibility_explainer = :seen WHERE id = 1")
    abstract suspend fun setHasSeenAccessibilityExplainer(seen: Boolean)

    @Query("SELECT pause_until_at FROM user_settings WHERE id = 1")
    abstract fun getPauseUntil(): Flow<Long>

    @Query("UPDATE user_settings SET pause_until_at = :pauseUntil WHERE id = 1")
    abstract suspend fun setPauseUntil(pauseUntil: Long)

    @Query("SELECT first_launch_at FROM user_settings WHERE id = 1")
    abstract fun getFirstLaunchAt(): Flow<Long>

    @Query("UPDATE user_settings SET first_launch_at = :firstLaunchAt WHERE id = 1")
    abstract suspend fun setFirstLaunchAt(firstLaunchAt: Long)

    @Query("SELECT has_seen_review_prompt FROM user_settings WHERE id = 1")
    abstract fun getHasSeenReviewPrompt(): Flow<Boolean>

    @Query("UPDATE user_settings SET has_seen_review_prompt = :seen WHERE id = 1")
    abstract suspend fun setHasSeenReviewPrompt(seen: Boolean)

    @Query("SELECT review_prompt_attempt_count FROM user_settings WHERE id = 1")
    abstract fun getReviewPromptAttemptCount(): Flow<Int>

    @Query("UPDATE user_settings SET review_prompt_attempt_count = :count WHERE id = 1")
    abstract suspend fun setReviewPromptAttemptCount(count: Int)

    @Query("SELECT review_prompt_last_attempt_at FROM user_settings WHERE id = 1")
    abstract fun getReviewPromptLastAttemptAt(): Flow<Long>

    @Query("UPDATE user_settings SET review_prompt_last_attempt_at = :timestamp WHERE id = 1")
    abstract suspend fun setReviewPromptLastAttemptAt(timestamp: Long)
}
