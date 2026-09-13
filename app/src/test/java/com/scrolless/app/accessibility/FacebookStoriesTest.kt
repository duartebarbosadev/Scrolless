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
@Config(manifest = Config.NONE, sdk = [28, 35])
class FacebookStoriesTest {
    private var includeStories = false
    private val service = Robolectric.buildService(TestAccessibilityService::class.java).create().get()
    private val scanner = ContentScanner(service, { false }, { false }, { includeStories })
    private val facebook = ResolvedBlockableApp(BlockableApp.FACEBOOK, "com.facebook.katana")
    private val storyActivity = "com.facebook.stories.viewer.activity.StoryViewerActivity"

    @Test
    fun `Stories opt in can be enabled and disabled during the same viewing session`() {
        showWindow(windowTitle = "${facebook.packageId}/$storyActivity")
        assertNull(scanner.findVisibleBlockedContent(facebook))
        includeStories = true
        assertNotNull(scanner.findVisibleBlockedContent(facebook))
        includeStories = false
        assertNull(scanner.findVisibleBlockedContent(facebook))
    }

    @Test
    fun `Stories detected via currentActivity even without window title`() {
        showWindow(windowTitle = "")
        includeStories = true
        assertNull(scanner.findVisibleBlockedContent(facebook))
        assertNotNull(scanner.findVisibleBlockedContent(facebook, currentActivity = storyActivity))
    }

    /**
     * On a real device the window title is the activity *label*, not its class name, so the
     * tracked activity name must still be consulted even when the title is present.
     */
    @Test
    fun `Stories detected when window title is the app label`() {
        showWindow(windowTitle = "Facebook")
        includeStories = true
        assertNull(scanner.findVisibleBlockedContent(facebook))
        assertNotNull(scanner.findVisibleBlockedContent(facebook, currentActivity = storyActivity))
    }

    @Test
    fun `Reels remain detected regardless of Stories preference`() {
        showWindow(contentDescription = "FbShortsComposerAttachmentComponentSpec_STICKER")
        assertNotNull(scanner.findVisibleBlockedContent(facebook))
        includeStories = true
        assertNotNull(scanner.findVisibleBlockedContent(facebook))
    }

    @Test
    fun `Normal Facebook feed and another app are not counted`() {
        includeStories = true
        showWindow(windowTitle = "com.facebook.katana/com.facebook.katana.activity.FbMainTabActivity")
        assertNull(scanner.findVisibleBlockedContent(facebook))
        showWindow(
            windowTitle = "${facebook.packageId}/$storyActivity",
            packageId = "com.example.other",
        )
        assertNull(scanner.findVisibleBlockedContent(facebook))
    }

    private fun showWindow(windowTitle: String? = null, contentDescription: String? = null, packageId: String = facebook.packageId) =
        service.showTestWindow(packageId, windowTitle = windowTitle, contentDescription = contentDescription)
}
