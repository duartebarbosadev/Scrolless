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
package com.scrolless.app.core.domain.model

import com.scrolless.app.core.model.BlockedHours
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockedHoursTest {

    @Test
    fun `no hours are blocked by default`() {
        assertTrue(BlockedHours.NONE.isEmpty)
        assertFalse(BlockedHours.NONE.isBlocked(0))
        assertNull(BlockedHours.NONE.millisUntilNextBlocked(at(hour = 10)))
        assertEquals(emptyList<Pair<Int, Int>>(), BlockedHours.NONE.ranges())
    }

    @Test
    fun `toggling an hour twice unblocks it`() {
        val blocked = BlockedHours.NONE.toggle(7)

        assertTrue(blocked.isBlocked(7))
        assertFalse(blocked.isBlocked(8))
        assertEquals(BlockedHours.NONE, blocked.toggle(7))
    }

    @Test
    fun `an hour is blocked from its first to its last minute`() {
        val blocked = BlockedHours.of(9)

        assertFalse(blocked.isBlockedAt(at(hour = 8, minute = 59)))
        assertTrue(blocked.isBlockedAt(at(hour = 9, minute = 0)))
        assertTrue(blocked.isBlockedAt(at(hour = 9, minute = 59)))
        assertFalse(blocked.isBlockedAt(at(hour = 10, minute = 0)))
    }

    @Test
    fun `the time until the next blocked hour wraps past midnight`() {
        val blocked = BlockedHours.of(1)

        assertEquals(0L, blocked.millisUntilNextBlocked(at(hour = 1, minute = 30)))
        assertEquals(30 * MINUTE_MILLIS, blocked.millisUntilNextBlocked(at(hour = 0, minute = 30)))
        assertEquals(150 * MINUTE_MILLIS, blocked.millisUntilNextBlocked(at(hour = 22, minute = 30)))
    }

    @Test
    fun `ranges merge neighbouring hours and keep overnight ranges whole`() {
        val blocked = BlockedHours.of(20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 9, 10, 11, 12, 13, 14, 15)

        assertEquals(listOf(9 to 16, 20 to 6), blocked.ranges())
    }

    @Test
    fun `blocking every hour is one range`() {
        val blocked = BlockedHours.of(*IntArray(BlockedHours.HOURS_PER_DAY) { it })

        assertEquals(listOf(0 to 0), blocked.ranges())
    }

    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 10, 30, hour, minute)

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
