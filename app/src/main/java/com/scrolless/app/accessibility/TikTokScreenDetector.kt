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

/**
 * Finds TikTok's video region while leaving its native tabs usable.
 * Uses IDs captured from TikTok 46.6.3 so detection does not depend on the tab labels' language.
 */
internal object TikTokScreenDetector : ContentCoverDetector {
    const val PLAYER = "player_view"
    const val INBOX = "ofd"
    const val PROFILE = "ofe"
    const val CREATE = "of9"
    private val nonFeedTabs = setOf(INBOX, PROFILE, CREATE)
    override val coverViewId = PLAYER
    override val supportingViewIds = nonFeedTabs
    override val titleRes = R.string.tiktok_blocked_title
    override val descriptionRes = R.string.tiktok_blocked_description

    /**
     * Returns the TikTok player bounds that should be covered.
     * A matching attached cover keeps a temporarily hidden player blocked, unless the user has
     * selected Inbox, Profile, or Create.
     */
    override fun coverBounds(nodes: List<ContentCoverNode>, windowId: Int?, attachedCover: ContentCoverTarget.Window?): ContentBounds? {
        val visible = nodes.filter { it.isVisible && it.bounds.isVisible }
        val player = visible.firstOrNull { it.viewId == coverViewId }
        // A visible video is coverable even if it was opened from Inbox or Profile.
        if (player != null) return player.bounds

        // Android can report the player as invisible because our cover is hiding it.
        // Removing the cover then would reveal the player and cause an endless blinking loop.
        val previous = attachedCover?.takeIf { it.windowId == windowId } ?: return null
        if (visible.any { it.isSelected && it.viewId in nonFeedTabs }) return null
        // Only keep the exact player we already covered. Other hidden players must stay ignored.
        return nodes.firstOrNull {
            it.viewId == coverViewId && it.bounds.isVisible && it.bounds == previous.bounds
        }?.bounds
    }
}
