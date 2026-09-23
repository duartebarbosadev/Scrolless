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

import com.scrolless.app.accessibility.ContentBounds
import kotlin.math.roundToInt

/** Short, bounded prediction between the feed's batched accessibility scroll events. */
internal class FeedScrollMotion {
    private var eventTime = 0L
    private var pixelsPerMillis = 0f

    fun onScroll(deltaY: Int, timeMillis: Long) {
        if (timeMillis <= eventTime) return
        val elapsed = if (eventTime > 0 && timeMillis - eventTime < IDLE_MILLIS) timeMillis - eventTime else 100L
        pixelsPerMillis = -deltaY.toFloat() / elapsed
        eventTime = timeMillis
    }

    fun reset() {
        eventTime = 0
        pixelsPerMillis = 0f
    }

    fun isMoving(nowMillis: Long): Boolean = eventTime > 0 && nowMillis - eventTime in 0 until IDLE_MILLIS && pixelsPerMillis != 0f

    fun offset(observedAtMillis: Long, nowMillis: Long): Int {
        if (observedAtMillis <= 0 || !isMoving(nowMillis)) return 0
        return (pixelsPerMillis * (nowMillis - observedAtMillis).coerceIn(0, 80)).roundToInt()
    }

    /** A clipped edge may hide more video outside the viewport; keep it covered as it enters. */
    fun project(bounds: ContentBounds, viewport: ContentBounds, offset: Int): ContentBounds = bounds.copy(
        top = if (bounds.top <= viewport.top && offset > 0) viewport.top else maxOf(viewport.top, bounds.top + offset),
        bottom = if (bounds.bottom >= viewport.bottom && offset < 0) viewport.bottom else minOf(viewport.bottom, bounds.bottom + offset),
    )

    companion object {
        // Android batches recurring scroll events at about 100 ms. Do not coast beyond a missing batch.
        const val IDLE_MILLIS = 160L
    }
}
