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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import androidx.compose.ui.graphics.toArgb
import com.scrolless.app.accessibility.ContentBounds
import com.scrolless.app.designsystem.theme.timerOverlayBackgroundColor
import androidx.core.graphics.withClip

/**
 * Draws feed covers inside a stationary, touch-through window. Moving the rectangle only schedules
 * a redraw, avoiding WindowManager moves and resizes on every accessibility update during a fling.
 */
// TODO Warning:(36, 16) Custom view `FeedContentCoverView` is missing constructor used by tools: `(Context)` or `(Context,AttributeSet)` or `(Context,AttributeSet,int)`
internal class FeedContentCoverView(context: Context, private val requestFrame: (() -> Unit)? = null) : View(context) {
    private var cover: ContentCover? = null
    private val motion = FeedScrollMotion()
    private val textLayouts = mutableMapOf<Int, Pair<StaticLayout, StaticLayout>>()
    private val padding = 32 * resources.displayMetrics.density
    private val background = timerOverlayBackgroundColor.toArgb() or 0xFF000000.toInt()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** Reuses text layouts while the video width is unchanged and batches drawing at the next frame. */
    fun update(next: ContentCover) {
        val regions = regions(next.target)
        if (next.titleRes != cover?.titleRes || next.descriptionRes != cover?.descriptionRes) textLayouts.clear()
        val widths = regions.map(::textWidth).toSet()
        textLayouts.keys.retainAll(widths)
        widths.forEach { width ->
            textLayouts.getOrPut(width) {
                textLayout(context.getString(next.titleRes), width, 24f, true) to
                    textLayout(context.getString(next.descriptionRes), width, 16f, false)
            }
        }
        cover = next
        redraw()
    }

    fun onScroll(deltaY: Int, eventTime: Long) {
        motion.onScroll(deltaY, eventTime)
        redraw()
    }

    fun stopScroll() {
        motion.reset()
        redraw()
    }

    private fun redraw() {
        if (requestFrame != null) requestFrame.invoke() else postInvalidateOnAnimation()
    }

    /** Keeps everything outside the reported video rectangle transparent, including native navigation. */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = cover ?: return
        val target = current.target
        val viewport = viewport(target)
        val now = android.os.SystemClock.uptimeMillis()
        val offset = motion.offset(current.observedAtMillis, now)
        val regions = regions(target)
        for (region in regions) {
            val bounds = viewport?.let { motion.project(region, it, offset) } ?: region
            if (!bounds.isVisible) continue
            canvas.withClip(bounds.left, bounds.top, bounds.right, bounds.bottom) {
                drawColor(background)
                val (heading, detail) = textLayouts.getValue(textWidth(region))
                val messageHeight = heading.height + padding / 2 + detail.height
                // Center within this Reel's visible cover, using the same geometry as its mask.
                // Small remaining slivers stay covered without showing fragments of the message.
                if (bounds.height >= messageHeight + 2 * padding) {
                    translate(bounds.left + padding, bounds.top + (bounds.height - messageHeight) / 2)
                    heading.draw(this)
                    translate(0f, heading.height + padding / 2)
                    detail.draw(this)
                }
            }
        }
        if (viewport != null && motion.isMoving(now)) redraw()
    }

    private fun regions(target: ContentCoverTarget): List<ContentBounds> = when (target) {
        is ContentCoverTarget.Screen -> target.regions
        is ContentCoverTarget.Window -> target.regions
    }

    private fun viewport(target: ContentCoverTarget): ContentBounds? = when (target) {
        is ContentCoverTarget.Screen -> target.viewport
        is ContentCoverTarget.Window -> target.bounds
    }

    private fun textWidth(bounds: ContentBounds): Int = (bounds.width - 2 * padding).toInt().coerceAtLeast(1)

    /** Wraps the message once per width change and respects the user's font scaling. */
    private fun textLayout(text: String, width: Int, size: Float, bold: Boolean): StaticLayout {
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, size, resources.displayMetrics)
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()
    }
}
