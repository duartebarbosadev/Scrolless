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

import androidx.annotation.StringRes
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.ResolvedBlockableApp

/**
 * Lets each app choose which video region to cover and which message to show.
 * Drawing the cover and tracking viewing time stay shared in the service.
 */
internal interface ContentCoverDetector {
    val viewIds: Set<String>
    @get:StringRes val titleRes: Int
    @get:StringRes val descriptionRes: Int

    // Return null for an allowed screen. All rectangles use the same origin, so they can be compared.
    fun coverBounds(nodes: List<ContentCoverNode>, windowId: Int? = null, attachedCover: ContentCoverTarget.Window? = null): ContentBounds?
}

/**
 * Holds the screen details a cover detector needs.
 * Plain values let us test app-specific rules without a running phone.
 */
internal data class ContentCoverNode(val viewId: String, val bounds: ContentBounds, val isVisible: Boolean, val isSelected: Boolean = false)

// To add an app, implement the detector above and add it here. No Instagram cover is enabled yet.
// Apps without a detector keep their existing rules and Back/Home action.
internal val ResolvedBlockableApp.coverDetector: ContentCoverDetector?
    get() = when (app) {
        BlockableApp.TIKTOK -> TikTokScreenDetector
        else -> null
    }
