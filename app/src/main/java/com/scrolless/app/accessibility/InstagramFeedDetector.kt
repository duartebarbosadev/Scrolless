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

/**
 * Covers the posts on the Instagram home feed, below the Stories tray.
 * Pressing Back on the feed would leave Instagram and hide Stories too, so the feed is covered instead.
 * The top bar, the Stories tray and the tab bar stay usable.
 */
internal object InstagramFeedDetector : ContentCoverDetector {
    private const val FEED_LIST = "refreshable_container"

    // Only the home feed has this top bar. It stays when scrolling hides the title inside it.
    // Profiles and posts opened from Home do not have it.
    private const val HOME_TOP_BAR = "action_bar_LinearLayout"
    private const val STORIES_TRAY = "reels_tray_container"
    private const val STORY_VIEWER = "reel_viewer_root"

    override val app = BlockableApp.REELS

    /** Set by the service from the user's "Block Instagram feed" setting. */
    @Volatile
    override var isEnabled = false

    // The home top bar goes first: other tabs, such as Messages, reuse the feed list ID.
    override val viewIds = setOf(HOME_TOP_BAR, FEED_LIST, STORIES_TRAY, STORY_VIEWER)
    override val titleRes = R.string.instagram_feed_blocked_title
    override val descriptionRes = R.string.instagram_feed_blocked_description

    /**
     * The cover never hides the top bar or the Stories tray, so both stay visible to Android while covered.
     * That keeps this check reliable without trusting views hidden under our own cover.
     */
    override fun coverBounds(nodes: List<ContentCoverNode>, activeCoverBounds: ContentBounds?): ContentBounds? {
        fun visible(id: String) = nodes.firstOrNull { it.viewId == id && it.isVisible && it.bounds.isVisible }?.bounds

        // The Stories viewer opens above the feed in the same window. Never cover a Story.
        if (visible(STORY_VIEWER) != null) return null
        val feed = visible(FEED_LIST) ?: return null
        val topBar = visible(HOME_TOP_BAR) ?: return null

        // The tray sticks below the top bar after scrolling, so keep both above the cover.
        val top = maxOf(feed.top, topBar.bottom, visible(STORIES_TRAY)?.bottom ?: 0)
        return feed.copy(top = top).takeIf { it.isVisible }
    }
}
