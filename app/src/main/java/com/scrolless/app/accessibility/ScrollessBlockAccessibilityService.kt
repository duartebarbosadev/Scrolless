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
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.scrolless.app.BuildConfig
import com.scrolless.app.core.blocking.BlockingManager
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.model.ContentBlockAction
import com.scrolless.app.core.model.DetectionMethod
import com.scrolless.app.core.model.DetectionNode
import com.scrolless.app.core.model.ResolvedBlockableApp
import com.scrolless.app.core.repository.BlockingConfigRepository
import com.scrolless.app.core.repository.SessionTracker
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.debug.DebugOverlayConfig
import com.scrolless.app.ui.overlay.BlockedContentOverlayManager
import com.scrolless.app.ui.overlay.TimerOverlayManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Accessibility service that monitors and blocks access to "brain rot" content based on user-configured limits.
 *
 * Reads app screens, asks [BlockingManager] whether viewing is allowed, and applies the screen's action.
 * A blocked video can be covered while the rest of its app stays usable.
 * Viewing time is saved when the user leaves the video or when a cover hides it.
 *
 * TODO: Move the session time tracking logic to the SessionTrackerImpl
 *
 * - [BlockOption.BlockAll]: Immediately blocks all detected content
 * - [BlockOption.DailyLimit]: Allows usage up to a configured daily time limit
 * - [BlockOption.IntervalTimer]: Allows usage within time intervals
 * - [BlockOption.NothingSelected]: No blocking is performed
 *
 * The service tracks usage time, displays an optional timer overlay, and performs automatic
 *  back navigation when limits are exceeded.
 *
 * @see com.scrolless.app.core.blocking.BlockingManager for blocking logic
 * @see BlockOption for available blocking strategies
 * @see BlockableApp for supported apps
 * @see com.scrolless.app.core.data.repository.SessionTrackerImpl for usage tracking implementation
 */
@SuppressLint("AccessibilityPolicy") // Accessibility APIs are required to enforce user-configured blocking policies.
@AndroidEntryPoint
class ScrollessBlockAccessibilityService : AccessibilityService() {

    override fun onCreate() {
        super.onCreate()
        timerOverlayManager.attachServiceContext(this)
        blockedContentOverlayManager.attachServiceContext(this)

        if (BuildConfig.DEBUG) {
            serviceScope.launch {
                DebugOverlayConfig.forceLegacyOverlay.drop(1).collect {
                    if (contentSession?.content?.cover != null) {
                        onBlockedContentExited()
                    }
                    blockedContentOverlayManager.hide()
                }
            }
        }
    }

    /** Schedules overlay updates and covered-screen checks on the main thread. */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Keep UI work on the main thread; one failed job should not stop all service work. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Saves viewing time and records app visits. */
    @Inject
    lateinit var sessionTracker: SessionTracker

    /** Decides whether the user's current limit allows more viewing. */
    @Inject
    lateinit var blockingManager: BlockingManager

    /** The blocking mode and the settings that enforce it. */
    @Inject
    lateinit var blockingConfigRepository: BlockingConfigRepository

    /** Supplies timer preferences, pauses, and the DM-video exemption. */
    @Inject
    lateinit var userSettingsStore: UserSettingsStore

    /** Shows usage time; it does not decide whether to block. */
    @Inject
    lateinit var timerOverlayManager: TimerOverlayManager

    /** Draws the blocking message over a detected video while leaving the rest of its app usable. */
    @Inject
    lateinit var blockedContentOverlayManager: BlockedContentOverlayManager

