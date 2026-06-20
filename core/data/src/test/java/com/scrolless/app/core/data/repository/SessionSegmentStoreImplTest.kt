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
import com.scrolless.app.core.data.database.dao.SessionSegmentDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SessionSegmentStoreImplTest : BaseTest() {

    private val timeProvider = MutableTimeProvider(LocalDate.of(2026, 6, 19))
    private val dao = mockk<SessionSegmentDao>()
    private val store = SessionSegmentStoreImpl(timeProvider = timeProvider, sessionSegmentDao = dao)

    @Test
    fun `today snapshot does not reuse date from store construction`() = runTest {
        val yesterday = LocalDate.of(2026, 6, 19)
        val today = LocalDate.of(2026, 6, 20)
        timeProvider.currentDate = today

        coEvery { dao.getTotalDurationSnapshot(yesterday, yesterday.plusDays(1)) } returns 99L
        coEvery { dao.getTotalDurationSnapshot(today, today.plusDays(1)) } returns 0L

        val total = store.getTodayTotalDurationSnapshot()

        assertEquals(0L, total)
        coVerify(exactly = 0) {
            dao.getTotalDurationSnapshot(yesterday, yesterday.plusDays(1))
        }
        coVerify(exactly = 1) {
            dao.getTotalDurationSnapshot(today, today.plusDays(1))
        }
    }

    private class MutableTimeProvider(var currentDate: LocalDate) : TimeProvider {
        override fun currentTimeInMillis(): Long =
            currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        override fun localDateNow(): LocalDate = currentDate

        override fun localDateTimeNow(): LocalDateTime = currentDate.atStartOfDay()
    }
}
