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

import com.scrolless.app.R
import com.scrolless.app.core.model.BlockableApp

/** Covers inline videos on Instagram Home without treating photo posts as Reels. */
internal object InstagramFeedScreenDetector : ContentCoverDetector {
    override val app = BlockableApp.REELS
    override val scrollViewId = "android:id/list"
    override val viewIds = setOf("feed_tab", scrollViewId, "clips_viewer_view_pager", "video_container")
    override val passThroughTouches = true
    override val titleRes = R.string.instagram_feed_blocked_title
    override val descriptionRes = R.string.instagram_feed_blocked_description

    /**
     * Requires the selected Home tab and the standalone video container. Photo carousels with
     * music use separate carousel video containers; playback-state views alone are not enough.
     * The tab stays available when scrolling hides or removes the Home header.
     * Leaves the full-screen viewer to its existing blocking and DM rules. An occluded video
     * stays covered only while its bounds still match a current cover. Cover every visible
     * video separately so the content between posts remains accessible.
     */
    override fun coverBounds(nodes: List<ContentCoverNode>, activeCoverBounds: List<ContentBounds>): List<ContentBounds> {
        if (nodes.none { it.viewId == "feed_tab" && it.isSelected && it.isVisible && it.bounds.isVisible }) return emptyList()
        if (nodes.any { it.viewId == "clips_viewer_view_pager" && it.isVisible && it.bounds.isVisible }) return emptyList()
        return nodes.filter {
            it.viewId == "video_container" && it.bounds.isVisible && (it.isVisible || it.bounds in activeCoverBounds)
        }.map { it.bounds }.distinct()
    }
}
