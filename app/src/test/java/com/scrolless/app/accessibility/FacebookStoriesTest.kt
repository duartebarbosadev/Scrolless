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

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.ResolvedBlockableApp
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28, 35])
class FacebookStoriesTest {
    private var includeStories = false
    private val service = Robolectric.buildService(TestService::class.java).create().get()
    private val scanner = ContentScanner(service, { false }, { false }, { includeStories })
    private val facebook = ResolvedBlockableApp(BlockableApp.FACEBOOK, "com.facebook.katana")

    @Test
    fun `Stories opt in can be enabled and disabled during the same viewing session`() {
        showWindow(windowTitle = "com.facebook.katana/com.facebook.stories.viewer.activity.StoryViewerActivity")
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
        assertNotNull(scanner.findVisibleBlockedContent(facebook, currentActivity = "com.facebook.stories.viewer.activity.StoryViewerActivity"))
    }

    @Test
    fun `Reels remain detected regardless of Stories preference`() {
        showContentDescription("FbShortsComposerAttachmentComponentSpec_STICKER")
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
            windowTitle = "com.facebook.katana/com.facebook.stories.viewer.activity.StoryViewerActivity",
            packageId = "com.example.other",
        )
        assertNull(scanner.findVisibleBlockedContent(facebook))
    }

    private fun newNode(): AccessibilityNodeInfo = if (Build.VERSION.SDK_INT >= 33) {
        AccessibilityNodeInfo()
    } else {
        @Suppress("DEPRECATION")
        AccessibilityNodeInfo.obtain()
    }

    private fun showWindow(
        windowTitle: String,
        visible: Boolean = true,
        packageId: String = facebook.packageId,
    ) {
        val root = newNode().apply {
            packageName = packageId
            isVisibleToUser = visible
            setBoundsInScreen(Rect(0, 97, 1080, 2298))
        }
        val window = AccessibilityWindowInfo.obtain()
        shadowOf(window).apply {
            setRoot(root)
            setType(AccessibilityWindowInfo.TYPE_APPLICATION)
            setActive(true)
            setFocused(true)
            if (Build.VERSION.SDK_INT >= 28) {
                setTitle(windowTitle)
            }
        }
        shadowOf(service).apply {
            setRootInActiveWindow(root)
            setWindows(listOf(window))
        }
    }

    private fun showContentDescription(
        contentDesc: String,
        visible: Boolean = true,
        packageId: String = facebook.packageId,
    ) {
        val root = newNode().apply {
            packageName = packageId
            contentDescription = contentDesc
            isVisibleToUser = visible
            setBoundsInScreen(Rect(0, 97, 1080, 2298))
        }
        val window = AccessibilityWindowInfo.obtain()
        shadowOf(window).apply {
            setRoot(root)
            setType(AccessibilityWindowInfo.TYPE_APPLICATION)
            setActive(true)
            setFocused(true)
        }
        shadowOf(service).apply {
            setRootInActiveWindow(root)
            setWindows(listOf(window))
        }
    }

    class TestService : AccessibilityService() {
        override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit
        override fun onInterrupt() = Unit
    }
}
