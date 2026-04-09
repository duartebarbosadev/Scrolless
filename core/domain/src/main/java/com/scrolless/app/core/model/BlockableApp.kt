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

// DetectionMethod holds the information to find out if blocked content is visible
// Most of the apps work by just checking if the view id is present
//  but facebook (thanks) needs to be different and only works via content descriptions which is a nice hammer
sealed class DetectionMethod {
    data class ViewId(val viewId: String) : DetectionMethod()
    data class ContentDescriptions(val contentDescriptions: Set<String>) : DetectionMethod()
    data class ContentDescriptionPrefix(
        val prefixes: Set<String>,
        val requireSelected: Boolean = false,
        val maxTopScreenFraction: Float? = null,
    ) : DetectionMethod()
    data class AnyOf(val detectionMethods: List<DetectionMethod>) : DetectionMethod()
}

// PackageMatcher the information to recognize supported app package variants
// Most of the apps work by just checking the exact package name
//  but patched variants need prefix/suffix matching because the package can be renamed or have different variants
//  while still being the same app family
private sealed class PackageMatcher {
    abstract fun matches(packageName: String): Boolean

    data class Exact(private val packageId: String) : PackageMatcher() {
        override fun matches(packageName: String): Boolean = packageName == packageId
    }

    data class Prefix(private val packagePrefix: String) : PackageMatcher() {
        override fun matches(packageName: String): Boolean = packageName.startsWith(packagePrefix)
    }

    data class Suffix(private val packageSuffix: String) : PackageMatcher() {
        override fun matches(packageName: String): Boolean = packageName.endsWith(packageSuffix)
    }
}

// Declares each supported app together with the package names we match, the detection signal to look for,
//  and the exit action to use once blocked content is found.
@Immutable
enum class BlockableApp(
    private val packageMatchers: List<PackageMatcher>,
    private val detectionMethod: DetectionMethod,
    private val exitStrategy: Int,
) {
    REELS(
        packageMatchers = listOf(PackageMatcher.Exact("com.instagram.android")),
        detectionMethod = DetectionMethod.ViewId("clips_viewer_view_pager"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    SHORTS(
        packageMatchers = listOf(
            PackageMatcher.Prefix("com.google.android.youtube"),
            PackageMatcher.Exact("com.google.android.apps.youtube.kids"),
            PackageMatcher.Suffix(".android.youtube"), // should match YouTube and other variants
        ),
        detectionMethod = DetectionMethod.ViewId("reel_player_page_container"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    TIKTOK(
        packageMatchers = listOf(
            PackageMatcher.Exact("com.zhiliaoapp.musically"),
            PackageMatcher.Exact("com.ss.android.ugc.trill"),
            PackageMatcher.Exact("com.ss.android.ugc.aweme"),
            PackageMatcher.Exact("com.zhiliaoapp.musically.go"),
        ),
        detectionMethod = DetectionMethod.ViewId("player_view"),
        exitStrategy = GLOBAL_ACTION_HOME,
    ),
    FACEBOOK(
        packageMatchers = listOf(PackageMatcher.Exact("com.facebook.katana")),
        // Facebook needs several detection methods as there's different ways of watching reels
        // 1. By pressing on a reel in the main feed
        //      - Easy detection by the content description Sticker & GIF
        // 2- By pressing the Reels tab
        //      - We need to see if there's "Reels" and "Tab" in the content description
        //      - The reason for not just "reels" we need to discard any text that just says Reels that can appear on the feed
        detectionMethod = DetectionMethod.AnyOf(
            listOf(
                DetectionMethod.ContentDescriptions(
                    setOf(
                        "FbShortsComposerAttachmentComponentSpec_STICKER",
                        "FbShortsComposerAttachmentComponentSpec_GIF",
                    ),
                ),
                // Facebook also shows feed shelves labeled just "Reels", which should not trigger blocking.
                // so we search for the accessibility label to start with "Reels, tab" (for example "Reels, tab 2 of 6").
                DetectionMethod.ContentDescriptionPrefix(
                    prefixes = setOf("Reels, tab"),
                    requireSelected = true,
                    maxTopScreenFraction = 0.2f,
                ),
            ),
        ),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    FACEBOOK_LITE(
        packageMatchers = listOf(PackageMatcher.Exact("com.facebook.lite")),
        detectionMethod = DetectionMethod.ViewId("video_view"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    SNAPCHAT(
        packageMatchers = listOf(PackageMatcher.Exact("com.snapchat.android")),
        detectionMethod = DetectionMethod.ViewId("spotlight_container"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    ;

    fun getExitStrategy(): Int = exitStrategy

    fun getDetectionMethod(): DetectionMethod = detectionMethod

    fun resolvePackage(packageName: String): String? = packageName.takeIf(::matchesPackage)

    private fun matchesPackage(packageName: String): Boolean = packageMatchers.any { it.matches(packageName) }
}

// Represents the specific package variant
@Immutable
data class ResolvedBlockableApp(val app: BlockableApp, val packageId: String) {
    fun getDetectionMethod(): DetectionMethod = app.getDetectionMethod()

    fun getExitStrategy(): Int = app.getExitStrategy()

    fun getViewId(detectionMethod: DetectionMethod.ViewId): String = "$packageId:id/${detectionMethod.viewId}"
}
