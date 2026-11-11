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
package com.scrolless.app.core.domain.model

import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.DetectionNode
import com.scrolless.app.core.model.ResolvedBlockableApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Node chains below are the views the scanner collects above each player, from Facebook Lite 535 on Pixel 8 Pro
 * (Android 14) and Pixel 9 Pro Fold inner screen (Android 16) emulators.
 */
class FacebookLiteDetectionTest {
    private val packageId = "com.facebook.lite"
    private val app = ResolvedBlockableApp(BlockableApp.FACEBOOK_LITE, packageId)

    private fun list(heightFraction: Float) = DetectionNode(
        nodeId = 0,
        className = RECYCLER_VIEW,
        screenWidthFraction = 1f,
        screenHeightFraction = heightFraction,
        isScrollable = true,
    )

    private fun container(nodeId: Int, parentNodeId: Int?, heightFraction: Float, isClickable: Boolean = false) = DetectionNode(
        nodeId = nodeId,
        parentNodeId = parentNodeId,
        className = VIEW_GROUP,
        screenWidthFraction = 1f,
        screenHeightFraction = heightFraction,
        isClickable = isClickable,
    )

    private fun player(parentNodeId: Int?, heightFraction: Float) = DetectionNode(
        nodeId = 9,
        parentNodeId = parentNodeId,
        viewId = "$packageId:id/video_view",
        className = "android.widget.FrameLayout",
        screenWidthFraction = 1f,
        screenHeightFraction = heightFraction,
    )

    @Test
    fun `reels are detected from the Reels tab and from the feed`() {
        // Reels tab, below the tab bar: list > page > player container > player.
        assertTrue(app.matchesDetectionNodes(listOf(list(0.88f), container(1, 0, 0.88f), container(2, 1, 0.8f), player(2, 0.8f))))
        // A reel opened from the feed starts at the top of the content; its player container is long-clickable only.
        assertTrue(
            app.matchesDetectionNodes(
                listOf(list(0.93f), container(1, 0, 0.93f), container(2, 1, 0.8f).copy(isLongClickable = true), player(2, 0.8f)),
            ),
        )
        // Fold inner screen: the page is the player container.
        assertTrue(app.matchesDetectionNodes(listOf(list(0.85f), container(1, 0, 0.85f), player(1, 0.85f))))
    }

    @Test
    fun `home feed videos are not detected`() {
        // Phone: list > row > clickable row content > player (the clickable button around it isn't collected).
        assertFalse(
            app.matchesDetectionNodes(
                listOf(list(0.83f), container(1, 0, 0.59f), container(2, 1, 0.59f, isClickable = true), player(2, 0.49f)),
            ),
        )
        // Fold: a feed video can fill the whole feed list, so size alone would match it.
        assertFalse(
            app.matchesDetectionNodes(
                listOf(list(0.8f), container(1, 0, 0.8f), container(2, 1, 0.8f, isClickable = true), player(2, 0.8f)),
            ),
        )
        assertFalse(app.matchesDetectionNodes(listOf(list(0.8f), container(1, 0, 0.66f, isClickable = true), player(1, 0.66f))))
    }

    @Test
    fun `a reel must be inside a large scrolling list`() {
        assertFalse(app.matchesDetectionNodes(listOf(container(1, null, 0.88f), player(1, 0.8f))))
        assertFalse(app.matchesDetectionNodes(listOf(list(0.88f).copy(isScrollable = false), container(1, 0, 0.88f), player(1, 0.8f))))
        // A small carousel, such as a row of reel previews.
        assertFalse(app.matchesDetectionNodes(listOf(list(0.3f), container(1, 0, 0.3f), player(1, 0.3f))))
    }

    @Test
    fun `the More videos list opened from a landscape video is not detected`() {
        // list > post (not clickable) > player. The post also holds the header and buttons, so the player never fills it.
        assertFalse(app.matchesDetectionNodes(listOf(list(0.93f), container(1, 0, 0.48f), player(1, 0.25f))))
        // A vertical clip in the same list is as tall as a reel.
        assertFalse(app.matchesDetectionNodes(listOf(list(0.93f), container(1, 0, 0.83f), player(1, 0.68f))))
        assertFalse(app.matchesDetectionNodes(listOf(list(0.93f), container(1, 0, 0.93f), player(1, 0.8f))))
    }

    @Test
    fun `a profile video whose post briefly can't be clicked is not detected`() {
        // Seen right after tapping a landscape video on the profile: the post around the player stops being clickable.
        assertFalse(app.matchesDetectionNodes(listOf(list(0.87f), container(1, 0, 0.25f), player(1, 0.25f))))
    }

    // The story viewer has no list around its player, so the scanner collects only the player for the Stories rule.
    private fun storyPlayer(heightFraction: Float, isAtContainerTop: Boolean = true) =
        player(parentNodeId = null, heightFraction = heightFraction).copy(isAtContainerTop = isAtContainerTop)

    private val withStories = app.getDetectionMethod(includeStories = true)

    @Test
    fun `video stories are detected only when Stories are included`() {
        // Phone, then the fold's inner screen: the player fills the content area from its top.
        for (story in listOf(storyPlayer(0.93f), storyPlayer(0.9f))) {
            assertTrue(app.matchesDetectionNodes(listOf(story), withStories))
            assertFalse(app.matchesDetectionNodes(listOf(story)))
        }
    }

    @Test
    fun `the Stories rule skips feed videos and reels below the header`() {
        // Fold feed video and Reels tab player: as tall as some phones' story player, but below the header and tab bar.
        assertFalse(app.matchesDetectionNodes(listOf(storyPlayer(0.8f, isAtContainerTop = false)), withStories))
        assertFalse(app.matchesDetectionNodes(listOf(storyPlayer(0.93f, isAtContainerTop = false)), withStories))
        // A small player at the top of the content.
        assertFalse(app.matchesDetectionNodes(listOf(storyPlayer(0.3f)), withStories))
    }

    @Test
    fun `reels are still detected and feed videos still skipped when Stories are included`() {
        assertTrue(
            app.matchesDetectionNodes(listOf(list(0.88f), container(1, 0, 0.88f), container(2, 1, 0.8f), player(2, 0.8f)), withStories),
        )
        assertFalse(
            app.matchesDetectionNodes(
                listOf(list(0.8f), container(1, 0, 0.8f), container(2, 1, 0.8f, isClickable = true), player(2, 0.8f)),
                withStories,
            ),
        )
    }

    private companion object {
        const val RECYCLER_VIEW = "androidx.recyclerview.widget.RecyclerView"
        const val VIEW_GROUP = "android.view.ViewGroup"
    }
}
