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
package com.scrolless.app.feature.home

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DayChangeTest {

    private val yesterday = LocalDate.of(2026, 10, 29)
    private val today = LocalDate.of(2026, 10, 30)

    @Test
    fun `selection on the previous today moves to the new day`() {
        assertEquals(today, selectedDateAfterDayChange(selectedDate = yesterday, previousToday = yesterday, today = today))
    }

    @Test
    fun `selection on an older day stays where the user left it`() {
        val olderDay = LocalDate.of(2026, 10, 25)
        assertEquals(olderDay, selectedDateAfterDayChange(selectedDate = olderDay, previousToday = yesterday, today = today))
    }

    @Test
    fun `selection after today is clamped when the clock moves backwards`() {
        assertEquals(yesterday, selectedDateAfterDayChange(selectedDate = today, previousToday = today, today = yesterday))
    }
}
