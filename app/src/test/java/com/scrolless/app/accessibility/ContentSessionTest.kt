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

import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.ResolvedBlockableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSessionTest {
    private val app = ResolvedBlockableApp(BlockableApp.TIKTOK, "com.zhiliaoapp.musically")
    private val cover = ContentCover(ContentCoverTarget.Screen(ContentBounds(0, 0, 1080, 2160)), 1, 2)
    private val content = DetectedBlockedContent(app, false, cover)

    @Test
    fun `covering finishes viewing only once`() {
        val session = ContentSession(content, 1_000)

        assertEquals(5_000L, session.cover(6_000)!!.durationMillis)
        assertTrue(session.isCovered)
        assertNull(session.cover(10_000))
        assertNull(session.finish(60_000))
    }

    @Test
    fun `viewing after a covered interval starts at the new time`() {
        val covered = ContentSession(content, 1_000)
        assertEquals(1_000L, covered.cover(2_000)!!.durationMillis)

        val resumed = ContentSession(content, 20_000)
        assertEquals(20_000L, resumed.startedAtMillis)
        assertEquals(5_000L, resumed.finish(25_000)!!.durationMillis)
    }

    @Test
    fun `suppressed content still records viewing time`() {
        val session = ContentSession(content.copy(blockingSuppressed = true), 1_000)

        assertTrue(session.blockingSuppressed)
        assertEquals(5_000L, session.finish(6_000)!!.durationMillis)
    }

    @Test
    fun `layout updates keep the original viewing start`() {
        val session = ContentSession(content, 1_000)
        session.content = content.copy(cover = cover.copy(target = ContentCoverTarget.Screen(ContentBounds(0, 0, 2160, 1000))))

        assertFalse(session.isCovered)
        assertEquals(1_000L, session.cover(2_000)!!.durationMillis)
    }

    @Test
    fun `clock moving backwards cannot record negative usage`() {
        val session = ContentSession(content, 10_000)

        assertEquals(0L, session.finish(1_000)!!.durationMillis)
    }
}