    private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }

    private var contentSession: ContentSession? = null

    /**
     * Whether covers should be attached to the app window.
     * Android 14 added that safer path; debug builds may force the legacy path for device testing.
     */
    private val useWindowAttachedCover: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !(BuildConfig.DEBUG && DebugOverlayConfig.forceLegacyOverlay.value)
    /** The session only while viewing time is still running; covered sessions remain tracked separately. */
    private val viewingSession: ContentSession?
        get() = contentSession?.takeUnless { it.isCovered }

    /** Latest timer preference, kept ready for the next viewing session. */
    private var currentTimerOverlayEnabled: Boolean = false

    /** Allow DM videos only when an app's DM detector recognizes the screen. */
    private var currentAllowVideosSentByDm: Boolean = false

    /** Time at which the temporary pause ends. A pause still counts viewing time. */
    @Volatile
    private var pauseUntilMillis: Long = 0L

    private fun isPauseActive(now: Long = System.currentTimeMillis()): Boolean = pauseUntilMillis > now

    /** Whether a pause or the current screen's exemption should prevent blocking right now. */
    private val isBlockingSuppressed: Boolean
        get() = isPauseActive() || contentSession?.blockingSuppressed == true

    private var currentForegroundBrainRotApp: ResolvedBlockableApp? = null

    /** Check the limit without waiting for another screen event, then schedule the next check. */
    private val videoCheckRunnable: Runnable = Runnable {

        if (!validateTrackedAppState("Periodic check")) {
            return@Runnable
        }

        if (contentSession == null) {
            Timber.v("Periodic check runnable executed but no longer processing content")
            return@Runnable
        }

        val session = viewingSession ?: return@Runnable
        val elapsed = System.currentTimeMillis() - session.startedAtMillis

        if (isBlockingSuppressed) {
            // While paused or suppressed, continue scheduling so usage and timer overlay stay current.
            Timber.v(
                "Periodic check skipped: elapsed=%d ms, paused=%b, suppressed=%b",
                elapsed,
                isPauseActive(),
                session.blockingSuppressed,
            )
            mainHandler.postDelayed(videoCheckRunnable, 1000)
        } else {
            Timber.v("Periodic check running: elapsed=%d ms", elapsed)
            serviceScope.launch {
                val action = blockingManager.onPeriodicCheck(elapsed)
                if (viewingSession !== session) return@launch
                when (action) {
                    is BlockingResult.BlockNow -> {
                        Timber.i("Periodic check: limit reached (elapsed=%d). Navigating back.", elapsed)
                        enforceBlocking(session)
                    }

                    is BlockingResult.CheckLater -> {
                        Timber.v("Periodic check: limit not reached, will check again later in %d ms", action.delayMillis)
                        mainHandler.postDelayed(videoCheckRunnable, action.delayMillis)
                    }

                    is BlockingResult.Continue -> {
                        Timber.v("Periodic check: limit not reached, scheduling next check")
                        mainHandler.postDelayed(videoCheckRunnable, 10000)
                    }
                }
            }
        }
    }

    /** Start watching settings so changes also apply to content that is already open. */
    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.i("Accessibility service connected")

        // Start with restricted configuration to save battery
        refreshServiceConfig()

        // The user may have just enabled accessibility in Android Settings; bring them back once.
        serviceScope.launch {
            val waitingForAccessibility = userSettingsStore.getWaitingForAccessibility().distinctUntilChanged()
            waitingForAccessibility.collect { waiting ->
                // If app is waiting for accessibility, bring it to foreground
                if (waiting) {
                    Timber.i("Bringing app to foreground")
                    bringAppToForeground()
                    userSettingsStore.setWaitingForAccessibility(false)
                } else {
                    Timber.i("Skipping bringing app to foreground")
                }
            }
        }

        // Observe changes to the block config
        serviceScope.launch {
            // Usage changes constantly; only the mode and its settings need a new handler.
            blockingConfigRepository.observeConfig()
                .map { it.activeOption to it.settings }
                .distinctUntilChanged()
                .collect { (option, settings) ->
                    Timber.d("Blocking option changed: %s", option)
                    blockingManager.init(option, settings)
                    reconsiderVisibleContent()
                }
        }

        // Observe timer overlay enabled changes
        serviceScope.launch {
            userSettingsStore.getTimerOverlayEnabled().collect { currentTimerOverlayEnabled = it }
        }

        // Re-detect an open video when the DM preference changes, then apply the new decision.
        serviceScope.launch {
            userSettingsStore.getAllowVideosSentByDm().collect {
                currentAllowVideosSentByDm = it
                refreshDetectedContent()
                reconsiderVisibleContent()
            }
        }

        // Apply a pause immediately, even if the app has not sent a new screen event.
        serviceScope.launch {
            userSettingsStore.getPauseUntil().collect { newPauseUntil ->
                pauseUntilMillis = newPauseUntil
                reconsiderVisibleContent()
            }
        }
    }

    /** End tracking when the app leaves or the screen turns off. Repeated exit events are harmless. */
    private fun handleTrackedAppExit(reason: String) {
        // A window cover can outlive tracking while its parent animates away. Screen-off clears it.
        if (!powerManager.isInteractive) blockedContentOverlayManager.hide()
        if (contentSession == null && currentForegroundBrainRotApp == null) {
            return
        }

        Timber.i("Handling tracked app exit: %s", reason)
        if (contentSession != null) {
            onBlockedContentExited(appLeft = true)
        }

        if (currentForegroundBrainRotApp != null) {
            updateForegroundAppState(null)
        }
    }

    /** Stop delayed work if the screen turned off or the app is no longer visible. */
    private fun validateTrackedAppState(source: String): Boolean {
        if (!powerManager.isInteractive) {
            if (contentSession != null || currentForegroundBrainRotApp != null) {
                handleTrackedAppExit("$source - screen is off")
            }
            return false
        }

        val trackedForegroundApp = currentForegroundBrainRotApp ?: return true
        if (isBlockedAppPackageVisible(trackedForegroundApp)) {
            return true
        }

        handleTrackedAppExit("$source - tracked app package is not visible")
        return false
    }

    /** Listen beyond the target app while it is open, so Home and app-switching events reach us. */
    private fun refreshServiceConfig() {
        updateServiceConfig(contentSession != null || currentForegroundBrainRotApp != null)
    }

    /** Use screen changes to start, update, or finish tracking. These events can arrive very often. */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Skip processing if screen is off
        if (!powerManager.isInteractive) {
            handleTrackedAppExit("screen is off while receiving accessibility event")
            return
        }

        val appWindows = AppWindows(windows)

        // End tracking on focus loss. Legacy covers disappear; attached covers stay on their app.
        contentSession?.let { session ->
            if (!appWindows.isEligible(session.app)) {
                handleTrackedAppExit("covered app lost foreground")
            }
        }

        // Window-list events have no reliable source package, including when returning to a covered app.
        val packageId = if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            appWindows.foregroundPackage.orEmpty()
        } else {
            event.packageName?.toString().orEmpty()
        }
        val userActiveApp = resolveForegroundBrainRotApp(packageId, appWindows)
        updateForegroundAppState(userActiveApp)

        // For unrelated apps, avoid touching the accessibility tree unless we're already tracking
        // blocked content.
        if (userActiveApp == null && contentSession == null) {
            return
        }

        // Android may return our cover as the active window. Read the app behind it instead.
        val rootNode = if (useWindowAttachedCover &&
            userActiveApp?.coverDetector != null
        ) {
            appWindows.roots.values.firstOrNull { it?.packageName?.toString() == userActiveApp.packageId }
        } else {
            rootInActiveWindow
        }
        if (rootNode == null) {
            validateTrackedAppState("Root node missing")
            Timber.v("No root node available, skipping content detection")
            return
        }

        val detectedContent = detectBlockedContent(packageId, rootNode, appWindows)

        // No match means the user left the video, even if they stayed inside the same app.
        if (detectedContent != null) {
            onBlockedContentDetected(detectedContent)
        } else {
            if (contentSession != null) {
                Timber.v("Brain rot content no longer detected, triggering exit")
                onBlockedContentExited()
            }
            // Returning directly to an allowed screen must also clear a retained window cover.
            if (userActiveApp?.coverDetector != null) {
                blockedContentOverlayManager.hide()
            }
        }
    }

    private fun updateForegroundAppState(nextApp: ResolvedBlockableApp?) {
        val previousApp = currentForegroundBrainRotApp
        if (previousApp == nextApp) return

        if (previousApp != null) {
            Timber.v("*** User appears to have left a brain rot app: %s (%s)", previousApp.app.name, previousApp.packageId)
            sessionTracker.onAppClose()
        }

        if (nextApp != null) {
            Timber.v("**** User appears to have entered a brain rot app: %s (%s)", nextApp.app.name, nextApp.packageId)
            sessionTracker.onAppOpen(nextApp.app)
        }

        currentForegroundBrainRotApp = nextApp
        refreshServiceConfig()
    }

    private fun resolveForegroundBrainRotApp(packageId: String, appWindows: AppWindows): ResolvedBlockableApp? {

        val brainRotApp = BlockableApp.entries.firstNotNullOfOrNull { appEnum ->
            appEnum.resolvePackage(packageId)?.let { matchedPackage ->
                ResolvedBlockableApp(appEnum, matchedPackage)
            }
        }

        if (brainRotApp != null) {
            if (appWindows.isVisible(brainRotApp)) {
                return brainRotApp
            }
            if (currentForegroundBrainRotApp == brainRotApp) {
                Timber.v(
                    "Event came from %s (%s) but package is not visible in interactive windows, ignoring",
                    brainRotApp.app.name,
                    brainRotApp.packageId,
                )
            }
        }

        // A keyboard or system event does not necessarily mean the user left the current app.
        currentForegroundBrainRotApp?.let { blockableApp ->
            if (appWindows.isVisible(blockableApp)) {
                return blockableApp
            } else {
                Timber.v("Blocked app package is no longer visible, treating as exit")
            }
        }
        return null
    }

    /**
     * Called when the accessibility service is interrupted.
     */
    override fun onInterrupt() = Unit

    /** Remove both overlays and cancel pending work when Android destroys the service. */
    override fun onDestroy() {
        super.onDestroy()
        Timber.d(
            "Service state at destroy: hasContentSession=%b, viewingApp=%s",
            contentSession != null, viewingSession?.app,
        )
        stopPeriodicCheck()
        mainHandler.removeCallbacks(coveredContentCheck)
        blockedContentOverlayManager.hide()
        timerOverlayManager.cleanup()
        serviceScope.cancel()
    }

    private fun detectBlockedContent(packageId: String, rootNode: AccessibilityNodeInfo, appWindows: AppWindows): DetectedBlockedContent? {
        // Check each app/root pair once, including variants that share a package.
        val trackedApp = contentSession?.app
        val apps = buildList {
            trackedApp?.let(::add)
            BlockableApp.entries.forEach { app -> app.resolvePackage(packageId)?.let { add(ResolvedBlockableApp(app, it)) } }
        }.distinct()
        val roots = (listOf(rootNode) + appWindows.roots.values.filterNotNull()).distinct()
        return apps.firstNotNullOfOrNull { app ->
            // Only an existing session may fall back behind a keyboard or another active window.
            val candidates = if (app == trackedApp) roots else listOf(rootNode)
            candidates.firstNotNullOfOrNull { it.detectContent(app, appWindows) }
        }
    }

    private fun AccessibilityNodeInfo.detectContent(blockableApp: ResolvedBlockableApp, appWindows: AppWindows): DetectedBlockedContent? {
        if (packageName?.toString() != blockableApp.packageId || !appWindows.isEligible(blockableApp)) return null
        // A matching region uses a cover; otherwise keep this app's normal screen detector.
        val cover = detectContentCover(blockableApp, appWindows)
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

    private fun AccessibilityNodeInfo.detectContentCover(app: ResolvedBlockableApp, appWindows: AppWindows): ContentCover? {
        val detector = app.coverDetector ?: return null
        val bounds = detector.coverBounds(coverNodes(app, detector.requiredViewIds)) ?: return null
        // Older Android positions covers on the screen. Android 14+ attaches them to an app window.
        val target = if (useWindowAttachedCover) {
            val targetWindow = appWindows.roots.keys.firstOrNull { it.id == windowId } ?: return null
            ContentCoverTarget.Window(windowId, targetWindow.displayId, bounds)
        } else {
            ContentCoverTarget.Screen(bounds)
        }
        return ContentCover(target, detector.titleRes, detector.descriptionRes)
    }

    private fun findVisibleBlockedContent(blockableApp: ResolvedBlockableApp): DetectedBlockedContent? {
        val appWindows = AppWindows(windows)
        return appWindows.roots.values.firstNotNullOfOrNull { it?.detectContent(blockableApp, appWindows) }
    }

    private fun isBlockedAppPackageVisible(app: ResolvedBlockableApp): Boolean = AppWindows(windows).isVisible(app)

    // Async blocking decisions must check fresh windows before drawing a cover.
    private fun isContentWindowEligible(app: ResolvedBlockableApp): Boolean =
        app.coverDetector == null || AppWindows(windows).isEligible(app)

    /** One synchronous scan shares its window roots and foreground decision. Never retain this across suspension. */
    private class AppWindows(windows: List<AccessibilityWindowInfo>) {
        val roots = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }.associateWith { it.root }
        val foregroundPackage = foregroundAppPackage(
            roots.map { (window, root) ->
                InteractiveWindowState(root?.packageName?.toString(), true, window.isActive, window.isFocused)
            },
        )

        fun isEligible(app: ResolvedBlockableApp): Boolean = app.coverDetector == null || foregroundPackage == app.packageId

        fun isVisible(app: ResolvedBlockableApp): Boolean = if (app.coverDetector != null) {
            isEligible(app)
        } else {
            roots.values.any { it?.packageName?.toString() == app.packageId }
        }
    }

    /** Reuse the current session when only the layout changed; do not restart its timer on every event. */
    private fun onBlockedContentDetected(detectedContent: DetectedBlockedContent) {
        val activeSession = contentSession
        if (activeSession != null) {
            // A cover and a Back action need separate sessions, even inside the same app.
            if (!activeSession.content.canContinueWith(detectedContent)) {
                onBlockedContentExited()
            } else {
                val suppressionChanged = activeSession.blockingSuppressed != detectedContent.blockingSuppressed
                activeSession.content = detectedContent
                if (activeSession.isCovered && detectedContent.cover?.let { blockedContentOverlayManager.show(it) } != true) {
                    // The failed update removed the cover. Start a new viewing period below.
                    onBlockedContentExited()
                } else {
                    if (suppressionChanged) reconsiderVisibleContent()
                    return
                }
            }
        }

        contentSession = ContentSession(detectedContent, System.currentTimeMillis())
        onBlockedContentEntered()
    }

    /** Check the limit immediately. If viewing is allowed, show the timer and keep checking. */
    private fun onBlockedContentEntered() {
        val session = viewingSession ?: return
        Timber.d("Entered blocked content at %d (app=%s, paused=%b)", session.startedAtMillis, session.app, isPauseActive())

        // Expand service scope to detect when user leaves the app (e.g. to launcher)
        refreshServiceConfig()

        startPeriodicCheck()

        serviceScope.launch {
            // Let the blocking manager start its session even during a pause or DM exemption.
            val shouldBlock = blockingManager.onEnterBlockedContent()
            // Reading the limit may take time. Ignore the answer if the user has already left.
            if (viewingSession !== session) return@launch
            if (!isBlockingSuppressed && shouldBlock) {
                Timber.i("Blocking on enter")
                enforceBlocking(session)
                return@launch
            }

            // Viewing is allowed now, so remove any cover kept from an earlier visit.
            if (session.app.coverDetector != null) {
                blockedContentOverlayManager.hide()
            }

            showTimerOverlayIfEnabled(session)

            if (isBlockingSuppressed) {
                Timber.d(
                    "Skipping blocking check on enter, but tracking usage (paused=%b, suppressed=%b)",
                    isPauseActive(),
                    session.blockingSuppressed,
                )
            } else {
                Timber.d("Content allowed on enter, will monitor usage")
            }
        }
    }

    private suspend fun showTimerOverlayIfEnabled(session: ContentSession) {
        if (!currentTimerOverlayEnabled) return

        val config = blockingConfigRepository.getConfig()
        val showOverlay: () -> Unit
        // Interval mode shows usage within the active interval; other modes show today's total.
        if (config.activeOption == BlockOption.IntervalTimer) {
            showOverlay = {
                timerOverlayManager.showInterval(
                    sessionStartAt = session.startedAtMillis,
                    intervalUsage = config.intervalUsage,
                    intervalLengthMillis = config.settings.intervalLengthMillis,
                )
            }
        } else {
            val dailyUsageMillis = sessionTracker.getDailyUsage()
            showOverlay = {
                timerOverlayManager.showDaily(
                    sessionStartAt = session.startedAtMillis,
                    dailyUsageMillis = dailyUsageMillis,
                )
            }
        }

        // The settings lookup may finish after navigation. Only show the timer for this same session.
        mainHandler.post {
            if (
                currentTimerOverlayEnabled &&
                viewingSession === session
            ) {
                Timber.v("Showing timer overlay")
                showOverlay()
            }
        }
    }

    /** Clear tracking and save any remaining viewing time. A covered video's viewing time was already saved. */
    private fun onBlockedContentExited(appLeft: Boolean = false) {
        val session = contentSession
        contentSession = null
        val hadCover = session?.content?.cover != null
        val finished = session?.finish(System.currentTimeMillis())
        stopPeriodicCheck()
        mainHandler.removeCallbacks(coveredContentCheck)
        // Keep an attached cover during app switching, but remove it when changing screens in the app.
        if (hadCover) {
            if (appLeft) {
                blockedContentOverlayManager.onAppExit(powerManager.isInteractive)
            } else {
                blockedContentOverlayManager.hide()
            }
        }
        refreshServiceConfig()
        if (finished == null) return // A covered player already finished its viewing session.
        timerOverlayManager.hide(finished.startedAtMillis, finished.endedAtMillis)
        serviceScope.launch { saveViewing(finished) }
    }

    private suspend fun saveViewing(finished: FinishedViewing) {
        if (finished.durationMillis > 0L) {
            sessionTracker.addToDailyUsage(finished.durationMillis, finished.app.app)
        }
        // Even a video blocked immediately must close the session that the manager opened.
        blockingManager.onExitBlockedContent(finished.startedAtMillis, finished.endedAtMillis)
    }

    /** Start with one check after a second; later checks use the blocking manager's suggested delay. */
    private fun startPeriodicCheck() {
        Timber.d("Starting periodic usage checks (every 1 second)")
        mainHandler.removeCallbacks(videoCheckRunnable)
        mainHandler.postDelayed(videoCheckRunnable, 1000)
    }

    /** Cancel the next viewing check so it cannot keep running after the user leaves. */
    private fun stopPeriodicCheck() {
        mainHandler.removeCallbacks(videoCheckRunnable)
        Timber.d("Stopped periodic usage checks")
    }

    /** Stops viewing time at the moment the video is covered, not when the user later changes tabs. */
    private suspend fun enforceBlocking(session: ContentSession) {
        if (viewingSession !== session || isBlockingSuppressed) return
        when (val action = session.content.blockAction) {
            is ContentBlockAction.PerformGlobalAction -> performGlobalAction(action.action)

            ContentBlockAction.CoverVideoRegion -> {
                // The user may have switched apps while we were waiting for the blocking decision.
                if (!isContentWindowEligible(session.app)) {
                    handleTrackedAppExit("app lost foreground before covering")
                    return
                }
                val cover = session.content.cover ?: return
                // Returning from Recents can replace the app's drawing surface without resizing it.
                // Reattach now; normal screen updates can keep using that attachment.
                if (!blockedContentOverlayManager.show(cover, refreshAttachment = true)) {
                    startPeriodicCheck()
                    return
                }
                // Stop counting only after the cover was shown successfully.
                val finished = session.cover(System.currentTimeMillis()) ?: return
                stopPeriodicCheck()
                timerOverlayManager.dismissImmediately()
                refreshServiceConfig()
                saveViewing(finished)
                scheduleCoveredContentCheck()
            }
        }
    }

    // Covered time does not count. Still check for screen changes, a new allowance, or a pause.
    private val coveredContentCheck = Runnable {
        if (validateTrackedAppState("Covered screen")) {
            refreshDetectedContent()
            reconsiderVisibleContent()
        }
    }

    private fun scheduleCoveredContentCheck() {
        mainHandler.removeCallbacks(coveredContentCheck)
        if (contentSession?.isCovered == true) {
            mainHandler.postDelayed(coveredContentCheck, 1000)
        }
    }

    private fun refreshDetectedContent() {
        val app = contentSession?.app ?: return
        val content = findVisibleBlockedContent(app)
        if (content == null) onBlockedContentExited() else onBlockedContentDetected(content)
    }

    /** Reapply the current settings without making the user leave and reopen the video. */
    private fun reconsiderVisibleContent() {
        val current = contentSession ?: return
        if (!isContentWindowEligible(current.app)) {
            handleTrackedAppExit("app lost foreground before reconsidering content")
            return
        }
        serviceScope.launch {
            if (!current.isCovered) {
                if (!isBlockingSuppressed &&
                    blockingManager.onPeriodicCheck(System.currentTimeMillis() - current.startedAtMillis) == BlockingResult.BlockNow
                ) {
                    enforceBlocking(current)
                }
            } else {
                val now = System.currentTimeMillis()
                val shouldBlock = blockingManager.onEnterBlockedContent()
                // Ask whether viewing could start again. If not, close this check with zero usage.
                if (contentSession !== current || (shouldBlock && !isBlockingSuppressed)) {
                    blockingManager.onExitBlockedContent(now, now)
                    scheduleCoveredContentCheck()
                    return@launch
                }
                // Start counting from now, not from when the cover first appeared.
                val viewing = ContentSession(current.content, System.currentTimeMillis())
                contentSession = viewing
                mainHandler.removeCallbacks(coveredContentCheck)
                blockedContentOverlayManager.hide()
                refreshServiceConfig()
                startPeriodicCheck()
                showTimerOverlayIfEnabled(viewing)
            }
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

    /** Watch all app changes while tracking; ignore unrelated apps when there is nothing to track. */
    private fun updateServiceConfig(listenToAll: Boolean) {
        val info = serviceInfo ?: return

        // Do not delay Home/app-switching events while covered, or the old screen cover can linger.
        info.notificationTimeout = if (contentSession?.isCovered == true) 0L else 250L

        if (listenToAll) {
            info.packageNames = null // Listen to all
            Timber.d("Expanded service configuration to listen to all packages")

            // Ensure windows are available for visibility-based exit checks.
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        } else {
            info.packageNames = BlockableApp.entries.flatMap { it.getPackageIds() }.toTypedArray()
            Timber.d("Restricted service configuration to target packages only")
        }
        serviceInfo = info
    }

    /** Return to Scrolless after the user enables accessibility, reusing its existing screen if possible. */
    private fun bringAppToForeground() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            Timber.i("Successfully launched app to foreground")
        } catch (e: Exception) {
            Timber.e(e, "Failed to bring app to foreground")
        }
    }

    /** Default detection for screens without a cover: look for a known ID, label, or video layout. */
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
     * Scan the screen once. Return as soon as a simple label matches.
     * For layout rules, keep only the relevant nodes and how they are nested.
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

    /** Apps keep hidden views in their trees. Only visible views with a non-empty rectangle count here. */
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
