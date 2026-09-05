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

class ContentCoverTargetTest {
    private val bounds = ContentBounds(0, 0, 1080, 2160)
    private val screen = ContentCoverTarget.Screen(bounds)
    private val window = ContentCoverTarget.Window(windowId = 12, displayId = 0, bounds = bounds)

    @Test
    fun `cover retention follows overlay type and screen state`() {
        assertFalse(screen.keepOnAppExit(screenInteractive = true))
        assertTrue(window.keepOnAppExit(screenInteractive = true))
        assertFalse(screen.keepOnAppExit(screenInteractive = false))
        assertFalse(window.keepOnAppExit(screenInteractive = false))
    }

    @Test
    fun `returning from Recents refreshes an unchanged window attachment`() {
        assertTrue(window.keepOnAppExit(screenInteractive = true))
        assertTrue(window.needsUpdate(previous = window, refreshAttachment = true))
        // Once resumed, ordinary accessibility events must not repeatedly reattach the cover.
        assertFalse(window.needsUpdate(previous = window, refreshAttachment = false))
    }

    @Test
    fun `new windows and resized covers update without a forced refresh`() {
        assertTrue(window.needsUpdate(previous = null, refreshAttachment = false))
        assertTrue(window.copy(windowId = 13).needsUpdate(previous = window, refreshAttachment = false))
        assertTrue(window.copy(bounds = ContentBounds(0, 0, 800, 1400)).needsUpdate(previous = window, refreshAttachment = false))
    }

    @Test
    fun `attachment refresh leaves the legacy screen cover cache unchanged`() {
        assertFalse(screen.needsUpdate(previous = screen, refreshAttachment = true))
        assertTrue(screen.needsUpdate(previous = null, refreshAttachment = true))
    }

    @Test
    fun `window local player bounds are used directly as the cover target`() {
        val localPlayer = ContentBounds(0, 0, 800, 1400)
        val nodes = listOf(ContentCoverNode(TikTokScreenDetector.PLAYER, localPlayer, true))
        val target = window.copy(bounds = TikTokScreenDetector.coverBounds(nodes)!!)
        assertEquals(localPlayer, target.bounds)
    }
}
