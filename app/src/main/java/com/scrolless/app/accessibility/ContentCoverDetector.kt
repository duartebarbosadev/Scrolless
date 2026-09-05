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
 * Tells Scrolless which part of the screen to cover and what message to show.
 */
internal interface ContentCoverDetector {
    val requiredViewIds: Set<String>

    /** Title shown on the overlay covering the video. */
    @get:StringRes val titleRes: Int

    /** Message explaining why the video is covered. */
    @get:StringRes val descriptionRes: Int

    /**
     * Returns the area to cover, or `null` if the screen shouldn't be blocked.
     *
     * @param nodes Views found on the current screen.
     * @param activeCoverBounds The current cover's bounds, so an already covered player stays covered.
     */
    fun coverBounds(nodes: List<ContentCoverNode>, activeCoverBounds: ContentBounds? = null): ContentBounds?
}

/** A simplified view node used to test cover rules without Android device dependencies. */
internal data class ContentCoverNode(val viewId: String, val bounds: ContentBounds, val isVisible: Boolean)

/** Returns cover rules for this app, or null if it uses full-screen blocking (Back or Home). */
internal val ResolvedBlockableApp.coverDetector: ContentCoverDetector?
    get() = when (app) {
        BlockableApp.TIKTOK -> TikTokScreenDetector
        else -> null
    }
