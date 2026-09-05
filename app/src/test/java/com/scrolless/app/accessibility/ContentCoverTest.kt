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

import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.ContentBlockAction
import com.scrolless.app.core.model.ResolvedBlockableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentCoverTest {
    private val bounds = ContentBounds(0, 0, 1080, 2160)
    private val cover = ContentCover(ContentCoverTarget.Window(12, 0, bounds), 1, 2)

    @Test
    fun `only TikTok has a cover detector - other apps are unchanged`() {
        BlockableApp.entries.forEach { app ->
            val resolved = ResolvedBlockableApp(app, app.getPackageIds().first())
            if (app == BlockableApp.TIKTOK) {
                assertSame(TikTokScreenDetector, resolved.coverDetector)
            } else {
                assertNull(resolved.coverDetector)
            }
        }
    }

    @Test
    fun `a detected region overrides the app default only on that screen`() {
        // Synthetic content, not a newly registered app detector.
        val app = ResolvedBlockableApp(BlockableApp.SNAPCHAT, "com.snapchat.android")
        val fullScreen = DetectedBlockedContent(app, false)
        val feed = fullScreen.copy(cover = cover)
        assertEquals(app.getBlockAction(), fullScreen.blockAction)
        assertEquals(ContentBlockAction.CoverVideoRegion, feed.blockAction)
        assertFalse(feed.canContinueWith(fullScreen))
        assertFalse(fullScreen.canContinueWith(feed))
    }

    @Test
    fun `same app moving from cover to Back ends covered state before starting a new session`() {
        val app = ResolvedBlockableApp(BlockableApp.SNAPCHAT, "com.snapchat.android")
        val feed = DetectedBlockedContent(app, false, cover)
        val fullScreen = feed.copy(cover = null)
        val covered = ContentSession(feed, 1_000)
        assertEquals(1_000L, covered.cover(2_000)!!.durationMillis)
        assertFalse(covered.content.canContinueWith(fullScreen))
        assertNull(covered.finish(5_000))

        val viewing = ContentSession(fullScreen, 5_000)
        assertFalse(viewing.isCovered)
        assertEquals(app.getBlockAction(), viewing.content.blockAction)
        assertEquals(1_000L, viewing.finish(6_000)!!.durationMillis)
    }

    @Test
    fun `layout and suppression updates keep the same session but app changes do not`() {
        val app = ResolvedBlockableApp(BlockableApp.TIKTOK, "com.zhiliaoapp.musically")
        val content = DetectedBlockedContent(app, false, cover)
        assertTrue(content.canContinueWith(content.copy(blockingSuppressed = true)))
        assertTrue(content.canContinueWith(content.copy(cover = cover.copy(target = ContentCoverTarget.Screen(bounds)))))
        assertFalse(content.canContinueWith(content.copy(app = app.copy(packageId = "com.ss.android.ugc.trill"))))
    }

    @Test
    fun `different overlay copy cannot reuse a stale view even at identical bounds`() {
        assertFalse(cover.copy(titleRes = 3).canReuseView(cover))
        assertFalse(cover.copy(descriptionRes = 4).canReuseView(cover))
    }

    @Test
    fun `backend changes recreate the view while geometry changes only resize or reattach it`() {
        assertFalse(cover.copy(target = ContentCoverTarget.Screen(bounds)).canReuseView(cover))
        assertTrue(cover.canReuseView(cover))
        assertTrue(cover.copy(target = ContentCoverTarget.Window(15, 1, bounds.copy(right = 900))).canReuseView(cover))
    }
}
