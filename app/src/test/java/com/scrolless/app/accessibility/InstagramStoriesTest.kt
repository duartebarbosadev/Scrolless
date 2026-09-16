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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowAccessibilityNodeInfo

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28, 35], shadows = [InstagramStoriesTest.IndexedNode::class])
class InstagramStoriesTest {
    private var includeStories = false
    private val service = Robolectric.buildService(TestService::class.java).create().get()
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

    private fun newNode(): AccessibilityNodeInfo = if (Build.VERSION.SDK_INT >= 33) {
        AccessibilityNodeInfo()
    } else {
        @Suppress("DEPRECATION")
        AccessibilityNodeInfo.obtain()
    }

    private fun show(viewId: String, visible: Boolean = true, packageId: String = instagram.packageId) {
        val root = newNode().apply {
            packageName = packageId
            viewIdResourceName = "$packageId:id/$viewId"
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

    // Robolectric does not provide Android's indexed view-ID lookup.
    @Implements(AccessibilityNodeInfo::class)
    class IndexedNode : ShadowAccessibilityNodeInfo() {
        @RealObject private lateinit var node: AccessibilityNodeInfo

        // Invoked by Robolectric when Android performs a view-ID lookup.
        @Suppress("unused")
        @Implementation
        fun findAccessibilityNodeInfosByViewId(viewId: String): List<AccessibilityNodeInfo> =
            if (node.viewIdResourceName == viewId) listOf(node) else emptyList()
    }
}
