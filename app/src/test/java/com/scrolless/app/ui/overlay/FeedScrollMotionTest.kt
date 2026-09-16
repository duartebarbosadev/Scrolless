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
package com.scrolless.app.ui.overlay

import com.scrolless.app.accessibility.ContentBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FeedScrollMotionTest {
    private val motion = FeedScrollMotion()
    private val viewport = ContentBounds(0, 100, 1080, 2100)

    @Test
    fun `pixel deltas drive bounded motion between layout samples`() {
        motion.onScroll(150, 1000)
        motion.onScroll(162, 1108)
        assertEquals(-60, motion.offset(1108, 1148))
        assertEquals(-120, motion.offset(1108, 1228))
        // A newer layout is already farther along the scroll; do not apply the old displacement again.
        assertEquals(-15, motion.offset(1138, 1148))
        assertEquals(0, motion.offset(0, 1148))
    }

    @Test
    fun `stops and reversals do not keep the previous direction`() {
        motion.onScroll(100, 1000)
        motion.onScroll(-100, 1100)
        assertEquals(40, motion.offset(1100, 1140))
        motion.onScroll(0, 1200)
        assertEquals(0, motion.offset(1200, 1240))
        motion.onScroll(100, 1300)
        assertEquals(0, motion.offset(1300, 1460))
        assertFalse(motion.isMoving(1460))
    }

    @Test
    fun `navigation and out of order events cannot revive old velocity`() {
        motion.onScroll(100, 1000)
        motion.onScroll(-1000, 900)
        assertEquals(-40, motion.offset(1000, 1040))
        motion.reset()
        assertEquals(0, motion.offset(1000, 1040))
        assertFalse(motion.isMoving(1040))
    }

    @Test
    fun `partly hidden videos stay covered as more of them enters the viewport`() {
        val bottomClipped = ContentBounds(0, 1500, 1080, 2100)
        assertEquals(ContentBounds(0, 1400, 1080, 2100), motion.project(bottomClipped, viewport, -100))
        val topClipped = ContentBounds(0, 100, 1080, 600)
        assertEquals(ContentBounds(0, 100, 1080, 700), motion.project(topClipped, viewport, 100))
    }

    @Test
    fun `predicted rectangles stay inside the feed and leave navigation clear`() {
        val video = ContentBounds(0, 200, 1080, 2000)
        assertEquals(ContentBounds(0, 100, 1080, 1800), motion.project(video, viewport, -200))
        assertEquals(ContentBounds(0, 400, 1080, 2100), motion.project(video, viewport, 200))
        assertFalse(motion.project(ContentBounds(0, 100, 1080, 150), viewport, -100).isVisible)
    }
}
