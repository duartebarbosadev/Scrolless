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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Applies blocking policy to detected content and records each viewing period once. */
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

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Keep UI work on the main thread; one failed job should not stop all service work. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Inject
    lateinit var sessionTracker: SessionTracker

    @Inject
    lateinit var blockingManager: BlockingManager

    @Inject
    lateinit var blockingConfigRepository: BlockingConfigRepository

    @Inject
    lateinit var userSettingsStore: UserSettingsStore

    @Inject
    lateinit var timerOverlayManager: TimerOverlayManager

    @Inject
    lateinit var blockedContentOverlayManager: BlockedContentOverlayManager

    private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }

    private var contentSession: ContentSession? = null
    private val contentScanner by lazy {
        ContentScanner(this, { isWindowAttachedCoverAllowed }, { currentAllowVideosSentByDm })
    }

    /**
     * Whether covers are allowed to attach to an app window.
     * Debug builds may force the legacy screen-based overlay path for device testing.
     */
    private val isWindowAttachedCoverAllowed: Boolean
        get() = !(BuildConfig.DEBUG && DebugOverlayConfig.forceLegacyOverlay.value)
    /** The session only while viewing time is still running; covered sessions remain tracked separately. */
    private val viewingSession: ContentSession?
        get() = contentSession?.takeUnless { it.isCovered }

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
        if (contentScanner.isBlockedAppPackageVisible(trackedForegroundApp)) {
            return true
        }

        handleTrackedAppExit("$source - tracked app package is not visible")
        return false
    }

    /** Listen beyond the target app while it is open, so Home and app-switching events reach us. */
    private fun refreshServiceConfig() {
        updateServiceConfig(contentSession != null || currentForegroundBrainRotApp != null)
    }

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
        val session = contentSession ?: return
        val activeCover = session.content.cover.takeIf { session.isCovered }
        val content = contentScanner.findVisibleBlockedContent(session.app, activeCover)
        if (content == null) onBlockedContentExited() else onBlockedContentDetected(content)
    }

    /** Reapply the current settings without making the user leave and reopen the video. */
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
}
