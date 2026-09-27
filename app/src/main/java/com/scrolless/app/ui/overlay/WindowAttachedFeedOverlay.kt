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
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceControl
import androidx.annotation.RequiresApi
import timber.log.Timber

/** A drawable app-window child with no input window, so Instagram keeps native touch handling. */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class WindowAttachedFeedOverlay(private val service: AccessibilityService) {
    private var control: SurfaceControl? = null
    private var surface: Surface? = null
    private var painter: FeedContentCoverView? = null
    private var target: ContentCoverTarget.Window? = null
    private val choreographer = Choreographer.getInstance()
    private var framePending = false
    private val frame = Choreographer.FrameCallback {
        framePending = false
        try {
            draw()
        } catch (error: RuntimeException) {
            Timber.e(error, "Unable to draw attached feed cover")
            hide()
        }
    }

    fun show(cover: ContentCover, refreshAttachment: Boolean): Boolean {
        val next = cover.target as ContentCoverTarget.Window
        if (target?.windowId != next.windowId || target?.displayId != next.displayId) hide()
        val previous = target
        try {
            val width = next.bounds.right.coerceAtLeast(1)
            val height = next.bounds.bottom.coerceAtLeast(1)
            val sc = control ?: SurfaceControl.Builder()
                .setName("ScrollessFeedCover")
                .setBufferSize(width, height)
                .setFormat(PixelFormat.RGBA_8888)
                .build().also {
                    control = it
                    surface = Surface(it)
                    val display = service.getSystemService(DisplayManager::class.java).getDisplay(next.displayId)
                    requireNotNull(display)
                    painter = FeedContentCoverView(service.createDisplayContext(display), ::scheduleFrame)
                }
            if (previous?.bounds?.right != width || previous.bounds.bottom != height) {
                SurfaceControl.Transaction().use {
                    it.setBufferSize(sc, width, height).setLayer(sc, Int.MAX_VALUE).setVisibility(sc, true).apply()
                }
            }
            val view = requireNotNull(painter)
            view.layout(0, 0, width, height)
            view.update(cover)
            if (previous == null) draw()
            if (refreshAttachment || previous?.windowId != next.windowId || previous.displayId != next.displayId) {
                service.attachAccessibilityOverlayToWindow(next.windowId, sc)
            }
            target = next
            return true
        } catch (error: RuntimeException) {
            Timber.e(error, "Unable to attach feed cover")
            hide()
            return false
        }
    }

    fun onScroll(deltaY: Int, eventTime: Long) {
        painter?.onScroll(deltaY, eventTime)
    }

    fun stopScroll() {
        painter?.stopScroll()
    }

    private fun scheduleFrame() {
        if (framePending) return
        framePending = true
        choreographer.postFrameCallback(frame)
    }

    private fun draw() {
        val output = surface ?: return
        val view = painter ?: return
        val canvas = output.lockHardwareCanvas()
        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            view.draw(canvas)
        } finally {
            output.unlockCanvasAndPost(canvas)
        }
    }

    fun hide() {
        choreographer.removeFrameCallback(frame)
        framePending = false
        painter = null
        target = null
        surface?.release()
        surface = null
        control?.let { sc ->
            SurfaceControl.Transaction().use { it.setVisibility(sc, false).reparent(sc, null).apply() }
            sc.release()
        }
        control = null
    }
}
