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
package com.scrolless.app.core.debug

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Lets the debug panel select an overlay to test on a supported phone.
 * The real Android API requirements still apply, and release builds stay in automatic mode.
 */
enum class DebugOverlayMode {
    AUTO,
    LEGACY,
    WINDOW_ATTACHED,
    ;

    // Attached covers need real API 34 support. Release builds ignore the debug choice entirely.
    fun usesWindowAttachment(sdkInt: Int, isDebug: Boolean): Boolean = sdkInt >= 34 && (!isDebug || this != LEGACY)

    companion object {
        // Shared by the debug panel and service; resets when the app process restarts.
        val selection = MutableStateFlow(AUTO)
    }
}
