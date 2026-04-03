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
package com.scrolless.app.core.model

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import androidx.compose.runtime.Immutable

@Immutable
enum class BlockableApp(private val packageIds: List<String>, private val viewId: String, private val exitStrategy: Int, private var activePackageId) {
    REELS(
        listOf("com.instagram.android"),
        "clips_viewer_view_pager",
        GLOBAL_ACTION_BACK,
        null,
    ),
    SHORTS(
        listOf("com.google.android.youtube"),
        "reel_player_page_container",
        GLOBAL_ACTION_BACK,
        null,
    ),
    TIKTOK(
        listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme", "com.zhiliaoapp.musically.go"),
        "player_view",
        GLOBAL_ACTION_HOME,
        null,
    ),
    ;

    fun getViewId(): String = "$packageIds:id/$viewId"

    fun getExitStrategy(): Int = exitStrategy

    fun getPackageIds(): List<String> = packageIds

    fun matchesPackage(packageName: String): Boolean = packageIds.any { packageName.startsWith(it) }
}
