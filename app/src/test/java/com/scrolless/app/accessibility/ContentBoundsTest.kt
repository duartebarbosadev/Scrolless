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
import org.junit.Test

class ContentBoundsTest {
    @Test
    fun `current TikTok player ends exactly at its navigation bar`() {
        val player = ContentBounds(0, 0, 1080, 2160)
        val tabs = ContentBounds(0, 2160, 1080, 2298)
        assertEquals(player, player.above(tabs))
    }

    @Test
    fun `overlapping player is clipped above native tabs`() {
        assertEquals(
            ContentBounds(40, 80, 1040, 2160),
            ContentBounds(40, 80, 1040, 2300).above(ContentBounds(0, 2160, 1080, 2298)),
        )
    }

    @Test
    fun `nested player without bottom tabs uses its own bounds`() {
        val player = ContentBounds(0, 80, 1080, 2100)
        assertEquals(player, player.above(null))
    }

    @Test
    fun `resized or rotated window uses new node coordinates`() {
        assertEquals(
            ContentBounds(120, 40, 2000, 940),
            ContentBounds(120, 40, 2000, 1000).above(ContentBounds(120, 940, 2000, 1080)),
        )
    }

    @Test
    fun `zero sized nodes are not visible`() {
        assertFalse(ContentBounds(0, 2160, 1080, 2160).isVisible)
        assertFalse(ContentBounds(0, 0, 0, 2160).isVisible)
    }
}
