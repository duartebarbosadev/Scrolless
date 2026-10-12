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
package com.scrolless.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scrolless.app.core.data.database.ScrollessDatabase
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.repository.BlockingConfigRepository
import com.scrolless.app.core.repository.SessionSegmentStore
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.feature.home.debug.ScreenshotMode
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Loads the fixed sample data used for the Play Store screenshots:
 * `adb shell am broadcast -n com.scrolless.app.debug/com.scrolless.app.debug.ScreenshotSeedReceiver`
 *
 * It also turns on [ScreenshotMode] to hide the debug panel.
 *
 * Today shows 55m across three sessions with a 1h daily limit selected, and the previous two weeks
 * get one total per weekday so the averages and history read the same in every capture.
 */
class ScreenshotSeedReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SeedEntryPoint {
        fun database(): ScrollessDatabase
        fun sessionSegmentStore(): SessionSegmentStore
        fun blockingConfigRepository(): BlockingConfigRepository
        fun userSettingsStore(): UserSettingsStore
    }

    override fun onReceive(context: Context, intent: Intent) {
        ScreenshotMode.isEnabled = true
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, SeedEntryPoint::class.java)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seed(entryPoint)
                Timber.i("Screenshot sample data loaded")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun seed(entryPoint: SeedEntryPoint) {
        val today = LocalDate.now()
        val sessions = entryPoint.sessionSegmentStore()

        sessions.replaceSessionSegmentsForDate(
            today,
            listOf(
                session(today, BlockableApp.SHORTS, "08:20", 10),
                session(today, BlockableApp.TIKTOK, "12:30", 30),
                session(today, BlockableApp.REELS, "18:15", 15),
            ),
        )
        for (daysAgo in 1L..HISTORY_DAYS) {
            val date = today.minusDays(daysAgo)
            sessions.replaceSessionSegmentsForDate(date, pastDay(date))
        }

        val firstLaunchAt = today.minusDays(HISTORY_DAYS + 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        entryPoint.database().openHelper.writableDatabase
            .execSQL("UPDATE user_settings SET first_launch_at = ? WHERE id = 1", arrayOf<Any>(firstLaunchAt))

        val settings = entryPoint.userSettingsStore()
        settings.setHasSeenAccessibilityExplainer(true)
        settings.setWaitingForAccessibility(false)
        settings.setHasSeenReviewPrompt(true)
        settings.setPauseUntil(0L)
        settings.setPauseDuration(TimeUnit.MINUTES.toMillis(5))
        settings.setAllowVideosSentByDm(true)
        settings.setIncludeStories(false)
        settings.setTimerOverlayToggle(true)

        val config = entryPoint.blockingConfigRepository()
        config.configureIntervalTimer(
            allowanceMillis = TimeUnit.MINUTES.toMillis(5),
            intervalLengthMillis = TimeUnit.HOURS.toMillis(1),
        )
        config.configureDailyLimit(TimeUnit.HOURS.toMillis(1))
    }

    /** Wednesdays match the history screenshot: TikTok 20m, Reels 15m, Shorts 7m. */
    private fun pastDay(date: LocalDate): List<SessionSegment> {
        if (date.dayOfWeek == DayOfWeek.WEDNESDAY) {
            return listOf(
                session(date, BlockableApp.TIKTOK, "09:10", 20),
                session(date, BlockableApp.REELS, "13:40", 15),
                session(date, BlockableApp.SHORTS, "21:05", 7),
            )
        }
        val total = WEEKDAY_TOTALS.getValue(date.dayOfWeek)
        val tiktok = total / 2
        val reels = total * 3 / 10
        return listOf(
            session(date, BlockableApp.TIKTOK, "09:00", tiktok),
            session(date, BlockableApp.REELS, "14:00", reels),
            session(date, BlockableApp.SHORTS, "20:30", total - tiktok - reels),
        )
    }

    private fun session(date: LocalDate, app: BlockableApp, start: String, minutes: Int) = SessionSegment(
        app = app,
        durationMillis = TimeUnit.MINUTES.toMillis(minutes.toLong()),
        startDateTime = date.atTime(LocalTime.parse(start)),
    )

    private companion object {
        const val HISTORY_DAYS = 14L

        val WEEKDAY_TOTALS = mapOf(
            DayOfWeek.MONDAY to 35,
            DayOfWeek.TUESDAY to 75,
            DayOfWeek.THURSDAY to 58,
            DayOfWeek.FRIDAY to 48,
            DayOfWeek.SATURDAY to 90,
            DayOfWeek.SUNDAY to 65,
        )
    }
}
