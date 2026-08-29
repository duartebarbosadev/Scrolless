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
    data class ContentDescriptionPrefix(val prefixes: Set<String>, val requireSelected: Boolean = false) : DetectionMethod()
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
    data class AnyOf(val detectionMethods: List<DetectionMethod>) : DetectionMethod()
}

/**
 * The small, stable subset of an accessibility node needed by content detection.
 *
 * Keeping matching independent from [android.view.accessibility.AccessibilityNodeInfo] makes detection
 * rules deterministic and testable against captured UI-tree signals from third-party apps.
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
        packageIds = listOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.kids",
            "app.revanced.android.youtube",
        ),
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
    TIKTOK_LITE(
        packageIds = listOf("com.zhiliaoapp.musically.go"),
        detectionMethod = DetectionMethod.ViewId("h89"),
        exitStrategy = GLOBAL_ACTION_HOME,
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
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    FACEBOOK_LITE(
        packageIds = listOf("com.facebook.lite"),
        detectionMethod = DetectionMethod.ViewId("video_view"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    SNAPCHAT(
        packageIds = listOf("com.snapchat.android"),
        detectionMethod = DetectionMethod.ViewId("spotlight_container"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    ;

    fun getExitStrategy(): Int = exitStrategy

    fun getDetectionMethod(): DetectionMethod = detectionMethod

    fun getPackageIds(): List<String> = packageIds

    fun resolvePackage(packageName: String): String? = packageName.takeIf(::matchesPackage)

    private fun matchesPackage(packageName: String): Boolean = packageIds.any { it == packageName }
}

// Represents the specific package variant
@Immutable
data class ResolvedBlockableApp(val app: BlockableApp, val packageId: String) {
    fun getDetectionMethod(): DetectionMethod = app.getDetectionMethod()

    fun getExitStrategy(): Int = app.getExitStrategy()

    fun getViewId(detectionMethod: DetectionMethod.ViewId): String = "$packageId:id/${detectionMethod.viewId}"

    fun matchesDetectionNodes(nodes: Collection<DetectionNode>): Boolean {
        val childrenByParentId = nodes.groupBy(DetectionNode::parentNodeId)
        return getDetectionMethod().matches(nodes, childrenByParentId)
    }

    /** Returns true only for cheap, single-node rules. A false result may still match a structural rule. */
    fun matchesFastDetectionNode(node: DetectionNode): Boolean {
        return getDetectionMethod().matchesFastNode(node)
    }

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
        val requiredDescendant = descendant ?: return true
        return requiredDescendant.hasMatchingDescendant(node.nodeId, childrenByParentId)
    }

    private fun DetectionMethod.NodeStructure.hasMatchingDescendant(
        parentNodeId: Int,
        childrenByParentId: Map<Int?, List<DetectionNode>>,
    ): Boolean {
        return childrenByParentId[parentNodeId].orEmpty().any { child ->
            matchesNode(child, childrenByParentId) || hasMatchingDescendant(child.nodeId, childrenByParentId)
        }
    }
}
