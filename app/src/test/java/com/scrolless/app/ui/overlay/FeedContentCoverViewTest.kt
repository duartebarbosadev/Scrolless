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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import com.scrolless.app.accessibility.ContentBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeedContentCoverViewTest {
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `message stays centered within the video when its position changes`() {
        val view = FeedContentCoverView(RuntimeEnvironment.getApplication())
        val viewport = ContentBounds(0, 100, 1080, 2100)
        val cover = ContentCover(
            ContentCoverTarget.Window(1, 0, viewport, listOf(ContentBounds(0, 300, 1080, 1800))),
            android.R.string.ok,
            android.R.string.cancel,
            passThroughTouches = true,
        )
        view.layout(0, 0, 1080, 2340)
        fun textRows(next: ContentCover): Set<Int> {
            view.update(next)
            val bitmap = Bitmap.createBitmap(1080, 2340, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            return (100 until 2100).filterTo(mutableSetOf()) { y ->
                (0 until 1080).any { x -> bitmap.getPixel(x, y) == Color.WHITE }
            }
        }
        val before = textRows(cover)
        val after = textRows(cover.copy(target = ContentCoverTarget.Window(1, 0, viewport, listOf(ContentBounds(0, 500, 1080, 2000)))))
        assertTrue(before.isNotEmpty())
        assertEquals(before.map { it + 200 }.toSet(), after)
    }

    @Test
    fun `scroll updates keep the overlay size fixed without requesting layout`() {
        val view = FeedContentCoverView(RuntimeEnvironment.getApplication())
        val cover = ContentCover(
            ContentCoverTarget.Screen(ContentBounds(0, 300, 1080, 2000)),
            android.R.string.ok,
            android.R.string.cancel,
            passThroughTouches = true,
        )
        view.update(cover)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2340, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 1080, 2340)
        view.update(cover.copy(target = ContentCoverTarget.Screen(ContentBounds(0, 97, 1080, 800))))
        assertFalse(view.isLayoutRequested)
        assertEquals(1080, view.width)
        assertEquals(2340, view.height)
    }
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `draws both videos with a transparent gap and clears a removed video`() {
        val view = FeedContentCoverView(RuntimeEnvironment.getApplication())
        val first = ContentBounds(0, 100, 1080, 700)
        val second = ContentBounds(0, 1000, 800, 2100)
        val cover = ContentCover(
            ContentCoverTarget.Screen(listOf(first, second)),
            android.R.string.ok,
            android.R.string.cancel,
            passThroughTouches = true,
        )
        view.layout(0, 0, 1080, 2340)
        view.update(cover)
        val bitmap = Bitmap.createBitmap(1080, 2340, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        assertEquals(255, Color.alpha(bitmap.getPixel(10, 200)))
        assertEquals(255, Color.alpha(bitmap.getPixel(10, 1100)))
        assertEquals(0, Color.alpha(bitmap.getPixel(10, 800)))
        assertEquals(0, Color.alpha(bitmap.getPixel(900, 1100)))
        view.update(cover.copy(target = ContentCoverTarget.Screen(first)))
        bitmap.eraseColor(Color.TRANSPARENT)
        view.draw(Canvas(bitmap))
        assertEquals(255, Color.alpha(bitmap.getPixel(10, 200)))
        assertEquals(0, Color.alpha(bitmap.getPixel(10, 1100)))
    }
}
