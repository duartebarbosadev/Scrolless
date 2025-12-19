/*
 * Copyright (C) 2025 Scrolless
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
package com.scrolless.app.core.model

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import androidx.compose.runtime.Immutable

@Immutable
enum class BlockableApp(val packageId: String, private val viewId: String, private val exitStrategy: Int) {
    REELS(
        "com.instagram.android",
        "com.instagram.android:id/clips_viewer_view_pager",
        GLOBAL_ACTION_BACK,
    ),
    SHORTS(
        "com.google.android.youtube",
        "com.google.android.youtube:id/reel_player_page_container",
        GLOBAL_ACTION_BACK,
    ),
    TIKTOK(
        "com.zhiliaoapp.musically",
        "com.zhiliaoapp.musically:id/player_view",
        GLOBAL_ACTION_HOME,
    ),
    ;

    fun getViewId(): String = viewId

    fun getExitStrategy(): Int = exitStrategy
}
