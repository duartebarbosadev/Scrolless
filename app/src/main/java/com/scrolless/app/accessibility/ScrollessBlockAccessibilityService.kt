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
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import com.scrolless.app.BuildConfig
import com.scrolless.app.core.blocking.BlockingManager
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.model.ContentBlockAction
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
 * Accessibility service that monitors and blocks access to distracting short-form video content
 * based on user-configured limits.
 *
 * Using Android's Accessibility framework, this service detects when the user enters specific apps
 * or screens (such as YouTube Shorts, Instagram Reels, or TikTok) and enforces blocking policies
 * according to the active [BlockOption]:
 * - [BlockOption.BlockAll]: Immediately blocks all detected content.
 * - [BlockOption.DailyLimit]: Allows usage up to a configured daily time limit.
 * - [BlockOption.IntervalTimer]: Allows usage within scheduled intervals.
 * - [BlockOption.NothingSelected]: No blocking is performed.
 *
 * Depending on the app, blocking is enforced either by:
 * - Performing automatic Back navigation to exit full-screen content, or
 * - Displaying a cover overlay over the video player (e.g. TikTok) while keeping
 *   the rest of the app (like Inbox and Profile) usable.
 *
 * The service tracks active viewing time, displays an optional timer overlay, and saves completed
 * sessions to the user's usage history.
 *
 * Permissions Required:
 * - Accessibility service permission must be granted by the user.
 *
 * @see BlockingManager for blocking logic and evaluation
 * @see BlockOption for available blocking strategies
 * @see BlockableApp for supported apps
 * @see ContentScanner for screen and window scanning
 * @see BlockedContentOverlayManager for overlay presentation
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

    /** Main thread handler for running UI-related actions like overlays and navigation. */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Coroutine scope for service operations. Uses SupervisorJob so child failures don't cancel the service. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Tracks daily and session usage time across monitored apps. */
    @Inject
    lateinit var sessionTracker: SessionTracker

    /** Evaluates blocking rules and decides when to block or allow content. */
    @Inject
    lateinit var blockingManager: BlockingManager

    /** Repository for active blocking configurations and rules. */
    @Inject
    lateinit var blockingConfigRepository: BlockingConfigRepository

    /** Store for user settings, pause durations, and app preferences. */
    @Inject
    lateinit var userSettingsStore: UserSettingsStore

    /** Manages the floating timer overlay that shows remaining or elapsed time. */
    @Inject
    lateinit var timerOverlayManager: TimerOverlayManager

    /** Manages the blocker overlay placed directly over distracting video players. */
    @Inject
    lateinit var blockedContentOverlayManager: BlockedContentOverlayManager

    private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }

    /** Currently active content session, including viewing state and any attached cover. */
    private var contentSession: ContentSession? = null

    /** Scans the window tree to find target apps, videos, and compute cover coordinates. */
    private val contentScanner by lazy {
        ContentScanner(this, { isWindowAttachedCoverAllowed }, { currentAllowVideosSentByDm })
    }

    /**
     * Whether covers should attach directly to app windows (Android 14+), or use screen overlays (debug fallback).
     */
    private val isWindowAttachedCoverAllowed: Boolean
        get() = !(BuildConfig.DEBUG && DebugOverlayConfig.forceLegacyOverlay.value)

    /** Active viewing session where usage time is currently running (not yet covered). */
    private val viewingSession: ContentSession?
        get() = contentSession?.takeUnless { it.isCovered }

    /** Whether the user enabled the floating timer overlay in settings. */
    private var currentTimerOverlayEnabled: Boolean = false

    /** Whether videos received in direct messages are exempt from blocking. */
    private var currentAllowVideosSentByDm: Boolean = false

    /** Timestamp until which blocking is temporarily paused. */
    @Volatile
    private var pauseUntilMillis: Long = 0L

    private fun isPauseActive(now: Long = System.currentTimeMillis()): Boolean = pauseUntilMillis > now

    /** True if blocking is currently skipped due to an active pause or DM exemption. */
    private val isBlockingSuppressed: Boolean
        get() = isPauseActive() || contentSession?.blockingSuppressed == true

    /** Currently tracked target app in the foreground, or null if unrelated. */
    private var currentForegroundBrainRotApp: ResolvedBlockableApp? = null

    /** Periodically checks if the user exceeded their usage limit while watching blocked content. */
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

    /**
     * Called when the accessibility service connects and is ready to monitor apps.
     * Sets up package listeners and begins observing settings and blocking preferences.
     */
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

    /**
     * Cleans up tracking and overlays when the user leaves a monitored app or turns off the screen.
     *
     * @param reason Diagnostic message for logs explaining why the session ended.
     */
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

    /**
     * Confirms the tracked app is still in the foreground and the screen is interactive.
     *
     * @param source Name of the caller (event, periodic check, etc.) for logging.
     * @return True if tracking can safely continue, or false if the app exited.
     */
    private fun validateTrackedAppState(source: String): Boolean {
        if (!powerManager.isInteractive) {
            if (contentSession != null || currentForegroundBrainRotApp != null) {
                handleTrackedAppExit("$source - screen is off")
            }
            return false
        }

        val trackedForegroundApp = currentForegroundBrainRotApp ?: return true
        if (contentScanner.isBlockedAppPackageVisible(trackedForegroundApp)) {
            return true
        }

        handleTrackedAppExit("$source - tracked app package is not visible")
        return false
    }

    /**
     * Updates package filtering so we listen broadly while an app is open to detect exits,
     * or narrowly when idle to save battery.
     */
    private fun refreshServiceConfig() {
        updateServiceConfig(contentSession != null || currentForegroundBrainRotApp != null)
    }

    /**
     * Processes incoming accessibility events to detect when the user enters or exits blocked content.
     *
     * @param event The accessibility event emitted by Android.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!powerManager.isInteractive) {
            handleTrackedAppExit("screen is off while receiving accessibility event")
            return
        }

        val scan = contentScanner.scan(
            event.packageName?.toString(),
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            contentSession?.app,
            currentForegroundBrainRotApp,
            contentSession?.takeIf { it.isCovered }?.content?.cover,
        )
        if (scan.trackedAppExited) handleTrackedAppExit("covered app lost foreground")
        val userActiveApp = scan.foregroundApp
        updateForegroundAppState(userActiveApp)
        if (userActiveApp == null && contentSession == null) return
        if (!scan.rootAvailable) {
            validateTrackedAppState("Root node missing")
            return
        }
        val detectedContent = scan.content

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

    /** Records when the user opens or leaves a monitored app and adjusts tracking accordingly. */
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

    /** Called when the accessibility service is interrupted by the system. */
    override fun onInterrupt() = Unit

    /** Cleans up overlays, cancels handlers, and stops all background jobs when the service is destroyed. */
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

    /** Handles detected blocked content by updating the active session or starting a new one. */
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

    /**
     * Called when the user starts viewing blocked content.
     * Starts periodic limit checks and displays the timer overlay if enabled.
     */
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

    /** Shows the floating timer overlay displaying daily or interval usage. */
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

    /**
     * Called when the user leaves blocked content.
     * Stops usage checks, hides overlays, and records the completed viewing session.
     *
     * @param appLeft True if the user switched away from the host app entirely.
     */
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

    /** Records viewing time in the session tracker and closes the session with the blocking manager. */
    private suspend fun saveViewing(finished: FinishedViewing) {
        if (finished.durationMillis > 0L) {
            sessionTracker.addToDailyUsage(finished.durationMillis, finished.app.app)
        }
        // Even a video blocked immediately must close the session that the manager opened.
        blockingManager.onExitBlockedContent(finished.startedAtMillis, finished.endedAtMillis)
    }

    /** Starts periodic checks to monitor usage limits while content is visible. */
    private fun startPeriodicCheck() {
        Timber.d("Starting periodic usage checks (every 1 second)")
        mainHandler.removeCallbacks(videoCheckRunnable)
        mainHandler.postDelayed(videoCheckRunnable, 1000)
    }

    /** Stops periodic usage checks when the user exits blocked content. */
    private fun stopPeriodicCheck() {
        mainHandler.removeCallbacks(videoCheckRunnable)
        Timber.d("Stopped periodic usage checks")
    }

    /** Enforces the active blocking policy by attaching a cover overlay or performing Back navigation. */
    private suspend fun enforceBlocking(session: ContentSession) {
        if (viewingSession !== session || isBlockingSuppressed) return
        when (val action = session.content.blockAction) {
            is ContentBlockAction.PerformGlobalAction -> performGlobalAction(action.action)

            ContentBlockAction.CoverVideoRegion -> {
                // The user may have switched apps while we were waiting for the blocking decision.
                if (!contentScanner.isContentWindowEligible(session.app)) {
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

    /** Periodically checks whether a covered video is still on screen or if new allowance is available. */
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

    /** Re-scans visible windows to update the current content session. */
    private fun refreshDetectedContent() {
        val session = contentSession ?: return
        val activeCover = session.content.cover.takeIf { session.isCovered }
        val content = contentScanner.findVisibleBlockedContent(session.app, activeCover)
        if (content == null) onBlockedContentExited() else onBlockedContentDetected(content)
    }

    /** Checks if the user's latest settings or allowances should block or unblock the current screen. */
    private fun reconsiderVisibleContent() {
        val current = contentSession ?: return
        if (!contentScanner.isContentWindowEligible(current.app)) {
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
                // Query blocking policy without starting a viewing session or recording usage prematurely.
                val shouldBlock = blockingManager.shouldBlockContent()
                if (contentSession !== current || (shouldBlock && !isBlockingSuppressed)) {
                    scheduleCoveredContentCheck()
                    return@launch
                }
                // Transition the covered session into an active viewing session and start counting usage from now.
                val viewing = ContentSession(current.content, System.currentTimeMillis())
                contentSession = viewing
                mainHandler.removeCallbacks(coveredContentCheck)
                onBlockedContentEntered()
            }
        }
    }

    /**
     * Updates accessibility event filtering.
     *
     * @param listenToAll If true, listens to all packages to detect exits; if false, filters to target apps to save battery.
     */
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

    /** Reopens Scrolless after the user grants accessibility permissions in system settings. */
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
}
