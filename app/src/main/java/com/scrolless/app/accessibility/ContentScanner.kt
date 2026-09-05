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

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.ChecksSdkIntAtLeast
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.ContentBlockAction
import com.scrolless.app.core.model.DetectionMethod
import com.scrolless.app.core.model.DetectionNode
import com.scrolless.app.core.model.ResolvedBlockableApp
import com.scrolless.app.ui.overlay.ContentCover
import com.scrolless.app.ui.overlay.ContentCoverTarget

/**
 * Inspects visible windows to detect blocked content and compute cover bounds.
 *
 * Cover bounds let Scrolless block only the video player (e.g. TikTok feed)
 * while keeping the rest of the app (like Inbox and Profile) usable.
 */
internal class ContentScanner(
    private val service: AccessibilityService,
    private val windowAttachedCover: () -> Boolean,
    private val allowVideosSentByDm: () -> Boolean,
) {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val useWindowAttachedCover
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && windowAttachedCover()
    private val currentAllowVideosSentByDm get() = allowVideosSentByDm()

    /**
     * Scan results for the current screen.
     *
     * @property foregroundApp The app currently on screen, or null if the user is elsewhere.
     * @property trackedAppExited True if the user left the app we were tracking.
     * @property rootAvailable True if the window layout could be inspected.
     * @property content Details of any blocked video or screen found, or null if nothing matched.
     */
    data class Result(
        val foregroundApp: ResolvedBlockableApp?,
        val trackedAppExited: Boolean,
        val rootAvailable: Boolean,
        val content: DetectedBlockedContent?,
    )

    /**
     * Checks visible windows for apps and content that should be blocked.
     *
     * @param eventPackage Package name reported by the incoming accessibility event.
     * @param windowsChanged True if triggered by [AccessibilityEvent.TYPE_WINDOWS_CHANGED].
     * @param trackedApp Currently active tracked session app, if any.
     * @param foregroundApp Last known foreground target app.
     * @param activeCover Currently displayed cover, allowing detectors to keep an occluded player covered.
     */
    fun scan(
        eventPackage: String?,
        windowsChanged: Boolean,
        trackedApp: ResolvedBlockableApp?,
        foregroundApp: ResolvedBlockableApp?,
        activeCover: ContentCover? = null,
    ): Result {
        val appWindows = AppWindows(service.windows)
        val exited = trackedApp != null && !appWindows.isEligible(trackedApp)
        // TYPE_WINDOWS_CHANGED events often omit package info; fall back to the top window package.
        val packageId = if (windowsChanged) appWindows.foregroundPackage.orEmpty() else eventPackage.orEmpty()
        val activeApp = resolveForegroundBrainRotApp(packageId, appWindows, foregroundApp.takeUnless { exited })
        val remainingTrackedApp = trackedApp.takeUnless { exited }
        if (activeApp == null && remainingTrackedApp == null) return Result(null, exited, true, null)

        // A window-attached cover may be the active root; read its parent application instead.
        val root = if (useWindowAttachedCover && activeApp?.coverDetector != null) {
            appWindows.roots.values.firstOrNull { it?.packageName?.toString() == activeApp.packageId }
        } else {
            service.rootInActiveWindow
        }

        // Prioritize activeApp.packageId so events from system UI or keyboards don't skip detection.
        val targetPackageId = activeApp?.packageId ?: packageId
        return Result(
            activeApp, exited, root != null,
            root?.let {
                detectBlockedContent(targetPackageId, it, appWindows, remainingTrackedApp, activeCover)
            },
        )
    }

    private fun resolveForegroundBrainRotApp(
        packageId: String,
        appWindows: AppWindows,
        currentForegroundBrainRotApp: ResolvedBlockableApp?,
    ): ResolvedBlockableApp? {

        val eventApp = BlockableApp.entries.firstNotNullOfOrNull { app ->
            app.resolvePackage(packageId)?.let { ResolvedBlockableApp(app, it) }
        }
        // Keyboard/system events may arrive while the tracked app remains visible.
        return eventApp?.takeIf(appWindows::isVisible)
            ?: currentForegroundBrainRotApp?.takeIf(appWindows::isVisible)
    }

    private fun detectBlockedContent(
        packageId: String,
        rootNode: AccessibilityNodeInfo,
        appWindows: AppWindows,
        trackedApp: ResolvedBlockableApp?,
        activeCover: ContentCover? = null,
    ): DetectedBlockedContent? {
        // Check each app/root pair once, including variants that share a package.
        val apps = buildList {
            trackedApp?.let(::add)
            BlockableApp.entries.forEach { app -> app.resolvePackage(packageId)?.let { add(ResolvedBlockableApp(app, it)) } }
        }.distinct()
        val roots = (listOf(rootNode) + appWindows.roots.values.filterNotNull()).distinct()
        return apps.firstNotNullOfOrNull { app ->
            // Only an existing session may fall back behind a keyboard or another active window.
            val candidates = if (app == trackedApp) roots else listOf(rootNode)
            candidates.firstNotNullOfOrNull { it.detectContent(app, appWindows, activeCover) }
        }
    }

    private fun AccessibilityNodeInfo.detectContent(
        blockableApp: ResolvedBlockableApp,
        appWindows: AppWindows,
        activeCover: ContentCover? = null,
    ): DetectedBlockedContent? {
        if (packageName?.toString() != blockableApp.packageId || !appWindows.isEligible(blockableApp)) return null
        // A matching region uses a cover; otherwise keep this app's normal screen detector.
        val cover = detectContentCover(blockableApp, appWindows, activeCover)
        // A cover-only app must provide a rectangle. Never guess a region or press Back instead.
        if (cover == null &&
            (blockableApp.getBlockAction() == ContentBlockAction.CoverVideoRegion || !matchesBlockedContent(blockableApp))
        ) return null
        return DetectedBlockedContent(
            app = blockableApp,
            blockingSuppressed = shouldSuppressBlocking(blockableApp),
            cover = cover,
        )
    }

    private fun AccessibilityNodeInfo.detectContentCover(
        app: ResolvedBlockableApp,
        appWindows: AppWindows,
        activeCover: ContentCover? = null,
    ): ContentCover? {
        val detector = app.coverDetector ?: return null
        val activeBounds = activeCover?.target?.takeIf { target ->
            when (target) {
                is ContentCoverTarget.Window -> target.windowId == windowId
                is ContentCoverTarget.Screen -> !useWindowAttachedCover
            }
        }?.bounds
        val bounds = detector.coverBounds(coverNodes(app, detector.requiredViewIds), activeBounds) ?: return null
        // Older Android positions covers on the screen. Android 14+ attaches them to an app window.
        val target = if (useWindowAttachedCover) {
            val targetWindow = appWindows.roots.keys.firstOrNull { it.id == windowId } ?: return null
            ContentCoverTarget.Window(windowId, targetWindow.displayId, bounds)
        } else {
            ContentCoverTarget.Screen(bounds)
        }
        return ContentCover(target, detector.titleRes, detector.descriptionRes)
    }

    /** Looks for blocked content across all visible windows of [blockableApp]. */
    fun findVisibleBlockedContent(blockableApp: ResolvedBlockableApp, activeCover: ContentCover? = null): DetectedBlockedContent? {
        val appWindows = AppWindows(service.windows)
        return appWindows.roots.values.firstNotNullOfOrNull { it?.detectContent(blockableApp, appWindows, activeCover) }
    }

    /** Returns true if any window of [app] is currently open on screen. */
    fun isBlockedAppPackageVisible(app: ResolvedBlockableApp): Boolean = AppWindows(service.windows).isVisible(app)

    /** Makes sure [app] is still in the foreground before showing an overlay over it. */
    fun isContentWindowEligible(app: ResolvedBlockableApp): Boolean =
        app.coverDetector == null || AppWindows(service.windows).isEligible(app)

    /**
     * Captures a synchronous snapshot of application windows.
     * Never retain this across suspension or between events.
     */
    private class AppWindows(windows: List<AccessibilityWindowInfo>) {
        // Map application windows to their root accessibility nodes; ignores system bars and overlays.
        val roots = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }.associateWith { it.root }

        // Determine which package currently has user focus or active interaction.
        val foregroundPackage = foregroundAppPackage(
            roots.map { (window, root) ->
                InteractiveWindowState(
                    packageName = root?.packageName?.toString(),
                    isApplication = true,
                    isActive = window.isActive,
                    isFocused = window.isFocused,
                )
            },
        )

        // For apps using video covers, the app must actually be in the foreground to show overlays.
        fun isEligible(app: ResolvedBlockableApp): Boolean = app.coverDetector == null || foregroundPackage == app.packageId

        // Checks if an application window for this app is present on the screen.
        fun isVisible(app: ResolvedBlockableApp): Boolean = if (app.coverDetector != null) {
            isEligible(app)
        } else {
            roots.values.any { it?.packageName?.toString() == app.packageId }
        }
    }

    // Read only the IDs requested by this app's detector; do not walk the whole screen tree.
    private fun AccessibilityNodeInfo.coverNodes(app: ResolvedBlockableApp, viewIds: Set<String>): List<ContentCoverNode> =
        viewIds.flatMap { id ->
            findAccessibilityNodeInfosByViewId("${app.packageId}:id/$id").map { node ->
                ContentCoverNode(id, node.coverBounds(), node.isVisibleToUser)
            }
        }

    // The rectangle must use the same origin as the overlay that will draw it.
    private fun AccessibilityNodeInfo.coverBounds(): ContentBounds {
        val bounds = android.graphics.Rect()
        if (useWindowAttachedCover) {
            getBoundsInWindow(bounds)
        } else {
            getBoundsInScreen(bounds)
        }
        return ContentBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    /** Checks if the screen matches a known video layout, view ID, or label for [blockableApp]. */
    private fun AccessibilityNodeInfo.matchesBlockedContent(blockableApp: ResolvedBlockableApp): Boolean {
        val detectionMethod = blockableApp.getDetectionMethod()

        // View IDs are indexed by Android, so use the platform lookup instead of walking the tree.
        if (detectionMethod is DetectionMethod.ViewId) {
            return findAccessibilityNodeInfosByViewId(blockableApp.getViewId(detectionMethod)).any(::isNodeVisibleToTheUser)
        }

        return matchesComplexBlockedContent(blockableApp)
    }

    // Only Instagram has DM-screen detection here so far; other apps do not use this exemption yet.
    private fun AccessibilityNodeInfo.shouldSuppressBlocking(blockableApp: ResolvedBlockableApp): Boolean {
        return blockableApp.app == BlockableApp.REELS &&
            currentAllowVideosSentByDm &&
            isInstagramReelSentInDm(blockableApp)
    }

    private fun AccessibilityNodeInfo.isInstagramReelSentInDm(blockableApp: ResolvedBlockableApp): Boolean {
        val senderUsernameId = blockableApp.getViewId(DetectionMethod.ViewId(INSTAGRAM_DM_SENDER_USERNAME_VIEW_ID))
        val senderTimestampId = blockableApp.getViewId(DetectionMethod.ViewId(INSTAGRAM_DM_SENDER_TIMESTAMP_VIEW_ID))
        val replyBarId = blockableApp.getViewId(DetectionMethod.ViewId(INSTAGRAM_DM_REPLY_BAR_VIEW_ID))
        val suggestedTitleId = blockableApp.getViewId(DetectionMethod.ViewId(INSTAGRAM_SUGGESTED_TITLE_VIEW_ID))

        // Sender details and a reply bar identify the DM viewer; recommendations are not DM videos.
        return hasVisibleViewId(senderUsernameId) &&
            hasVisibleViewId(senderTimestampId) &&
            hasVisibleViewId(replyBarId) &&
            !hasVisibleViewId(suggestedTitleId)
    }

    private fun AccessibilityNodeInfo.hasVisibleViewId(viewId: String): Boolean {
        return findAccessibilityNodeInfosByViewId(viewId).any(::isNodeVisibleToTheUser)
    }

    /**
     * Scans the view hierarchy for complex layout patterns. Returns immediately if a fast rule matches.
     */
    private fun AccessibilityNodeInfo.matchesComplexBlockedContent(blockableApp: ResolvedBlockableApp): Boolean {
        val structuralNodes = mutableListOf<DetectionNode>()
        val structuralClassNames = blockableApp.getStructuralClassNames()
        val nodesToVisit = ArrayDeque<Pair<AccessibilityNodeInfo, Int?>>()
        val rootBounds = android.graphics.Rect().also(::getBoundsInScreen)
        var nextStructuralNodeId = 0
        nodesToVisit.add(this to null)

        while (nodesToVisit.isNotEmpty()) {
            val (node, parentStructuralNodeId) = nodesToVisit.removeFirst()
            val isVisible = isNodeVisibleToTheUser(node)
            var structuralNodeId: Int? = null

            // Invisible nodes cannot match any rule. But still visit their children because Android can
            // expose visible descendants below an invisible accessibility wrapper.
            if (isVisible) {
                val fastNode = DetectionNode(
                    nodeId = -1,
                    viewId = node.viewIdResourceName,
                    contentDescription = node.contentDescription?.toString(),
                    isSelected = node.isSelected,
                )

                if (blockableApp.matchesFastDetectionNode(fastNode)) {
                    return true
                }

                val className = node.className?.toString()
                if (className in structuralClassNames) {
                    val nodeBounds = android.graphics.Rect().also(node::getBoundsInScreen)
                    val nodeId = nextStructuralNodeId++
                    structuralNodeId = nodeId
                    structuralNodes += DetectionNode(
                        nodeId = nodeId,
                        parentNodeId = parentStructuralNodeId,
                        className = className,
                        screenWidthFraction = nodeBounds.width().fractionOf(rootBounds.width()),
                        screenHeightFraction = nodeBounds.height().fractionOf(rootBounds.height()),
                        isScrollable = node.isScrollable,
                        isLongClickable = node.isLongClickable,
                    )
                }
            }

            // Skip irrelevant wrappers while keeping useful nodes linked to their nearest useful parent.
            val childStructuralParentId = structuralNodeId ?: parentStructuralNodeId
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { child -> nodesToVisit.addLast(child to childStructuralParentId) }
            }
        }

        return blockableApp.matchesDetectionNodes(structuralNodes)
    }

    private fun Int.fractionOf(total: Int): Float {
        return if (total > 0) (toFloat() / total).coerceIn(0f, 1f) else 0f
    }

    /** Checks if a view node is actually visible on screen with non-zero dimensions. */
    private fun isNodeVisibleToTheUser(node: AccessibilityNodeInfo): Boolean {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        return node.isVisibleToUser && rect.width() > 0 && rect.height() > 0
    }

    private companion object {
        const val INSTAGRAM_DM_SENDER_USERNAME_VIEW_ID = "sender_username_or_fullname"
        const val INSTAGRAM_DM_SENDER_TIMESTAMP_VIEW_ID = "sender_timestamp"
        const val INSTAGRAM_DM_REPLY_BAR_VIEW_ID = "reply_bar_edittext"
        const val INSTAGRAM_SUGGESTED_TITLE_VIEW_ID = "suggested_title"
    }
}
