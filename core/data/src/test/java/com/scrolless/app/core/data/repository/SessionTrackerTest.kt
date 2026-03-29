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

import com.scrolless.app.core.data.repository.utils.TestSchedulerTimeProvider
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.repository.SessionSegmentStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class SessionTrackerTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()

    private val store = mockk<SessionSegmentStore>()
    private var timeProvider = TestSchedulerTimeProvider(testDispatcher.scheduler)
    private var sessionTracker = SessionTrackerImpl(timeProvider = timeProvider, store)

    @Test
    fun `first app open starts new session segment`() = runTest(testDispatcher) {

        coEvery { store.addSessionSegment(any()) } returns 1L

        val app = BlockableApp.REELS
        sessionTracker.onAppOpen(app)

        sessionTracker.addToDailyUsage(200L, app)
        coVerify(exactly = 1) {
            store.addSessionSegment(any())
        }
    }

    @Test
    fun `user opens, leaves and reopens reels on same app uses same segment`() = runTest(testDispatcher) {

        val segmentId = 1L

        coEvery { store.addSessionSegment(any()) } returns segmentId
        coEvery { store.updateSessionSegmentDuration(any(), any()) } returns Unit

        val app = BlockableApp.REELS
        sessionTracker.onAppOpen(app)

        val firstSessionTime = 5_000L
        val secondSessionTime = 2_000L
        sessionTracker.addToDailyUsage(firstSessionTime, app)
        sessionTracker.addToDailyUsage(secondSessionTime, app)

        // Make sure only 1 session is created and that session is updated once
        coVerify(exactly = 1) {
            store.addSessionSegment(any())
            store.updateSessionSegmentDuration(segmentId, firstSessionTime + secondSessionTime)
        }
    }

    @Test
    fun `user opens, leaves app and after 5 sec reopens reels on same app uses same segment`() = runTest(testDispatcher) {

        val segmentId = 1L
        coEvery { store.addSessionSegment(any()) } returns segmentId
        coEvery { store.updateSessionSegmentDuration(any(), any()) } returns Unit

        val app = BlockableApp.REELS
        sessionTracker.onAppOpen(app)

        val firstSessionTime = 5_000L
        val secondSessionTime = 2_000L
        sessionTracker.addToDailyUsage(firstSessionTime, app)
        delay(1)
        sessionTracker.onAppClose()

        // Delay for a 1 second to pretend the user minimized and will open the app again, and it should use the same segment
        delay(1000)

        sessionTracker.onAppOpen(app)
        sessionTracker.addToDailyUsage(secondSessionTime, app)

        // Make sure only 1 session is created and that session is updated once
        coVerify(exactly = 1) {
            store.addSessionSegment(any())
        }
        coVerify(exactly = 1) {
            store.updateSessionSegmentDuration(segmentId, firstSessionTime + secondSessionTime)
        }
    }

    @Test
    fun `reopening app after merge window should create a new segment`() = runTest(testDispatcher) {

        coEvery { store.addSessionSegment(any()) } returns 1L
        coEvery { store.updateSessionSegmentDuration(any(), any()) } returns Unit

        val app = BlockableApp.REELS
        sessionTracker.onAppOpen(app)

        val firstSessionTime = 5_000L
        val secondSessionTime = 2_000L
        sessionTracker.addToDailyUsage(firstSessionTime, app)
        delay(1)
        sessionTracker.onAppClose()

        // Delay for a bit to pretend the user took a while to reopen the app for it to create a new session
        delay(40_000)

        sessionTracker.onAppOpen(app)
        sessionTracker.addToDailyUsage(secondSessionTime, app)

        // Make sure only 1 session is created and that session is updated once
        coVerify(exactly = 2) {
            store.addSessionSegment(any())
        }
        coVerify(exactly = 0) {
            store.updateSessionSegmentDuration(any(), any())
        }
    }

    @Test
    fun `opening two separate apps should create two separate segments`() = runTest(testDispatcher) {

        val app1 = BlockableApp.REELS
        val app2 = BlockableApp.SHORTS

        val segmentId = 1L
        coEvery { store.addSessionSegment(any()) } returns segmentId
        coEvery { store.updateSessionSegmentDuration(any(), any()) } returns Unit

        sessionTracker.onAppOpen(app1)

        val exampleSessionTime = 5_000L
        sessionTracker.addToDailyUsage(exampleSessionTime, app1)
        delay(1)
        sessionTracker.onAppClose()

        // Delay for a 1 second to pretend the user minimized and will open the app again, and it should use the same segment
        delay(1000)

        sessionTracker.onAppOpen(app2)
        sessionTracker.addToDailyUsage(exampleSessionTime, app2)

        // Make sure only 1 session is created and that session is updated once
        coVerify(exactly = 2) {
            store.addSessionSegment(any())
        }
        coVerify(exactly = 0) {
            store.updateSessionSegmentDuration(any(), any())
        }
    }
    @Test
    fun `watching brainrot changes segment at midnight`() = runTest(testDispatcher) {

        val segmentId = 1L
        coEvery { store.addSessionSegment(any()) } returns segmentId
        coEvery { store.updateSessionSegmentDuration(any(), any()) } returns Unit

        // Set the current time 5 minutes before midnight
        val timeUntilMidnight = LocalTime.MAX.minusMinutes(5)
        delay(timeUntilMidnight.toSecondOfDay() * 1000L)

        val app = BlockableApp.REELS
        sessionTracker.onAppOpen(app)

        // 10-minute session that will go past midnight, so it should create two new segments for the next day
        val exampleSessionTime = 10 * 60 * 1000L
        sessionTracker.addToDailyUsage(exampleSessionTime, app)
        delay(1)

        // Make sure only 1 session is created and that session is updated once
        coVerify(exactly = 2) {
            store.addSessionSegment(any())
        }
        coVerify(exactly = 0) {
            store.updateSessionSegmentDuration(any(), any())
        }
    }
}
