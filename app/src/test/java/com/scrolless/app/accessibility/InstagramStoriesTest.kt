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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28, 35], shadows = [IndexedTestNode::class])
class InstagramStoriesTest {
    private var includeStories = false
    private val service = Robolectric.buildService(TestAccessibilityService::class.java).create().get()
    private val scanner = ContentScanner(service, { false }, { false }, { includeStories })
    private val instagram = ResolvedBlockableApp(BlockableApp.REELS, "com.instagram.android")

    @Test
    fun `Stories opt in can be enabled and disabled during the same viewing session`() {
        show("reel_viewer_root")
        assertNull(scanner.findVisibleBlockedContent(instagram))
        includeStories = true
        assertNotNull(scanner.findVisibleBlockedContent(instagram))
        includeStories = false
        assertNull(scanner.findVisibleBlockedContent(instagram))
    }

    @Test
    fun `Reels remain detected regardless of Stories preference`() {
        show("clips_viewer_view_pager")
        assertNotNull(scanner.findVisibleBlockedContent(instagram))
        includeStories = true
        assertNotNull(scanner.findVisibleBlockedContent(instagram))
    }

    @Test
    fun `story tray hidden viewer and another app are not counted`() {
        includeStories = true
        show("reel_viewer_profile_picture")
        assertNull(scanner.findVisibleBlockedContent(instagram))
        show("reel_viewer_root", visible = false)
        assertNull(scanner.findVisibleBlockedContent(instagram))
        show("reel_viewer_root", packageId = "com.example.other")
        assertNull(scanner.findVisibleBlockedContent(instagram))
    }

    private fun show(viewId: String, visible: Boolean = true, packageId: String = instagram.packageId) =
        service.showTestWindow(packageId, viewId = viewId, visible = visible)
}
