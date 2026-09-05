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
package com.scrolless.app.ui.overlay

import androidx.annotation.StringRes
import com.scrolless.app.accessibility.ContentBounds

/** Rendering request independent of app-specific detection rules. */
internal data class ContentCover(
    val target: ContentCoverTarget,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    /**
     * Whether an existing cover view can display this cover.
     * Moving or resizing can reuse the view, but different text or a different overlay type cannot.
     */
    fun canReuseView(previous: ContentCover): Boolean = titleRes == previous.titleRes && descriptionRes == previous.descriptionRes &&
        (target is ContentCoverTarget.Window) == (previous.target is ContentCoverTarget.Window)
}

/**
 * Keeps a cover's rectangle together with the place it is measured from.
 * This prevents mixing up positions on the phone screen and positions inside an app window.
 */
internal sealed interface ContentCoverTarget {
    val bounds: ContentBounds

    /**
     * Positions the cover from the phone screen's top-left corner.
     * Used by the legacy overlay, which must be removed when the user leaves the app.
     */
    data class Screen(override val bounds: ContentBounds) : ContentCoverTarget

    /**
     * Positions the cover from an app window's top-left corner on Android 14+.
     * Attaching it to that window lets Android move them together during app switching.
     */
    data class Window(val windowId: Int, val displayId: Int, override val bounds: ContentBounds) : ContentCoverTarget
}

/** Keep attached covers during Home/Recents gestures. Remove screen covers so they do not cover the launcher. */
internal fun ContentCoverTarget.keepOnAppExit(screenInteractive: Boolean): Boolean = this is ContentCoverTarget.Window && screenInteractive

/** Returning from Recents can replace the app's drawing surface without changing its window ID or size. */
internal fun ContentCoverTarget.needsUpdate(previous: ContentCoverTarget?, refreshAttachment: Boolean): Boolean =
    this != previous || (this is ContentCoverTarget.Window && refreshAttachment)
