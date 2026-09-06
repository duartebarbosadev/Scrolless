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

/** Window state used to figure out which app is currently in front. */
internal data class InteractiveWindowState(
    val packageName: String?,
    val isApplication: Boolean,
    val isActive: Boolean,
    val isFocused: Boolean,
)

/**
 * Finds which app the user is actively using.
 * Focus is checked first because Android can lag behind during Home and Recents gestures.
 */
internal fun foregroundAppPackage(windows: List<InteractiveWindowState>): String? {
    // Ignore our overlay and system windows; we want the app behind them.
    val apps = windows.filter { it.isApplication }
    // During a Home gesture, Android may still call the old touched window "active".
    // Prefer the focused app so tracking stops as soon as the launcher takes over.
    val foreground = apps.firstOrNull { it.isFocused } ?: apps.firstOrNull { it.isActive }
    return foreground?.packageName
}
