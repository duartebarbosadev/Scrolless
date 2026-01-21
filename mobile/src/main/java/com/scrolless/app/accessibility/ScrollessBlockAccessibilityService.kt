/*
 * Copyright (C) 2025 Scrolless
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
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.scrolless.app.core.blocking.BlockingManager
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.repository.UsageTracker
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.ui.overlay.TimerOverlayManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Accessibility service that monitors and blocks access to "brain rot" content based on user-configured limits.
 *
 * This service uses Android's Accessibility framework to detect when the user enters specific apps
 * or UI elements (such as YouTube Shorts or Instagram Reels) and enforces blocking policies according
 * to the active [BlockOption]:
 * - [BlockOption.BlockAll]: Immediately blocks all detected content
 * - [BlockOption.DailyLimit]: Allows usage up to a configured daily time limit
 * - [BlockOption.IntervalTimer]: Allows usage within time intervals
 * - [BlockOption.NothingSelected]: No blocking is performed
 *
 * The service tracks usage time, displays an optional timer overlay, and performs automatic
 * back navigation when limits are exceeded.
 *
 * Permissions Required:
 * - Accessibility service permission must be granted by the user
 *
 * @see com.scrolless.app.core.blocking.BlockingManager for blocking logic
 * @see BlockOption for available blocking strategies
 * @see BlockableApp for supported apps
 */
@SuppressLint("AccessibilityPolicy") // Accessibility APIs are required to enforce user-configured blocking policies.
@AndroidEntryPoint
class ScrollessBlockAccessibilityService : AccessibilityService() {

    override fun onCreate() {
        super.onCreate()
        timerOverlayManager.attachServiceContext(this)
    }

    /**
     * Main thread handler for executing UI-related operations like back navigation.
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Handler for scheduling periodic checks of usage time while user is in blocked content.
     * Checks are performed every 1 second.
     */
    private val videoCheckHandler = Handler(Looper.getMainLooper())

    /**
     * Coroutine scope for the service lifecycle. Uses [SupervisorJob] to prevent child
     * coroutine failures from cancelling the entire scope.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Tracks daily and session usage time for blocked content.
     */
    @Inject
    lateinit var usageTracker: UsageTracker

    /**
     * Manages blocking logic and decisions based on configured [BlockOption].
     */
    @Inject
    lateinit var blockingManager: BlockingManager

    /**
     * Provides access to user settings including active block option, time limits, and overlay preferences.
     */
    @Inject
    lateinit var userSettingsStore: UserSettingsStore

    /**
     * Manages the display of an optional timer overlay showing current usage time.
     */
    @Inject
    lateinit var timerOverlayManager: TimerOverlayManager

