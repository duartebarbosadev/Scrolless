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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Node layouts below are taken from Instagram Lite 536 accessibility dumps. */
class InstagramLiteDetectionTest {
    private val packageId = "com.instagram.lite"
    private val app = ResolvedBlockableApp(BlockableApp.INSTAGRAM_LITE, packageId)

    private fun list(atContainerTop: Boolean, heightFraction: Float = 0.88f) = DetectionNode(
        nodeId = 0,
        className = "androidx.recyclerview.widget.RecyclerView",
        screenWidthFraction = 1f,
        screenHeightFraction = heightFraction,
        isScrollable = true,
        isAtContainerTop = atContainerTop,
    )

    private fun player(parentNodeId: Int? = 0, heightFraction: Float = 0.8f) = DetectionNode(
        nodeId = 1,
        parentNodeId = parentNodeId,
        viewId = "$packageId:id/video_view",
        className = "android.widget.FrameLayout",
        screenWidthFraction = 1f,
        screenHeightFraction = heightFraction,
    )

    @Test
    fun `Instagram Lite resolves only to its own rule`() {
        assertEquals(listOf(BlockableApp.INSTAGRAM_LITE), BlockableApp.entries.filter { it.resolvePackage(packageId) != null })
    }

    @Test
    fun `Reels viewer starting at the top of the container is detected`() {
        assertTrue(app.matchesDetectionNodes(listOf(list(atContainerTop = true), player())))
        // A landscape video is letterboxed in the Reels viewer, so the player's size must not matter.
        assertTrue(app.matchesDetectionNodes(listOf(list(atContainerTop = true), player(heightFraction = 0.6f))))
    }

    @Test
    fun `feed and Explore videos below a header are not detected`() {
        assertFalse(app.matchesDetectionNodes(listOf(list(atContainerTop = false, heightFraction = 0.83f), player(heightFraction = 0.71f))))
    }

    @Test
    fun `a player outside any list, such as a story, is not detected`() {
        assertFalse(app.matchesDetectionNodes(listOf(player(parentNodeId = null))))
    }

    @Test
    fun `a missing, hidden or unrelated player is not detected`() {
        assertFalse(app.matchesDetectionNodes(listOf(list(atContainerTop = true))))
        assertFalse(app.matchesDetectionNodes(listOf(list(atContainerTop = true), player().copy(isVisible = false))))
        assertFalse(app.matchesDetectionNodes(listOf(list(atContainerTop = true), player().copy(viewId = "$packageId:id/other_view"))))
    }
}
