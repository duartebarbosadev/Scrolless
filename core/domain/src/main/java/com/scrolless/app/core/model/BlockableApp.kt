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

/**
 * DetectionMethod describes how to recognize a video screen from its ID, labels, or layout.
 * Each app can use the signals it exposes to detect when a user is watching reels etc.
 */
sealed class DetectionMethod {
    /**
     * Matches a known view ID, using Android's direct lookup when available.
     */
    data class ViewId(val viewId: String) : DetectionMethod()

    /**
     * Matches known accessibility labels when a stable view ID is not available.
     */
    data class ContentDescriptions(val contentDescriptions: Set<String>) : DetectionMethod()

    /**
     * Matches the start of a label when the rest can change, such as an unread count.
     * Can also require selection to avoid matching an inactive tab.
     */
    data class ContentDescriptionPrefix(val prefixes: Set<String>, val requireSelected: Boolean = false) : DetectionMethod()

    /**
     * Recognizes a video layout by view types, size, and related views inside it.
     * Requiring the pieces to be nested avoids combining unrelated parts of the screen.
     */
    data class NodeStructure(
        val classNames: Set<String>,
        val minScreenWidthFraction: Float = 0f,
        val minScreenHeightFraction: Float = 0f,
        val requireScrollable: Boolean = false,
        val requireLongClickable: Boolean = false,
        val descendant: NodeStructure? = null,
    ) : DetectionMethod() {
        init {
            require(minScreenWidthFraction in 0f..1f)
            require(minScreenHeightFraction in 0f..1f)
        }
    }

    /**
     * Accepts any one of several known detection rules.
     * This supports apps that expose different screens for the same kind of video.
     */
    data class AnyOf(val detectionMethods: List<DetectionMethod>) : DetectionMethod()
}

/**
 * A plain copy of the screen details used by detection.
 */
@Immutable
data class DetectionNode(
    val nodeId: Int,
    val parentNodeId: Int? = null,
    val viewId: String? = null,
    val contentDescription: String? = null,
    val className: String? = null,
    val screenWidthFraction: Float = 0f,
    val screenHeightFraction: Float = 0f,
    val isVisible: Boolean = true,
    val isSelected: Boolean = false,
    val isScrollable: Boolean = false,
    val isLongClickable: Boolean = false,
)

/**
 * Describes how to block detected content: navigate away or cover its video region.
 * Detection chooses the action; the service carries it out when the user's limit is reached.
 */
sealed interface ContentBlockAction {
    /**
     * Uses an Android navigation action, such as Back or Home, to leave the video.
     * Used to leave the current screen the user is on
     *  (example if the user is on Reels it will press back so that he moves into the main Instagram feed)
     */
    data class PerformGlobalAction(val action: Int) : ContentBlockAction

    /**
     * Hides only the detected video rectangle so the rest of the app remains usable.
     */
    data object CoverVideoRegion : ContentBlockAction
}

/**
 * Lists supported apps, their package variants, and their default detection and blocking action.
 * A screen-specific cover detector can override the default action for a detected video region.
 */
