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
package com.scrolless.app.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentBoundsTest {
    @Test
    fun `dimensions are derived from node coordinates`() {
        val bounds = ContentBounds(40, 80, 1040, 2300)
        assertEquals(1000, bounds.width)
        assertEquals(2220, bounds.height)
        assertTrue(bounds.isVisible)
    }

    @Test
    fun `zero sized nodes are not visible`() {
        assertFalse(ContentBounds(0, 2160, 1080, 2160).isVisible)
        assertFalse(ContentBounds(0, 0, 0, 2160).isVisible)
    }
}
