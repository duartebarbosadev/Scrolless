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
    /** The target app this cover detector is built for. */
    val app: BlockableApp

    /** App-local IDs, or fully qualified IDs for controls owned by Android. */
    val viewIds: Set<String>

    /** Fully qualified scroll container ID used to clip covers before they reach native navigation. */
    val scrollViewId: String? get() = null

    /** Lets the underlying feed handle touches naturally while its video remains hidden. */
    val passThroughTouches: Boolean get() = false

    /** Title shown on the overlay covering the video. */
    @get:StringRes val titleRes: Int

    /** Message explaining why the video is covered. */
    @get:StringRes val descriptionRes: Int

    /**
     * Returns the areas to cover, or an empty list if the screen shouldn't be blocked.
     *
     * @param nodes Views found on the current screen.
     * @param activeCoverBounds The current cover's bounds, so an already covered player stays covered.
     */
    fun coverBounds(nodes: List<ContentCoverNode>, activeCoverBounds: List<ContentBounds> = emptyList()): List<ContentBounds>
}

/** A lightweight view representation (ID, bounds, and visibility) passed to cover detectors. */
internal data class ContentCoverNode(val viewId: String, val bounds: ContentBounds, val isVisible: Boolean, val isSelected: Boolean = false)

/** All active cover detectors registered in the app. */
private val coverDetectors: Map<BlockableApp, ContentCoverDetector> = listOf(
    TikTokScreenDetector(BlockableApp.TIKTOK),
    TikTokScreenDetector(BlockableApp.TIKTOK_LITE),
    InstagramFeedScreenDetector,
).associateBy { it.app }

/** Returns cover rules for this app, or null if it uses full-screen blocking (Back or Home). */
internal val BlockableApp.coverDetector: ContentCoverDetector?
    get() = coverDetectors[this]

/** Returns cover rules for this app, or null if it uses full-screen blocking (Back or Home). */
internal val ResolvedBlockableApp.coverDetector: ContentCoverDetector?
    get() = app.coverDetector
