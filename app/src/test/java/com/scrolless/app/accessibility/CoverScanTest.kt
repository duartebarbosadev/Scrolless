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
import com.scrolless.app.ui.overlay.ContentCover
import com.scrolless.app.ui.overlay.ContentCoverTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Scanner-level checks for the cover lookups, which stop early when the detector's main view is not on screen. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28, 35], shadows = [IndexedTestNode::class])
class CoverScanTest {
    private val service = Robolectric.buildService(TestAccessibilityService::class.java).create().get()
    private val scanner = ContentScanner(service, { false }, { false }, { false })
    private val tiktok = ResolvedBlockableApp(BlockableApp.TIKTOK, "com.zhiliaoapp.musically")
    private val playerBounds = ContentBounds(0, 97, 1080, 2298)
    private val activeCover = ContentCover(ContentCoverTarget.Screen(playerBounds), 1, 2)

    @Test
    fun `a TikTok player hidden under the active cover stays covered`() {
        service.showTestWindow(tiktok.packageId, viewId = "player_view", visible = false)
        assertEquals(playerBounds, scanner.findVisibleBlockedContent(tiktok, activeCover)?.cover?.target?.bounds)
    }

    @Test
    fun `a hidden TikTok player does not start a cover`() {
        service.showTestWindow(tiktok.packageId, viewId = "player_view", visible = false)
        assertNull(scanner.findVisibleBlockedContent(tiktok))
    }

    @Test
    fun `a visible TikTok player is covered`() {
        service.showTestWindow(tiktok.packageId, viewId = "player_view")
        assertEquals(playerBounds, scanner.findVisibleBlockedContent(tiktok)?.cover?.target?.bounds)
    }
}
