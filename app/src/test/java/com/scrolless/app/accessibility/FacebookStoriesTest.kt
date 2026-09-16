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

import android.content.ComponentName
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.ResolvedBlockableApp
import org.junit.Assert.assertEquals
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
    private var currentActivity: ComponentName? = null
    private val scanner = ContentScanner(service, { false }, { false }, { includeStories }, { currentActivity })
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
    fun `Stories detected from the activity name even without window title`() {
        showWindow(windowTitle = "")
        includeStories = true
        assertNull(scanner.findVisibleBlockedContent(facebook))
        currentActivity = ComponentName(facebook.packageId, storyActivity)
        assertNotNull(scanner.findVisibleBlockedContent(facebook))
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
        currentActivity = ComponentName(facebook.packageId, storyActivity)
        assertNotNull(scanner.findVisibleBlockedContent(facebook))
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

    @Test
    fun `an activity from another app is not used for Facebook`() {
        showWindow(windowTitle = "Facebook")
        includeStories = true
        currentActivity = ComponentName("com.example.other", storyActivity)
        assertNull(scanner.findVisibleBlockedContent(facebook))
    }

    @Test
    fun `scan without an event discovers an untracked Story when preference becomes enabled`() {
        showWindow(windowTitle = "Facebook")
        currentActivity = ComponentName(facebook.packageId, storyActivity)

        val disabled = scanner.scan(eventPackage = null, windowsChanged = true, trackedApp = null, foregroundApp = null)
        assertEquals(facebook, disabled.foregroundApp)
        assertNull(disabled.content)

        includeStories = true
        val enabled = scanner.scan(eventPackage = null, windowsChanged = true, trackedApp = null, foregroundApp = null)
        assertEquals(facebook, enabled.foregroundApp)
        assertNotNull(enabled.content)
    }

    @Test
    fun `package events and window changes detect the same Story`() {
        showWindow(windowTitle = "Facebook")
        currentActivity = ComponentName(facebook.packageId, storyActivity)
        includeStories = true

        val packageScan = scanner.scan(facebook.packageId, windowsChanged = false, trackedApp = null, foregroundApp = null)
        val windowScan = scanner.scan(eventPackage = null, windowsChanged = true, trackedApp = null, foregroundApp = null)
        assertNotNull(packageScan.content)
        assertEquals(packageScan.content, windowScan.content)
    }

    private fun showWindow(windowTitle: String? = null, contentDescription: String? = null, packageId: String = facebook.packageId) =
        service.showTestWindow(packageId, windowTitle = windowTitle, contentDescription = contentDescription)
}
