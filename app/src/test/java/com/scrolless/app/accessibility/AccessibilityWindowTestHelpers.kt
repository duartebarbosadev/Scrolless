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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowAccessibilityNodeInfo

/**
 * Replaces the service's windows with one synthetic app window for scanner tests.
 * The window is active and focused, with a single root node and fixed, non-empty screen bounds.
 *
 * [viewId] is the resource name without the package prefix, such as `reel_viewer_root`.
 * [visible] controls the root node's visibility; the window remains present in the service's window list.
 */
internal fun AccessibilityService.showTestWindow(
    packageId: String,
    viewId: String? = null,
    contentDescription: String? = null,
    windowTitle: String? = null,
    visible: Boolean = true,
) {
    val root = if (Build.VERSION.SDK_INT >= 33) {
        AccessibilityNodeInfo()
    } else {
        @Suppress("DEPRECATION")
        AccessibilityNodeInfo.obtain()
    }
    root.apply {
        packageName = packageId
        viewIdResourceName = viewId?.let { "$packageId:id/$it" }
        this.contentDescription = contentDescription
        isVisibleToUser = visible
        setBoundsInScreen(Rect(0, 97, 1080, 2298))
    }
    val window = AccessibilityWindowInfo.obtain()
    shadowOf(window).apply {
        setRoot(root)
        setType(AccessibilityWindowInfo.TYPE_APPLICATION)
        setActive(true)
        setFocused(true)
        if (windowTitle != null) setTitle(windowTitle)
    }
    shadowOf(this).apply {
        setRootInActiveWindow(root)
        setWindows(listOf(window))
    }
}

class TestAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit
    override fun onInterrupt() = Unit
}

// Robolectric does not provide Android's indexed view-ID lookup.
@Implements(AccessibilityNodeInfo::class)
class IndexedTestNode : ShadowAccessibilityNodeInfo() {
    @RealObject private lateinit var node: AccessibilityNodeInfo

    @Suppress("unused")
    @Implementation
    fun findAccessibilityNodeInfosByViewId(viewId: String): List<AccessibilityNodeInfo> =
        if (node.viewIdResourceName == viewId) listOf(node) else emptyList()
}
