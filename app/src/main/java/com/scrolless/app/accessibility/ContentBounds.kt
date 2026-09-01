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

/**
 * Holds a rectangle reported by the app, such as its video or tab bar.
 * Using actual bounds keeps the cover aligned without guessing from the phone's screen size.
 */
data class ContentBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val isVisible: Boolean get() = width > 0 && height > 0

    // Stop at the tab bar so Inbox and Profile remain visible and tappable.
    fun above(navigation: ContentBounds?): ContentBounds = if (navigation != null && navigation.isVisible && navigation.top > top) {
        copy(bottom = minOf(bottom, navigation.top))
    } else {
        this
    }
}
