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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramFeedScreenDetectorTest {
    private val home = ContentCoverNode("feed_tab", ContentBounds(0, 2163, 216, 2298), true, isSelected = true)
    private val video = ContentCoverNode("video_container", ContentBounds(0, 287, 1080, 2066), true)

    @Test
    fun `covers the inline video and retains it while occluded`() {
        assertEquals(listOf(video.bounds), InstagramFeedScreenDetector.coverBounds(listOf(home, video)))
        val hidden = video.copy(isVisible = false)
        assertEquals(listOf(video.bounds), InstagramFeedScreenDetector.coverBounds(listOf(home, hidden), listOf(video.bounds)))
        assertTrue(InstagramFeedScreenDetector.coverBounds(listOf(home, hidden)).isEmpty())
        assertTrue(InstagramFeedScreenDetector.coverBounds(listOf(home), listOf(video.bounds)).isEmpty())
    }

    @Test
    fun `photos and screens outside Home do not get feed covers`() {
        assertTrue(InstagramFeedScreenDetector.coverBounds(listOf(home, video.copy(viewId = "media_group"))).isEmpty())
        assertTrue(InstagramFeedScreenDetector.coverBounds(listOf(video)).isEmpty())
        assertTrue(InstagramFeedScreenDetector.coverBounds(listOf(home.copy(isVisible = false), video)).isEmpty())
        assertTrue(InstagramFeedScreenDetector.coverBounds(listOf(home.copy(isSelected = false), video), listOf(video.bounds)).isEmpty())
        val viewer = video.copy(viewId = "clips_viewer_view_pager")
        assertTrue(InstagramFeedScreenDetector.coverBounds(listOf(home, video, viewer)).isEmpty())
        assertEquals(listOf(video.bounds), InstagramFeedScreenDetector.coverBounds(listOf(home, video, viewer.copy(isVisible = false))))
    }

    @Test
    fun `photo carousels with music are not standalone Reels`() {
        val photo = listOf(home) + listOf(
            "media_group", "carousel_media_group", "carousel_video_media_group",
            "carousel_video_image", "video_states",
        ).map { video.copy(viewId = it) }
        assertTrue(InstagramFeedScreenDetector.coverBounds(photo).isEmpty())
        assertTrue(InstagramFeedScreenDetector.coverBounds(photo, listOf(video.bounds)).isEmpty())
    }

    @Test
    fun `uses clipped video bounds and ignores empty players`() {
        val clipped = video.copy(bounds = ContentBounds(0, 97, 1080, 400))
        val empty = video.copy(bounds = ContentBounds(0, 97, 1080, 97))
        assertEquals(listOf(clipped.bounds), InstagramFeedScreenDetector.coverBounds(listOf(home, empty, clipped)))
        assertTrue(InstagramFeedScreenDetector.coverBounds(listOf(home, empty)).isEmpty())
        assertEquals(listOf(clipped.bounds, video.bounds), InstagramFeedScreenDetector.coverBounds(listOf(home, clipped, video)))
    }
    @Test
    fun `retains both covered videos and drops a removed post`() {
        val first = video.copy(bounds = ContentBounds(0, 100, 1080, 700))
        val second = video.copy(bounds = ContentBounds(0, 1000, 1080, 2100))
        val regions = listOf(first.bounds, second.bounds)
        assertEquals(regions, InstagramFeedScreenDetector.coverBounds(listOf(home, first, second)))
        val hidden = listOf(first, second).map { it.copy(isVisible = false) }
        assertEquals(regions, InstagramFeedScreenDetector.coverBounds(listOf(home) + hidden, regions))
        assertEquals(listOf(second.bounds), InstagramFeedScreenDetector.coverBounds(listOf(home, hidden.last()), regions))
    }
}
