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
import com.scrolless.app.core.model.DetectionMethod

/**
 * Covers the configured player in either TikTok variant while keeping navigation accessible.
 * Reads the player's ID from the app rule so detection and cover placement use the same target.
 */
internal class TikTokScreenDetector(override val app: BlockableApp) : ContentCoverDetector {
    private val playerViewId = (app.getDetectionMethod() as DetectionMethod.ViewId).viewId
    override val viewIds = setOf(playerViewId)
    override val titleRes = R.string.tiktok_blocked_title
    override val descriptionRes = R.string.tiktok_blocked_description

    /**
     * Returns the TikTok player bounds that should be covered.
     * When a cover is already active over the player, Android reports the player as not visible
     * to the user because our opaque cover occludes it. In that case, keep it covered as long
     * as the player view remains at the covered bounds.
     */
    override fun coverBounds(nodes: List<ContentCoverNode>, activeCoverBounds: ContentBounds?): ContentBounds? = nodes.firstOrNull {
        it.viewId == playerViewId && it.bounds.isVisible && (it.isVisible || it.bounds == activeCoverBounds)
    }?.bounds
}
