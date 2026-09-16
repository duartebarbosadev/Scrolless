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
import android.content.ComponentName
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.ChecksSdkIntAtLeast
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.ContentBlockAction
import com.scrolless.app.core.model.DetectionMethod
import com.scrolless.app.core.model.DetectionNode
import com.scrolless.app.core.model.DmExemptionRule
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
    private val includeStories: () -> Boolean,
    private val currentActivity: () -> ComponentName? = { null },
) {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val useWindowAttachedCover
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && windowAttachedCover()

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
     * @param windowsChanged True to resolve the foreground package from the current windows, including scans without an event.
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
        val trackedAppExited = trackedApp != null && !appWindows.isEligible(trackedApp)

        // Window-change events often omit package info, and synthetic rescans have no event.
        // In both cases, resolve the package directly from the active foreground window.
        val packageId = if (windowsChanged) {
            appWindows.foregroundPackage.orEmpty()
        } else {
            eventPackage.orEmpty()
        }

        val currentForegroundApp = if (trackedAppExited) null else foregroundApp
        val activeApp = resolveForegroundBrainRotApp(packageId, appWindows, currentForegroundApp)
        val remainingTrackedApp = if (trackedAppExited) null else trackedApp

        // Neither an active app nor an ongoing tracked session is on screen.
        if (activeApp == null && remainingTrackedApp == null) {
            return Result(null, trackedAppExited, true, null)
        }

        val root = findRootNode(appWindows, activeApp)

        // Prioritize activeApp.packageId so events from system UI or keyboards don't skip detection.
        val targetPackageId = activeApp?.packageId ?: packageId
        val content = root?.let {
            detectBlockedContent(targetPackageId, it, appWindows, remainingTrackedApp, activeCover)
        }
        return Result(activeApp, trackedAppExited, root != null, content)
    }

    private fun findRootNode(appWindows: AppWindows, activeApp: ResolvedBlockableApp?): AccessibilityNodeInfo? {
        // A window-attached cover may be the active root; read its parent application instead.
        if (useWindowAttachedCover && activeApp?.coverDetector != null) {
            return appWindows.roots.values.firstOrNull { it?.packageName?.toString() == activeApp.packageId }
        }
        return service.rootInActiveWindow
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

        if (cover == null) {
            // This app requires a video cover, but we could not find the player's bounds.
            if (blockableApp.getBlockAction() == ContentBlockAction.CoverVideoRegion) return null
            // Only use the app's normal blocking action when the screen matches its detection rule.
            if (!matchesBlockedContent(blockableApp, appWindows)) return null
        }

        return DetectedBlockedContent(
            app = blockableApp,
            blockingSuppressed = shouldSuppressBlocking(blockableApp, cover),
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
        val bounds = detector.coverBounds(coverNodes(app, detector.viewIds), activeBounds) ?: return null
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

    /**
     * Verifies that [app] is in a valid state to display an overlay before attaching it.
     * Non-cover apps bypass window inspection to avoid unnecessary IPC calls; cover-based
     * apps must currently be the focused foreground package to avoid misplaced overlays.
     */
    fun isContentWindowEligible(app: ResolvedBlockableApp): Boolean =
        app.coverDetector == null || AppWindows(service.windows).isEligible(app)

    /**
     * Captures a synchronous snapshot of application windows.
     * Never retain this across suspension or between events.
     */
    private inner class AppWindows(windows: List<AccessibilityWindowInfo>) {
        // Map application windows to their root accessibility nodes; ignores system bars and overlays.
        val roots = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }.associateWith { it.root }

        /** Returns the window title and the last activity name reported for its app. */
        fun screenNames(targetWindowId: Int): List<String> = buildList {
            val (window, root) = roots.entries.firstOrNull { it.key.id == targetWindowId } ?: return@buildList
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.title?.toString()?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            currentActivity()?.takeIf { it.packageName == root?.packageName?.toString() }?.className?.let { add(it) }
        }

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

        /**
         * Determines whether [app] qualifies for content detection and overlay display.
         *
         * Apps that draw floating video covers (e.g. Reels or TikTok) must be the active foreground window
         * to avoid misplacing covers over background apps or recent-task thumbnails.
         * Apps using full-screen blocking (such as Back navigation to leave the block) do not have this restriction.
         */
        fun isEligible(app: ResolvedBlockableApp): Boolean = app.coverDetector == null || foregroundPackage == app.packageId

        /**
         * Checks whether [app] is active on screen:
         * - Cover-based apps must have focused foreground presence.
         * - Non-cover apps only require a visible application window.
         */
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
    private fun AccessibilityNodeInfo.matchesBlockedContent(blockableApp: ResolvedBlockableApp, appWindows: AppWindows): Boolean {
        return matchesDetectionMethod(blockableApp, blockableApp.getDetectionMethod(includeStories()), appWindows)
    }

    private fun AccessibilityNodeInfo.matchesDetectionMethod(
        blockableApp: ResolvedBlockableApp,
        detectionMethod: DetectionMethod,
        appWindows: AppWindows,
    ): Boolean {
        // Check the window title and the last tracked activity name. A rule such as
        // "StoryViewerActivity" can match a full name that includes the app's package prefix.
        fun matchesActivity(method: DetectionMethod.ActivityName): Boolean = appWindows.screenNames(windowId)
            .any { it.contains(method.activityName, ignoreCase = true) }

        // This rule depends only on the activity name, so no view-tree scan is needed, even if it doesn't match.
        if (detectionMethod is DetectionMethod.ActivityName) return matchesActivity(detectionMethod)

        if (detectionMethod is DetectionMethod.AnyOf) {
            // Fast-path: if any alternative is an activity rule matching the current screen, accept immediately.
            val activityMatch = detectionMethod.detectionMethods.any { it is DetectionMethod.ActivityName && matchesActivity(it) }
            if (activityMatch) return true

            // ID-only alternatives can use Android's indexed view lookup instead of walking the full tree.
            if (detectionMethod.detectionMethods.all { it is DetectionMethod.ViewId }) {
                return detectionMethod.detectionMethods.any { matchesDetectionMethod(blockableApp, it, appWindows) }
            }
        }

        // View IDs are indexed by Android, so use the platform lookup instead of walking the tree.
        if (detectionMethod is DetectionMethod.ViewId) {
            return hasVisibleViewId(blockableApp.getViewId(detectionMethod))
        }

        return matchesComplexBlockedContent(blockableApp, detectionMethod)
    }

    /**
     * Checks whether the user's DM allowance applies to this screen.
     * Skips layout checks when that setting is off or the app has no DM rule, so ordinary blocking
     * remains unchanged. [cover] supplies player bounds only for rules that check a reply's position.
     */
    private fun AccessibilityNodeInfo.shouldSuppressBlocking(blockableApp: ResolvedBlockableApp, cover: ContentCover?): Boolean {
        // Only check layout rules if the user explicitly enabled DM video allowance in settings.
        if (!allowVideosSentByDm()) return false
        val rule = blockableApp.dmExemptionRule ?: return false
        return isVideoSentInDm(blockableApp, rule, cover)
    }

    /**
     * Checks the screen against all conditions in the app's DM rule before exempting a video.
     * Required IDs must be visible, forbidden IDs must be absent, and an any-of group needs a match.
     * If reply labels are configured, a matching button must also sit below the player in [cover].
     */
    private fun AccessibilityNodeInfo.isVideoSentInDm(app: ResolvedBlockableApp, rule: DmExemptionRule, cover: ContentCover?): Boolean {
        // If an app defines no DM rules, we cannot determine DM state; fail closed (do not exempt).
        if (rule.requiredViewIds.isEmpty() && rule.anyOfViewIds.isEmpty() && rule.replyLabelsBelowPlayer == null) return false

        // If any feed-only or non-DM indicator is on screen (e.g. "suggested reels" title),
        // reject exemption immediately to prevent accidental unblocking of the feed.
        if (rule.forbiddenViewIds.any { hasVisibleViewId(app.getViewId(it)) }) return false

        // All required DM elements (e.g. sender username, reply input) must be simultaneously visible.
        // If even one is missing, this screen is not a confirmed DM video.
        if (rule.requiredViewIds.any { !hasVisibleViewId(app.getViewId(it)) }) return false

        rule.replyLabelsBelowPlayer?.let { labels ->
            if (cover == null || !hasReplyBelowPlayer(labels, cover.target.bounds) { it.coverBounds() }) return false
        }

        // If any-of elements are specified, at least one must be present on screen.
        return rule.anyOfViewIds.isEmpty() || rule.anyOfViewIds.any { hasVisibleViewId(app.getViewId(it)) }
    }

    /**
     * Finds nodes with the given fully-qualified [viewId] and verifies at least one is visible
     * to the user with positive screen dimensions.
     */
    private fun AccessibilityNodeInfo.hasVisibleViewId(viewId: String): Boolean {
        return findAccessibilityNodeInfosByViewId(viewId).any(::isNodeVisibleToTheUser)
    }

    /**
     * Scans the view hierarchy for complex layout patterns. Returns immediately if a fast rule matches.
     */
    private fun AccessibilityNodeInfo.matchesComplexBlockedContent(
        blockableApp: ResolvedBlockableApp,
        detectionMethod: DetectionMethod,
    ): Boolean {
        val structuralNodes = mutableListOf<DetectionNode>()
        val structuralClassNames = blockableApp.getStructuralClassNames(detectionMethod)
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

                if (blockableApp.matchesFastDetectionNode(fastNode, detectionMethod)) {
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

        return blockableApp.matchesDetectionNodes(structuralNodes, detectionMethod)
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
}
