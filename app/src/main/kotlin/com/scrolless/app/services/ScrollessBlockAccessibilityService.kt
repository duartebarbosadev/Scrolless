/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.scrolless.app.features.home.BlockOption
import com.scrolless.app.overlay.TimerOverlayManager
import com.scrolless.app.provider.AppProvider
import com.scrolless.app.provider.UsageTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [ScrollessBlockAccessibilityService] blocks or restricts user access to certain "brain rot"
 * content based on configured [BlockOption] and time limits.
 *
 * It uses Android's accessibility framework to detect specific UI elements (like YouTube Shorts
 * or Instagram Reels) and enforces user-configured usage limits (daily, interval-based, or temporary).
 */
@AndroidEntryPoint
class ScrollessBlockAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_ACCESSIBILITY_SERVICE_ENABLE =
            "com.scrolless.app.ACCESSIBILITY_SERVICE_ENABLED"
    }

    @Inject
    lateinit var appProvider: AppProvider

    private val mainHandler = Handler(Looper.getMainLooper())
    private val videoCheckHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Inject
    lateinit var usageTracker: UsageTracker

    @Inject
    lateinit var blockController: BlockController

    @Inject
    lateinit var timerOverlayManager: TimerOverlayManager

    private var isProcessingBlockedContent = false
    private var timeStartOnBrainRot: Long = 0L

    private var detectedApp: AppEnum? = null

    private val videoCheckRunnable = object : Runnable {
        override fun run() {
            if (isProcessingBlockedContent) {
                val elapsed = System.currentTimeMillis() - timeStartOnBrainRot
                if (blockController.onPeriodicCheck(elapsed)) {
                    performBackNavigation()
                } else {
                    videoCheckHandler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        configureServiceInfo()

        // Make sure the timer overlay manager has the service's context
        timerOverlayManager.attachServiceContext(this)

        // Service is now connected and running!
        // Send a local broadcast to notify the app
        val intent = Intent(ACTION_ACCESSIBILITY_SERVICE_ENABLE)
        sendBroadcast(intent)

        // Observe changes to the block config
        serviceScope.launch {
            appProvider.blockConfigFlow.collect { newConfig ->
                blockController.init(newConfig)
            }
        }

        // Check daily reset on service start
        usageTracker.checkDailyReset()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val rootNode = rootInActiveWindow ?: return
        detectedApp = detectAppForBlockedContent(rootNode)

        if (detectedApp != null) {
            onBlockedContentEntered()
        } else {
            onBlockedContentExited()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        stopPeriodicCheck()

        // Save usage in memory to SharedPreferences, so we don’t lose it
        usageTracker.save()
        serviceScope.cancel()
    }

    private fun configureServiceInfo() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100

            flags =
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    private fun detectAppForBlockedContent(rootNode: AccessibilityNodeInfo): AppEnum? =
        AppEnum.entries.firstOrNull { appEnum ->
            rootNode.findAccessibilityNodeInfosByViewId(appEnum.getViewId()).isNotEmpty()
        }

    private fun onBlockedContentEntered() {
        // If the currentOnVideos boolean is set to true, we already dealt with the event
        if (isProcessingBlockedContent) {
            return
        }

        isProcessingBlockedContent = true
        timeStartOnBrainRot = System.currentTimeMillis()
        startPeriodicCheck()

        // If timer overlay is enabled, show it
        if (appProvider.timerOverlayEnabled) {
            timerOverlayManager.show()
        }

        // Check for daily reset (If its past midnight, reset the daily usage)
        usageTracker.checkDailyReset()

        if (blockController.onEnterBlockedContent()) {
            performBackNavigation()
        }
    }

    private fun onBlockedContentExited() {
        // Early stop, If we are no longer processing blocked content, we already dealt with the event
        if (!isProcessingBlockedContent) {
            return
        }

        val sessionTime = System.currentTimeMillis() - timeStartOnBrainRot

        // Add to usage in memory
        usageTracker.addToDailyUsage(sessionTime)

        // Let block controller do its logic, if needed
        blockController.onExitBlockedContent(sessionTime)

        isProcessingBlockedContent = false
        stopPeriodicCheck()

        if (appProvider.timerOverlayEnabled) {
            timerOverlayManager.hide()
        }

        usageTracker.save()
    }

    private fun startPeriodicCheck() {
        videoCheckHandler.removeCallbacks(videoCheckRunnable)
        videoCheckHandler.postDelayed(videoCheckRunnable, 1000)
    }

    private fun stopPeriodicCheck() {
        videoCheckHandler.removeCallbacks(videoCheckRunnable)
    }

    private fun performBackNavigation() {
        mainHandler.post {
            performGlobalAction(detectedApp?.getGetawayStrategy() ?: GLOBAL_ACTION_BACK)
        }
    }
}
