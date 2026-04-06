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

// DetectionMethod holds the technique to find out if blocked content is visible
// Most of the apps work by just checking if the view id is present
//  but facebook (thanks) needs to be different and only works via content descriptions which is a nice hammer
sealed class DetectionMethod {
    data class ViewId(val viewId: String) : DetectionMethod()
    data class ContentDescriptions(val contentDescriptions: List<String>) : DetectionMethod()
}

// Declares each supported app together with the package names we match, the detection signal to look for,
//  and the exit action to use once blocked content is found.
@Immutable
enum class BlockableApp(
    private val packageIds: List<String>,
    private val detectionMethod: DetectionMethod,
    private val exitStrategy: Int,
) {
    REELS(
        packageIds = listOf("com.instagram.android"),
        detectionMethod = DetectionMethod.ViewId("clips_viewer_view_pager"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    SHORTS(
        packageIds = listOf("com.google.android.youtube"),
        detectionMethod = DetectionMethod.ViewId("reel_player_page_container"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    TIKTOK(
        packageIds = listOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme",
            "com.zhiliaoapp.musically.go",
        ),
        detectionMethod = DetectionMethod.ViewId("player_view"),
        exitStrategy = GLOBAL_ACTION_HOME,
    ),
    FACEBOOK(
        packageIds = listOf("com.facebook.katana"),
        detectionMethod = DetectionMethod.ContentDescriptions(
            listOf(
                "FbShortsComposerAttachmentComponentSpec_STICKER",
                "FbShortsComposerAttachmentComponentSpec_GIF",
                "Reels",
            ),
        ),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    ;

    fun getExitStrategy(): Int = exitStrategy

    fun getPackageIds(): List<String> = packageIds

    fun getDetectionMethod(): DetectionMethod = detectionMethod

    fun resolvePackage(packageName: String): String? = when {
        packageIds.contains(packageName) -> packageName

        else ->
            packageIds
                .asSequence()
                .filter { packageName.startsWith(it) }
                .maxByOrNull { it.length }
    }
}

// Represents the specific package variant
@Immutable
data class ResolvedBlockableApp(val app: BlockableApp, val packageId: String) {
    fun getDetectionMethod(): DetectionMethod = app.getDetectionMethod()

    fun getExitStrategy(): Int = app.getExitStrategy()

    // Method to obtain view id
    // The app detection method must be confirmed to be view id
    fun getViewId(): String {

        assert(app.getDetectionMethod() is DetectionMethod.ViewId)
        val viewId = (app.getDetectionMethod() as DetectionMethod.ViewId).viewId
        return "$packageId:id/$viewId"
    }
}
