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

/** Covers TikTok's visible player while leaving the rest of the app usable. */
internal object TikTokScreenDetector : ContentCoverDetector {
    const val PLAYER = "player_view"
    override val requiredViewIds = setOf(PLAYER)
    override val titleRes = R.string.tiktok_blocked_title
    override val descriptionRes = R.string.tiktok_blocked_description

    override fun coverBounds(nodes: List<ContentCoverNode>): ContentBounds? =
        nodes.firstOrNull { it.viewId == PLAYER && it.isVisible && it.bounds.isVisible }?.bounds
}