@Immutable
enum class BlockableApp(
    private val packageIds: List<String>,
    private val detectionMethod: DetectionMethod,
    private val blockAction: ContentBlockAction,
) {
    REELS(
        packageIds = listOf("com.instagram.android"),
        detectionMethod = DetectionMethod.ViewId("clips_viewer_view_pager"),
        blockAction = ContentBlockAction.PerformGlobalAction(GLOBAL_ACTION_BACK),
    ),
    SHORTS(
        packageIds = listOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.kids",
            "app.revanced.android.youtube",
        ),
        detectionMethod = DetectionMethod.ViewId("reel_player_page_container"),
        blockAction = ContentBlockAction.PerformGlobalAction(GLOBAL_ACTION_BACK),
    ),
    // Keep the app open so the user can reach its native tabs while the video is covered.
    TIKTOK(
        packageIds = listOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme",
            "com.zhiliaoapp.musically.go",
        ),
        detectionMethod = DetectionMethod.ViewId("player_view"),
        blockAction = ContentBlockAction.CoverVideoRegion,
    ),
    TIKTOK_LITE(
        packageIds = listOf("com.zhiliaoapp.musically.go"),
        detectionMethod = DetectionMethod.ViewId("h89"),
        blockAction = ContentBlockAction.PerformGlobalAction(GLOBAL_ACTION_HOME),
    ),
    FACEBOOK(
        packageIds = listOf("com.facebook.katana"),
        // Facebook exposes different accessibility trees depending on how a Reel is opened and changes
        // its user-facing labels with the app language.
        // 1. Legacy Reel viewers expose internal composer attachment labels.
        // 2. "Reels" text content seems to remain stable in many locales. A selected navigation
        //    label beginning with "Reels," is a useful fast path, but is not required for other languages.
        // 3. The fallback requires one real viewer subtree: a large scrolling viewer that
        //    contains a large, long-clickable item, which itself contains the large video surface.
        detectionMethod = DetectionMethod.AnyOf(
            listOf(
                DetectionMethod.ContentDescriptions(
                    setOf(
                        "FbShortsComposerAttachmentComponentSpec_STICKER",
                        "FbShortsComposerAttachmentComponentSpec_GIF",
                    ),
                ),
                DetectionMethod.ContentDescriptionPrefix(
                    prefixes = setOf("Reels,"),
                    requireSelected = true,
                ),
                DetectionMethod.NodeStructure(
                    classNames = setOf("androidx.recyclerview.widget.RecyclerView"),
                    minScreenWidthFraction = 0.9f,
                    minScreenHeightFraction = 0.75f,
                    requireScrollable = true,
                    descendant = DetectionMethod.NodeStructure(
                        classNames = setOf("android.widget.Button"),
                        minScreenWidthFraction = 0.9f,
                        minScreenHeightFraction = 0.75f,
                        requireLongClickable = true,
                        descendant = DetectionMethod.NodeStructure(
                            classNames = setOf("android.view.SurfaceView"),
                            minScreenWidthFraction = 0.9f,
                            minScreenHeightFraction = 0.75f,
                        ),
                    ),
                ),
            ),
        ),
        blockAction = ContentBlockAction.PerformGlobalAction(GLOBAL_ACTION_BACK),
    ),
    FACEBOOK_LITE(
        packageIds = listOf("com.facebook.lite"),
        detectionMethod = DetectionMethod.ViewId("video_view"),
        blockAction = ContentBlockAction.PerformGlobalAction(GLOBAL_ACTION_BACK),
    ),
    SNAPCHAT(
        packageIds = listOf("com.snapchat.android"),
        detectionMethod = DetectionMethod.ViewId("spotlight_container"),
        blockAction = ContentBlockAction.PerformGlobalAction(GLOBAL_ACTION_BACK),
    ),
    ;

    fun getBlockAction(): ContentBlockAction = blockAction

    fun getDetectionMethod(): DetectionMethod = detectionMethod

    fun getPackageIds(): List<String> = packageIds

    fun resolvePackage(packageName: String): String? = packageName.takeIf(::matchesPackage)

    private fun matchesPackage(packageName: String): Boolean = packageIds.any { it == packageName }
}

/**
 * Pairs a supported app with the actual package that is open.
 * Keeping that package name lets view-ID lookups work with regional and modified app variants.
 */
@Immutable
data class ResolvedBlockableApp(val app: BlockableApp, val packageId: String) {
    fun getDetectionMethod(): DetectionMethod = app.getDetectionMethod()

    fun getBlockAction(): ContentBlockAction = app.getBlockAction()

    fun getViewId(detectionMethod: DetectionMethod.ViewId): String = "$packageId:id/${detectionMethod.viewId}"

    fun matchesDetectionNodes(nodes: Collection<DetectionNode>): Boolean {
        // Group once so nested-layout checks can find children without rescanning the whole list.
        val childrenByParentId = nodes.groupBy(DetectionNode::parentNodeId)
        return getDetectionMethod().matches(nodes, childrenByParentId)
    }

