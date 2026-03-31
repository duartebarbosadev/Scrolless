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
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.repository.SessionSegmentStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Duration
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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

        val sessionTime = 200L
        delay(sessionTime)
        sessionTracker.addToDailyUsage(sessionTime, app)
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
        delay(firstSessionTime)
        sessionTracker.addToDailyUsage(firstSessionTime, app)
        delay(secondSessionTime)
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
        delay(firstSessionTime)
        sessionTracker.addToDailyUsage(firstSessionTime, app)
        delay(1)
        sessionTracker.onAppClose()

        // Delay for a 1 second to pretend the user minimized and will open the app again, and it should use the same segment
        delay(1000)

        sessionTracker.onAppOpen(app)
        delay(secondSessionTime)
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
        delay(firstSessionTime)
        sessionTracker.addToDailyUsage(firstSessionTime, app)
        delay(1)
        sessionTracker.onAppClose()

        // Delay for a bit to pretend the user took a while to reopen the app for it to create a new session
        delay(40_000)

        sessionTracker.onAppOpen(app)
        delay(secondSessionTime)
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

        val capturedSegments = mutableListOf<SessionSegment>()
        coEvery { store.addSessionSegment(capture(capturedSegments)) } returns 1L
        coEvery { store.updateSessionSegmentDuration(any(), any()) } returns Unit

        val app1 = BlockableApp.REELS
        val app2 = BlockableApp.SHORTS

        sessionTracker.onAppOpen(app1)

        val exampleSessionTimeApp1 = 5_000L
        val exampleSessionTimeApp2 = 6_000L
        delay(exampleSessionTimeApp1)
        sessionTracker.addToDailyUsage(exampleSessionTimeApp1, app1)
        delay(1)
        sessionTracker.onAppClose()

        // Delay for a 1 second to pretend the user minimized and will open the app again, and it should use the same segment
        delay(1000)

        sessionTracker.onAppOpen(app2)
        delay(exampleSessionTimeApp2)
        sessionTracker.addToDailyUsage(exampleSessionTimeApp2, app2)

        // Make sure only 1 session is created and that session is updated once
        coVerify(exactly = 2) {
            store.addSessionSegment(any())
        }
        coVerify(exactly = 0) {
            store.updateSessionSegmentDuration(any(), any())
        }
        assertEquals(exampleSessionTimeApp1, capturedSegments[0].durationMillis)
        assertEquals(exampleSessionTimeApp2, capturedSegments[1].durationMillis)
    }

    @Test
    fun `watching brainrot changes segment at midnight`() = runTest(testDispatcher) {

        val capturedSegments = mutableListOf<SessionSegment>()
        coEvery { store.addSessionSegment(capture(capturedSegments)) } returns 1L
        coEvery { store.updateSessionSegmentDuration(any(), any()) } returns Unit

        // Advance the virtual clock to 5 minutes before midnight (23:55)
        val now = timeProvider.localDateTimeNow()
        val fiveMinutesBeforeMidnight = now.toLocalDate().atTime(23, 55)
        val millisUntil2355 = Duration.between(now, fiveMinutesBeforeMidnight).toMillis()
        delay(millisUntil2355)

        val app = BlockableApp.REELS
        sessionTracker.onAppOpen(app)

        // 11-minute session that will go past midnight (5 mins before, 6 mins after)
        val sessionDuration = 11 * 60 * 1000L
        delay(sessionDuration)
        sessionTracker.addToDailyUsage(sessionDuration, app)

        // Verify that two segments were created (splitting at midnight)
        coVerify(exactly = 2) {
            store.addSessionSegment(any())
        }

        assertEquals(2, capturedSegments.size)

        // The first segment should be the duration of 5 minutes (the time remaining until midnight)
        val fiveMinutesInMillis = 5 * 60 * 1000L
        assertEquals(fiveMinutesInMillis, capturedSegments[0].durationMillis)

        // The second segment should be the remaining 6 minutes after midnight
        val sixMinutesInMillis = 6 * 60 * 1000L
        assertEquals(sixMinutesInMillis, capturedSegments[1].durationMillis)
    }

    @Test
    fun `watching reels, then 26 hours later watching reels changes segment at midnight`() = runTest(testDispatcher) {

        val capturedSegments = mutableListOf<SessionSegment>()
        coEvery { store.addSessionSegment(capture(capturedSegments)) } returns 1L
        coEvery { store.updateSessionSegmentDuration(any(), any()) } returns Unit

        // Set first session

        // Set the time to today at 22:00 (2 hours before midnight) (Random time)
        var now = timeProvider.localDateTimeNow()
        val firstTimeToSet = now.toLocalDate().atTime(22, 0)
        val millisUntilTimeToSet = Duration.between(now, firstTimeToSet).toMillis()
        delay(millisUntilTimeToSet)

        val app = BlockableApp.REELS
        sessionTracker.onAppOpen(app)

        // 20-minute session that will WON'T go past midnight
        val firstSessionDuration = 20 * 60 * 1000L
        delay(20 * 60 * 1000L)
        sessionTracker.addToDailyUsage(firstSessionDuration, app)
        sessionTracker.onAppClose()

        // Set second session after 26 hours
        now = timeProvider.localDateTimeNow()
        val secondTimeToSet = now.plusHours(26)
        val millisUntilSecondTimeToSet = Duration.between(now, secondTimeToSet).toMillis()
        delay(millisUntilSecondTimeToSet)

        sessionTracker.onAppOpen(app)

        val secondSessionDuration = 10 * 60 * 1000L
        delay(secondSessionDuration)
        sessionTracker.addToDailyUsage(secondSessionDuration, app)

        // Verify that two segments were created (splitting at midnight)
        coVerify(exactly = 2) {
            store.addSessionSegment(any())
        }

        assertEquals(2, capturedSegments.size)

        // The first segment should be the duration of the firstSessionDuration
        assertEquals(firstSessionDuration, capturedSegments[0].durationMillis)

        // The second segment should be the duration of secondSessionDuration
        assertEquals(secondSessionDuration, capturedSegments[1].durationMillis)
    }
}
