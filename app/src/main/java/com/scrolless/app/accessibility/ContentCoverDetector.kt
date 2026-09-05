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
    val requiredViewIds: Set<String>

    /** The message title shown instead of the covered video. */
    @get:StringRes val titleRes: Int

    /** The message text that tells the user why the video is covered. */
    @get:StringRes val descriptionRes: Int

    /**
     * Returns the rectangle to cover, or `null` when the current screen should remain usable.
     */
    fun coverBounds(nodes: List<ContentCoverNode>): ContentBounds?
}

/**
 * Holds the screen details a cover detector needs.
 * Plain values let us test app-specific rules without a running phone.
 */
internal data class ContentCoverNode(val viewId: String, val bounds: ContentBounds, val isVisible: Boolean)

/**
 * Returns the screen-specific cover rules for this app.
 * Apps without a detector keep their existing Back or Home action.
 */
internal val ResolvedBlockableApp.coverDetector: ContentCoverDetector?
    get() = when (app) {
        BlockableApp.TIKTOK -> TikTokScreenDetector
        else -> null
    }