    private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }

    /**
     * Flag indicating whether the user is currently in blocked content.
     * Prevents duplicate processing of accessibility events.
     */
    private var isProcessingBlockedContent = false

    /**
     * Timestamp (in milliseconds) when the user entered blocked content.
     * Used to calculate session duration.
     */
    private var timeStartOnBrainRot: Long = 0L

    /**
     * The currently detected blocked app, or null if no blocked content is active.
     */
    private var detectedApp: BlockableApp? = null

    /**
     * Current timer overlay enabled state, updated reactively.
     */
    private var currentTimerOverlayEnabled: Boolean = false

    /**
     * Current active block option, updated reactively.
     */
    private var currentBlockOption: BlockOption = BlockOption.NothingSelected

    /**
     * Epoch millis until which blocking logic should remain paused.
     */
    @Volatile
    private var pauseUntilMillis: Long = 0L

    private fun isPauseActive(now: Long = System.currentTimeMillis()): Boolean = pauseUntilMillis > now

    private var currentBlockableApp: BlockableApp? = null

    /**
     * Runnable that performs periodic checks (every 1 second) to determine if the user
     * has exceeded their time limit while in blocked content.
     *
     * If the limit is exceeded, triggers automatic back navigation.
     */
    private val videoCheckRunnable: Runnable = Runnable {

        if (!isProcessingBlockedContent) {
            Timber.w("Periodic check runnable executed but no longer processing content")
            return@Runnable
        }

        val elapsed = System.currentTimeMillis() - timeStartOnBrainRot

        if (isPauseActive()) {
            // While paused, continue scheduling to keep timer overlay updated but skip blocking checks
            Timber.v("Periodic check (paused): elapsed=%d ms", elapsed)
            videoCheckHandler.postDelayed(videoCheckRunnable, 1000)
        } else {
            Timber.v("Periodic check running: elapsed=%d ms", elapsed)
            serviceScope.launch {
                when (val action = blockingManager.onPeriodicCheck(elapsed)) {
                    is BlockingResult.BlockNow -> {
                        Timber.i("Periodic check: limit reached (elapsed=%d). Navigating back.", elapsed)
                        performBackNavigation()
                    }

                    is BlockingResult.CheckLater -> {
                        Timber.v("Periodic check: limit not reached, but asked to check again later in %d ms", action.delayMillis)
                        videoCheckHandler.postDelayed(videoCheckRunnable, action.delayMillis)
                    }

                    is BlockingResult.Continue -> {
                        Timber.v("Periodic check: limit not reached, scheduling next check")
                        videoCheckHandler.postDelayed(videoCheckRunnable, 10000)
                    }
                }
            }
        }
    }

    /**
     * Called when the accessibility service is successfully connected and ready to use.
     *
     * Performs initialization:
     * - Configures accessibility service info (event types, flags)
     * - Attaches service context to [TimerOverlayManager]
     * - Starts observing user settings for block option changes
     * - Performs initial daily usage reset check
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.i("Accessibility service connected")

        // Start with restricted configuration to save battery
        updateServiceConfig(false)

        // Check if we need to bring the app to foreground
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
            val timeLimitFlow = userSettingsStore.getTimeLimit().distinctUntilChanged()
            val intervalLengthFlow = userSettingsStore.getIntervalLength().distinctUntilChanged()
            val blockOptionFlow = userSettingsStore.getActiveBlockOption().distinctUntilChanged()
            combine(timeLimitFlow, intervalLengthFlow, blockOptionFlow) { _, _, blockOption -> blockOption }.collect { blockOption ->
                Timber.d("Settings changed, re-initializing blocking manager with %s", blockOption)
                blockingManager.init(blockOption)
            }
        }

        // Observe timer overlay enabled changes
        serviceScope.launch {
            userSettingsStore.getTimerOverlayEnabled().collect { currentTimerOverlayEnabled = it }
        }

        // Observe active block option changes
        serviceScope.launch {
            userSettingsStore.getActiveBlockOption().collect { currentBlockOption = it }
        }

        // Observe pause toggle
        serviceScope.launch {
            userSettingsStore.getPauseUntil().collect { newPauseUntil ->
                val wasPaused = isPauseActive()
                pauseUntilMillis = newPauseUntil
                val nowPaused = isPauseActive()
                if (nowPaused && !wasPaused) {
                    Timber.i("Pause activated until %d - session tracking continues", pauseUntilMillis)
                    // Session continues, usage still tracked, only blocking checks are skipped
                } else if (!nowPaused && wasPaused) {
                    Timber.i("Pause expired, resuming blocking checks")
                    // If still in blocked content, check if should block now
                    if (isProcessingBlockedContent) {
                        serviceScope.launch(Dispatchers.IO) {
                            usageTracker.checkDailyReset()
                            if (blockingManager.onEnterBlockedContent()) {
                                Timber.i("Blocking immediately after pause expired")
                                performBackNavigation()
                            }
                        }
                    }
                } else {
                    Timber.v("Pause timestamp updated to %d (no state change)", pauseUntilMillis)
                }
            }
        }

        // Check daily reset on service start
        serviceScope.launch {
            usageTracker.checkDailyReset()
        }
    }

    /**
     * Called when an accessibility event is received from the system.
     *
     * Monitors window state changes and window content changes to detect when the user
     * enters or exits blocked content. Triggers [onBlockedContentEntered] or
     * [onBlockedContentExited] accordingly.
     *
     * Note: Careful when adding logs as this can get spammed a lot
     *
     * @param event The accessibility event containing information about the UI change
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Skip processing if screen is off
        if (!powerManager.isInteractive) {
            if (isProcessingBlockedContent) {
                Timber.d("Calling exit because screen is off")
                onBlockedContentExited()
            }
            return
        }

        // Get root node
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            if (isProcessingBlockedContent) {
                Timber.w("Root node missing while processing content, treating as exit")
                onBlockedContentExited()
            } else {
                Timber.v("No root node available, skipping event")
            }
            return
        }

        val packageId = event.packageName?.toString() ?: ""

        // Ignore events from our own app (e.g. Timer Overlay updates)
        if (packageId == packageName) {
            return
        }

        // Detect blocked content
        val onBrainRotApp = detectAppForBlockedContent(packageId, rootNode)

        // Only trigger changes if detection state actually changed
        if (onBrainRotApp != null) {

            currentBlockableApp = onBrainRotApp
            detectedApp = onBrainRotApp
            onBlockedContentEntered()
        } else if (isProcessingBlockedContent) {

            currentBlockableApp = null
            // If we were processing, but now nothing detected, we exited
            Timber.v("Brain rot content no longer detected, triggering exit")
            onBlockedContentExited()
        }
    }

    /**
     * Called when the accessibility service is interrupted.
     */
    override fun onInterrupt() = Unit

    /**
     * Called when the service is being destroyed.
     *
     * Performs cleanup:
     * - Stops periodic checks
     * - Cancels all coroutines in [serviceScope]
     */
    override fun onDestroy() {
        super.onDestroy()
        Timber.d(
            "Service state at destroy: isProcessingBlockedContent=%b, detectedApp=%s",
            isProcessingBlockedContent, detectedApp,
        )
        stopPeriodicCheck()
        timerOverlayManager.cleanup()
        serviceScope.cancel()
    }

    /**
     * Detects if the current window contains blocked content by inspecting the accessibility node tree.
     *
     * Searches for view IDs that match known blocked apps (e.g., YouTube Shorts, Instagram Reels).
     * Optimized to exit early once a match is found.
     *
     * @param rootNode The root accessibility node of the current window
     * @return The detected [BlockableApp], or null if no blocked content is found
     */
    private fun detectAppForBlockedContent(packageId: String, rootNode: AccessibilityNodeInfo): BlockableApp? {

        // If we are processing content and we received an event
        //  make sure that the app is still visible as we can get events from other apps
        //  otherwise it means that the user has left the app
        currentBlockableApp?.takeIf { isProcessingBlockedContent }?.let { blockableApp ->
            if (isBlockedAppVisible(blockableApp)) {
                return blockableApp
            } else {
                Timber.v("Blocked app is no longer visible, treating as exit")
            }
        }

        // Detect if there's a new app
        val match = BlockableApp.entries.firstOrNull { appEnum ->
            if (!packageId.startsWith(appEnum.packageId)) return@firstOrNull false

            val nodes = rootNode.findAccessibilityNodeInfosByViewId(appEnum.getViewId())

            // Make sure if we found nodes, they are visible to the user
            val match = nodes.any { node ->
                val rect = android.graphics.Rect()
                node.getBoundsInScreen(rect)

                node.isVisibleToUser && rect.width() > 0 && rect.height() > 0
            }

            match
        }

        return match
    }

    /**
     * Checks if ANY blocked app has a visible window on screen AND the blocked content view is visible.
     * This handles cases where the user interacts with SystemUI (notification shade) or keyboards,
     * where the blocked app is still visible but not the source of the latest event.
     */
    private fun isBlockedAppVisible(blockableApp: BlockableApp): Boolean {

        // windows returns a list of windows in z-order (top to bottom)
        return windows.any { window ->
            if (window.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION) {
                val root = window.root ?: return@any false

                // Find if this package corresponds to a blocked app
                // If it is a blocked app, check if the specific view ID is visible
                val nodes = root.findAccessibilityNodeInfosByViewId(blockableApp.getViewId())
                val isViewVisible = nodes.any { node ->
                    val rect = android.graphics.Rect()
                    node.getBoundsInScreen(rect)
                    node.isVisibleToUser && rect.width() > 0 && rect.height() > 0
                }
                isViewVisible
            } else {
                false
            }
        }
    }

    /**
     * Called when the user enters blocked content.
     *
     * If already processing blocked content, this call is ignored to prevent duplicate handling.
     *
     * Actions performed:
     * - Marks [isProcessingBlockedContent] as true to prevent duplicate event handling
     * - Records entry timestamp to track session duration
     * - Starts periodic usage checks to enforce time limits
     * - Shows timer overlay (if enabled) to keep user informed of usage
     * - Checks for daily usage reset to ensure accurate daily tracking
     * - Immediately blocks if limit already exceeded (e.g., BlockAll mode)
     */
    private fun onBlockedContentEntered() {

        // If the currentOnVideos boolean is set to true, we already dealt with the event
        if (isProcessingBlockedContent) {
            // Avoid this log as its spamming
            // Timber.v("Already processing blocked content, ignoring duplicate enter event")
            return
        }

        isProcessingBlockedContent = true
        timeStartOnBrainRot = System.currentTimeMillis()
        Timber.d("Entered blocked content at %d (app=%s, paused=%b)", timeStartOnBrainRot, detectedApp, isPauseActive())

        // Expand service scope to detect when user leaves the app (e.g. to launcher)
        updateServiceConfig(true)

        startPeriodicCheck()

        // Only perform blocking checks if NOT paused
        if (!isPauseActive()) {
            serviceScope.launch(Dispatchers.IO) {

                // Check for daily reset (If its past midnight, reset the daily usage)
                usageTracker.checkDailyReset()

                val shouldBlock = blockingManager.onEnterBlockedContent()
                if (shouldBlock) {
                    Timber.i("Blocking on enter")
                    performBackNavigation()
                } else {
                    // Only show timer overlay if we're NOT blocking immediately
                    // (no point showing timer if user is about to be kicked out)
                    mainHandler.post {
                        if (currentTimerOverlayEnabled && isProcessingBlockedContent) {
                            Timber.v("Showing timer overlay")
                            timerOverlayManager.show(timeStartOnBrainRot)
                        }
                    }
                    Timber.d("Content allowed on enter, will monitor usage")
                }
            }
        } else {
            // Paused - still check for daily reset and show timer overlay
            serviceScope.launch(Dispatchers.IO) {
                usageTracker.checkDailyReset()
            }
            if (currentTimerOverlayEnabled) {
                Timber.v("Showing timer overlay (paused)")
                mainHandler.post {
                    timerOverlayManager.show(timeStartOnBrainRot)
                }
            }
            Timber.d("Pause active - skipping blocking check on enter, but tracking usage")
        }
    }

    /**
     * Called when the user exits blocked content.
     *
     * If not currently processing blocked content, this call is ignored.
     *
     * Actions performed:
     * - Calculates session duration
     * - Marks [isProcessingBlockedContent] as false
     * - Stops periodic usage checks
     * - Records session usage time
     * - Notifies [BlockingManager] of exit
     * - Hides timer overlay (if enabled)
     */
    private fun onBlockedContentExited() {

        val sessionTime = System.currentTimeMillis() - timeStartOnBrainRot
        Timber.d("Exited blocked content. Session=%d ms (app=%s)", sessionTime, detectedApp)

        // Hide timer overlay if enabled
        if (currentTimerOverlayEnabled) {
            Timber.v("Hiding timer overlay")
            timerOverlayManager.hide()
        }

        stopPeriodicCheck()
        isProcessingBlockedContent = false

        // Restrict service scope again to save battery
        updateServiceConfig(false)

        // Capture detectedApp before clearing it
        val exitedApp = detectedApp

        serviceScope.launch(Dispatchers.IO) {
            // Add to usage in memory with per-app tracking
            Timber.d("Recording session usage: %d ms for app: %s", sessionTime, exitedApp?.name)
            if (exitedApp == null) {
                Timber.w("Exited app is null; recording total usage only")
            }
            usageTracker.addToDailyUsage(sessionTime, exitedApp)

            // Let blocking manager do its logic, if needed
            blockingManager.onExitBlockedContent(sessionTime)
        }

        Timber.d("Exit handling completed for app: %s", exitedApp?.name)
        detectedApp = null
    }

    /**
     * Starts periodic checks of usage time.
     *
     * Schedules [videoCheckRunnable] to run every 1 second to monitor if the user
     * has exceeded their time limit while in blocked content.
     */
    private fun startPeriodicCheck() {
        Timber.d("Starting periodic usage checks (every 1 second)")
        videoCheckHandler.removeCallbacks(videoCheckRunnable)
        videoCheckHandler.postDelayed(videoCheckRunnable, 1000)
        Timber.v("First periodic check scheduled in 1 second")
    }

    /**
     * Stops periodic usage checks.
     *
     * Removes any pending executions of [videoCheckRunnable] from the handler queue.
     */
    private fun stopPeriodicCheck() {
        Timber.d("Stopping periodic usage checks")
        val hadCallbacks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            videoCheckHandler.hasCallbacks(videoCheckRunnable)
        } else {
            true // Assume true on older versions
        }
        videoCheckHandler.removeCallbacks(videoCheckRunnable)
        if (hadCallbacks) {
            Timber.i("Periodic checks stopped - callbacks were pending and have been removed")
        } else {
            Timber.v("Periodic checks stopped - no callbacks were pending")
        }
    }

    /**
     * Performs automatic back navigation to exit blocked content.
     *
     * Uses the detected app's configured exit strategy, or defaults to [GLOBAL_ACTION_BACK].
     * The action is posted to the main thread handler to ensure it runs on the UI thread.
     */
    private fun performBackNavigation() {
        mainHandler.post {
            val action = detectedApp?.getExitStrategy() ?: GLOBAL_ACTION_BACK
            Timber.d("Performing back navigation with action=%d", action)
            val success = performGlobalAction(action)
            Timber.d("Back navigation result: success=%b", success)
        }
    }

    /**
     * Updates the accessibility service configuration to either listen to all packages or only target packages.
     * This is necessary to know when the user has left the application
     *
     * @param listenToAll If true, clears package filter to receive events from all apps (needed to detect exit).
     *                    If false, restricts events to [BlockableApp] packages only (to save battery).
     */
    private fun updateServiceConfig(listenToAll: Boolean) {

        val info = serviceInfo ?: return
        if (listenToAll) {
            info.packageNames = null // Listen to all
            // Ensure we can always retrieve windows
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            Timber.d("Expanded service configuration to listen to all packages")
        } else {
            info.packageNames = BlockableApp.entries.map { it.packageId }.toTypedArray()
            Timber.d("Restricted service configuration to target packages only")
        }

        serviceInfo = info
    }

    /**
     * Brings the app to the foreground by starting the MainActivity.
     *
     * Uses FLAG_ACTIVITY_NEW_TASK and FLAG_ACTIVITY_REORDER_TO_FRONT to bring
     * the existing activity to front without recreating it.
     */
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
