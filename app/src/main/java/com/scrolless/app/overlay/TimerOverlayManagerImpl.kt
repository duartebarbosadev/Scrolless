package com.scrolless.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.scrolless.app.R
import com.scrolless.app.provider.AppProvider
import com.scrolless.app.provider.UsageTracker
import com.scrolless.framework.extensions.fadeOutWithBounceAnimation
import com.scrolless.framework.extensions.getReadableTime
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

/**
 * Implementation of TimerOverlayManager that displays a timer overlay on top of the brain rot content.
 */
class TimerOverlayManagerImpl @Inject constructor(
    private val usageTracker: UsageTracker,
    private val appProvider: AppProvider
) : TimerOverlayManager {

    private var overlayView: View? = null
    private var timerTextView: TextView? = null

    // Job for updating the timer text
    private var timerUpdateJob: Job? = null

    private lateinit var layoutParams: WindowManager.LayoutParams

    // Store the last known overlay position (persisted in appProvider)
    private var positionX = appProvider.timerOverlayPositionX
    private var positionY = appProvider.timerOverlayPositionY

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var serviceContext: Context
    private var windowManager: WindowManager? = null

    /**
     * Attaches the service context used to interact with the window manager.
     */
    override fun attachServiceContext(context: Context) {
        serviceContext = context
        windowManager = serviceContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /**
     * Displays the timer overlay on screen, if it's not already visible.
     */
    override fun show() {

        // If already showing, do nothing
        if (overlayView != null) return

        // Offset the start time by 1 second to account for delays.
        val startTimeMillis = System.currentTimeMillis() - 1000

        // Inflate the overlay view using the app's theme
        val themedContext = ContextThemeWrapper(serviceContext, R.style.AppTheme)
        val inflater = LayoutInflater.from(themedContext)
        overlayView = inflater.inflate(R.layout.overlay_timer, null)
        timerTextView = overlayView?.findViewById(R.id.timerTextView)

        // Set up layout parameters for the overlay window
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = positionX
            y = positionY
        }

        // Start invisible for a quick fade-in effect.
        overlayView?.alpha = 0f
        windowManager?.addView(overlayView, layoutParams)

        overlayView?.animate()
            ?.alpha(1f)
            ?.setDuration(200)
            ?.start()

        enableDragging()
        startTimer(startTimeMillis)
    }

    /**
     * Hides the timer overlay with an animation, then removes it from the window.
     */
    override fun hide() {
        stopTimer()

        val currentView = overlayView ?: return

        // Update the timer text with the total daily usage before hiding.
        val totalTime = usageTracker.getDailyUsage()
        timerTextView?.text = totalTime.getReadableTime()

        currentView.fadeOutWithBounceAnimation {
            currentView.visibility = View.GONE
            windowManager?.removeView(currentView)
            overlayView = null
            timerTextView = null
        }
    }

    /**
     * Starts the coroutine to update the timer text every second.
     *
     * @param startTimeMillis The starting point for the timer
     */
    private fun startTimer(startTimeMillis: Long) {

        // Cancel any existing timer before starting a new one
        stopTimer()

        timerUpdateJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTimeMillis
                timerTextView?.text = elapsed.getReadableTime()
                delay(1000)
            }
        }
    }

    /**
     * Cancels any active timer job.
     */
    private fun stopTimer() {
        timerUpdateJob?.cancel()
        timerUpdateJob = null
    }


    /**
     * Enables dragging of the overlay view, updating and persisting position to [appProvider].
     */
    private fun enableDragging() {
        overlayView?.setOnTouchListener(
            object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            initialTouchX = motionEvent.rawX
                            initialTouchY = motionEvent.rawY
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            layoutParams.x = initialX - (motionEvent.rawX - initialTouchX).toInt()
                            layoutParams.y = initialY + (motionEvent.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(overlayView, layoutParams)
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            positionX = layoutParams.x
                            positionY = layoutParams.y

                            // Persist final position to appProvider
                            appProvider.timerOverlayPositionX = positionX
                            appProvider.timerOverlayPositionY = positionY
                        }
                    }
                    return false
                }
            },
        )
    }
}