    /** Try rules that need only one node. Layout rules still need the full set of related nodes. */
    fun matchesFastDetectionNode(node: DetectionNode): Boolean {
        return getDetectionMethod().matchesFastNode(node)
    }

    // Tell the screen scanner which view types matter, so it can skip unrelated layout details.
    fun getStructuralClassNames(): Set<String> {
        return buildSet { getDetectionMethod().collectStructuralClassNames(this) }
    }

    private fun DetectionMethod.matchesFastNode(node: DetectionNode): Boolean {
        if (!node.isVisible) return false
        return when (this) {
            is DetectionMethod.ViewId -> node.viewId == getViewId(this)

            is DetectionMethod.ContentDescriptions -> {
                val description = node.contentDescription ?: return false
                contentDescriptions.any { expected -> description.equals(expected, ignoreCase = true) }
            }

            is DetectionMethod.ContentDescriptionPrefix -> {
                val description = node.contentDescription ?: return false
                val prefixMatches = prefixes.any { prefix -> description.startsWith(prefix, ignoreCase = true) }
                prefixMatches && (!requireSelected || node.isSelected)
            }

            is DetectionMethod.NodeStructure -> false

            is DetectionMethod.AnyOf -> detectionMethods.any { method -> method.matchesFastNode(node) }
        }
    }

    private fun DetectionMethod.collectStructuralClassNames(destination: MutableSet<String>) {
        when (this) {
            is DetectionMethod.NodeStructure -> {
                destination += classNames
                descendant?.collectStructuralClassNames(destination)
            }

            is DetectionMethod.AnyOf -> detectionMethods.forEach { method -> method.collectStructuralClassNames(destination) }

            is DetectionMethod.ViewId,
            is DetectionMethod.ContentDescriptions,
            is DetectionMethod.ContentDescriptionPrefix,
            -> Unit
        }
    }

    private fun DetectionMethod.matches(nodes: Collection<DetectionNode>, childrenByParentId: Map<Int?, List<DetectionNode>>): Boolean {
        return when (this) {
            is DetectionMethod.ViewId -> nodes.any { node -> matchesFastNode(node) }

            is DetectionMethod.ContentDescriptions -> nodes.any { node -> matchesFastNode(node) }

            is DetectionMethod.ContentDescriptionPrefix -> nodes.any { node -> matchesFastNode(node) }

            is DetectionMethod.NodeStructure -> nodes.any { node ->
                matchesNode(node, childrenByParentId)
            }

            is DetectionMethod.AnyOf -> detectionMethods.any { method -> method.matches(nodes, childrenByParentId) }
        }
    }

    private fun DetectionMethod.NodeStructure.matchesNode(
        node: DetectionNode,
        childrenByParentId: Map<Int?, List<DetectionNode>>,
    ): Boolean {
        val matchesThisNode = node.isVisible &&
            node.className in classNames &&
            node.screenWidthFraction >= minScreenWidthFraction &&
            node.screenHeightFraction >= minScreenHeightFraction &&
            (!requireScrollable || node.isScrollable) &&
            (!requireLongClickable || node.isLongClickable)

        if (!matchesThisNode) return false
        // Related pieces must be inside this matching node, not somewhere else on the screen.
        val requiredDescendant = descendant ?: return true
        return requiredDescendant.hasMatchingDescendant(node.nodeId, childrenByParentId)
    }

    private fun DetectionMethod.NodeStructure.hasMatchingDescendant(
        parentNodeId: Int,
        childrenByParentId: Map<Int?, List<DetectionNode>>,
    ): Boolean {
        // Allow extra wrapper views between the required parts of the video layout.
        return childrenByParentId[parentNodeId].orEmpty().any { child ->
            matchesNode(child, childrenByParentId) || hasMatchingDescendant(child.nodeId, childrenByParentId)
        }
    }
}
