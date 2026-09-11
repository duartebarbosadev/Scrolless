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
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import com.scrolless.app.designsystem.theme.timerOverlayBackgroundColor
import javax.inject.Inject

/**
 * Manages the overlay covering the blocked video or screen area.
 * On Android 14+, covers attach directly to the app's window so they move together.
 */
class BlockedContentOverlayManager @Inject constructor() {
    private lateinit var service: AccessibilityService
    private lateinit var windowManager: WindowManager
    private var view: View? = null
    private var shownCover: ContentCover? = null
    private var windowOverlay: WindowAttachedContentOverlay? = null
    private var feedWindowOverlay: WindowAttachedFeedOverlay? = null

    fun attachServiceContext(service: AccessibilityService) {
        this.service = service
        windowManager = service.getSystemService(WindowManager::class.java)
    }

    /**
     * Shows or updates the cover overlay.
     * Returns true if it was successfully displayed.
     */
    internal fun show(cover: ContentCover, refreshAttachment: Boolean = false): Boolean {
        val target = cover.target
        if (!target.bounds.isVisible) return false
        // Different text or a different rendering mode needs a fresh view, not a resize.
        shownCover?.let { if (!cover.canReuseView(it)) hide() }
        // Accessibility sends many identical events. Leave an unchanged cover alone.
        if (!target.needsUpdate(shownCover?.target, refreshAttachment) && !cover.passThroughTouches) return true
        when (target) {
            is ContentCoverTarget.Screen -> showScreenCover(cover)

            is ContentCoverTarget.Window -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
                if (cover.passThroughTouches) {
                    val overlay = feedWindowOverlay ?: WindowAttachedFeedOverlay(service).also { feedWindowOverlay = it }
                    if (!overlay.show(cover, refreshAttachment)) {
                        hide()
                        return false
                    }
                    shownCover = cover
                    return true
                }
                val overlay = windowOverlay ?: WindowAttachedContentOverlay(service) { createCoverView(it, cover) }.also {
                    windowOverlay = it
                }
                if (!overlay.show(target, shownCover?.target as? ContentCoverTarget.Window, refreshAttachment)) {
                    hide()
                    return false
                }
            }
        }
        shownCover = cover
        return true
    }

    /** Keep attached covers for the app-switching animation; screen-off always removes them. */
    fun onAppExit(screenInteractive: Boolean) {
        if (shownCover?.target?.keepOnAppExit(screenInteractive) != true) hide()
    }

    internal fun onFeedScroll(deltaY: Int, eventTime: Long) {
        (view as? FeedContentCoverView)?.onScroll(deltaY, eventTime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) feedWindowOverlay?.onScroll(deltaY, eventTime)
    }

    internal fun stopFeedScroll() {
        (view as? FeedContentCoverView)?.stopScroll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) feedWindowOverlay?.stopScroll()
    }

    /** Draws the legacy cover using screen coordinates on Android versions without window attachment. */
    @SuppressLint("RtlHardcoded")
    private fun showScreenCover(cover: ContentCover) {
        val currentView = view
        if (currentView is FeedContentCoverView) {
            currentView.update(cover)
            return
        }
        val bounds = cover.target.bounds

        // Keep app focus and accept touches only inside this rectangle. Native tabs stay usable.
        val params = WindowManager.LayoutParams(
            if (cover.passThroughTouches) WindowManager.LayoutParams.MATCH_PARENT else bounds.width,
            if (cover.passThroughTouches) WindowManager.LayoutParams.MATCH_PARENT else bounds.height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                (if (cover.passThroughTouches) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0),
            if (cover.passThroughTouches) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE,
        ).apply {
            // Accessibility gives physical left/top coordinates, even in right-to-left languages.
            gravity = Gravity.TOP or Gravity.LEFT
            x = if (cover.passThroughTouches) 0 else bounds.left
            y = if (cover.passThroughTouches) 0 else bounds.top
            // Do not let system-bar or cutout padding shift the rectangle Android reported.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setFitInsetsTypes(0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (currentView == null) {
            val newView = if (cover.passThroughTouches) FeedContentCoverView(service).apply {
                update(cover)
            } else createCoverView(service, cover)
            windowManager.addView(newView, params)
            view = newView
        } else {
            windowManager.updateViewLayout(currentView, params)
        }
    }

    /** Removes every cover and clears cached state so the next one is created safely. */
    fun hide() {
        // Remove both possible overlay types, including a cover left attached after leaving the app.
        view?.let(windowManager::removeViewImmediate)
        view = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            windowOverlay?.hide()
            feedWindowOverlay?.hide()
        }
        windowOverlay = null
        feedWindowOverlay = null
        shownCover = null
    }

    /** Builds the opaque, touch-consuming message shown in place of a blocked video. */
    private fun createCoverView(context: Context, cover: ContentCover): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        val padding = (32 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)
        // The timer color may be translucent. The blocker must be fully opaque to hide the video.
        setBackgroundColor(timerOverlayBackgroundColor.toArgb() or 0xFF000000.toInt())
        // Consume touches inside the covered player.
        isClickable = true
        addView(
            TextView(context).apply {
                setText(cover.titleRes)
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                ViewCompat.setAccessibilityHeading(this, true)
            },
        )
        addView(
            TextView(context).apply {
                setText(cover.descriptionRes)
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, padding / 2, 0, 0)
            },
        )
    }
}
