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

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Binder
import android.os.Build
import android.view.SurfaceControl
import android.view.SurfaceControlViewHost
import android.view.View
import androidx.annotation.RequiresApi
import timber.log.Timber

/**
 * Attaches the cover overlay directly to the app window (Android 14+).
 * This ensures the cover smoothly follows the app during animations and gestures.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class WindowAttachedContentOverlay(private val service: AccessibilityService, private val createView: (Context) -> View) {
    private var viewHost: SurfaceControlViewHost? = null
    private var surfacePackage: SurfaceControlViewHost.SurfacePackage? = null

    /**
     * Creates or repositions the window-attached cover. Returns true if successfully displayed.
     */
    fun show(next: ContentCoverTarget.Window, previous: ContentCoverTarget.Window?, refreshAttachment: Boolean): Boolean {
        // Build a fresh host for a different window or display instead of carrying over the old one.
        if (previous?.displayId != next.displayId || previous.windowId != next.windowId) hide()
        val bounds = next.bounds
        try {
            val host = viewHost ?: run {
                val display = service.getSystemService(DisplayManager::class.java).getDisplay(next.displayId)
                    ?: return false
                val context = service.createDisplayContext(display)
                // Give the view its own window token, as Android's accessibility overlay tests do.
                SurfaceControlViewHost(context, display, Binder()).also {
                    viewHost = it
                    it.setView(createView(context), bounds.width, bounds.height)
                    surfacePackage = it.surfacePackage
                }
            }
            val surface = surfacePackage?.surfaceControl ?: run {
                hide()
                return false
            }
            if (previous?.bounds?.width != bounds.width || previous.bounds.height != bounds.height) {
                host.relayout(bounds.width, bounds.height)
            }
            // Place the cover above the video using a position inside the app window, not the screen.
            SurfaceControl.Transaction().use { transaction ->
                transaction
                    .setPosition(surface, bounds.left.toFloat(), bounds.top.toFloat())
                    .setLayer(surface, Int.MAX_VALUE)
                    .setVisibility(surface, true)
                    .apply()
            }
            // Reattach on return from Recents, even if Android reused the window ID and size.
            // Ordinary content updates keep the existing attachment to avoid blinking.
            if (refreshAttachment || previous?.windowId != next.windowId || previous.displayId != next.displayId) {
                service.attachAccessibilityOverlayToWindow(next.windowId, surface)
                Timber.d("Requested content cover attachment: window=%d, refresh=%b", next.windowId, refreshAttachment)
            }
            return true
        } catch (error: RuntimeException) {
            hide()
            Timber.e(error, "Unable to attach content cover")
            return false
        }
    }

    fun hide() {
        val oldHost = viewHost
        val oldPackage = surfacePackage
        // Clear our references first, so repeated cleanup cannot reuse a half-released cover.
        viewHost = null
        surfacePackage = null
        try {
            oldPackage?.surfaceControl?.takeIf { it.isValid }?.let { surface ->
                SurfaceControl.Transaction().use { transaction ->
                    transaction.setVisibility(surface, false).reparent(surface, null).apply()
                }
            }
        } finally {
            // Release both resources even if detaching the cover or releasing the host fails.
            try {
                oldHost?.release()
            } finally {
                oldPackage?.release()
            }
        }
    }
}
