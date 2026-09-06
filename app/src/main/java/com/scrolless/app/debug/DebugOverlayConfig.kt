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
package com.scrolless.app.debug

import kotlinx.coroutines.flow.MutableStateFlow

/** Debug-only switch for comparing the legacy overlay with the automatic API-level choice. */
object DebugOverlayConfig {
    /**
     * Lets a debug build use the old screen overlay on a modern phone.
     * It resets with the app process because it is a temporary test choice, not a user setting.
     */
    val forceLegacyOverlay = MutableStateFlow(false)
}
