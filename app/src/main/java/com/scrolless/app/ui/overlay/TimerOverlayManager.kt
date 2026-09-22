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
package com.scrolless.app.ui.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsCompat
import com.scrolless.app.core.model.IntervalUsage
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.core.repository.setTimerOverlayPosition
import com.scrolless.app.designsystem.theme.timerOverlayBackgroundColor
import com.scrolless.app.designsystem.util.formatAsTime
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A View-based timer overlay that shows saved usage plus the current viewing session.
 * It uses Android Views instead of Compose because the Compose version lagged while dragging.
 */
class TimerOverlayManager @Inject constructor(private val userSettingsStore: UserSettingsStore) {

    private var rootView: DragInterceptFrameLayout? = null
    private var timerTextView: TextView? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var snapAnimator: ValueAnimator? = null
    private var wiggleAnimator: ObjectAnimator? = null
    private val enterAnimationRunnable = Runnable { startEnterAnimation() }
    private var velocityTracker: android.view.VelocityTracker? = null

    private lateinit var serviceContext: Context
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var sessionStartTime = 0L
    private var usage = IntervalUsage.NOT_STARTED
    private var windowLengthMillis = 0L
    private var timerJob: Job? = null
    private var exitAnimationJob: Job? = null
    private var screenBounds: ScreenBounds? = null

    // Drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var touchSlop = 0

    fun attachServiceContext(context: Context) {
        serviceContext = context
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun showDaily(sessionStartAt: Long, dailyUsageMillis: Long) {
        val zoneId = ZoneId.systemDefault()
        val currentDate = Instant.ofEpochMilli(sessionStartAt).atZone(zoneId).toLocalDate()
        val dayStartMillis = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextDayStartMillis = currentDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Reset at the next midnight. Edge case: if the overlay stays open for several days
        // during a daylight-saving clock change, a later reset may be one hour early or late.
        // This rare multi-day case is left as-is to keep the timer simple.
        // But at this point, it's the users "fault" :p
        showOverlay(
            sessionStartAt = sessionStartAt,
            usage = IntervalUsage(dayStartMillis, dailyUsageMillis),
            windowLengthMillis = nextDayStartMillis - dayStartMillis,
        )
    }

    fun showInterval(sessionStartAt: Long, intervalUsage: IntervalUsage, intervalLengthMillis: Long) {
        showOverlay(sessionStartAt, intervalUsage, intervalLengthMillis)
    }

    private fun showOverlay(sessionStartAt: Long, usage: IntervalUsage, windowLengthMillis: Long) {
        if (!::serviceContext.isInitialized) {
            Timber.w("Timer overlay requested before service context was attached")
            return
        }
        val wm = windowManager ?: return

        sessionStartTime = sessionStartAt
        this.usage = usage
        this.windowLengthMillis = windowLengthMillis

        val isNewView = rootView == null
        if (isNewView) createOverlayView()
        val view = rootView ?: return

        // New and reused timers start the session with the same text and position checks.
        stopOverlayAnimations(view)
        view.rotation = 0f
        view.translationX = 0f
        view.alpha = if (isNewView) 0f else 1f
        timerTextView?.text = displayedDurationAt(System.currentTimeMillis()).formatAsTime()
        screenBounds = calculateScreenBounds()
        if (isNewView) {
            // Measure before attaching so saved positions respect the timer's full height.
            val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(measureSpec, measureSpec)
        }
        keepTimerInAllowedArea()

        try {
            if (isNewView) wm.addView(view, layoutParams)
            startTimer()
            if (isNewView) view.post(enterAnimationRunnable)
        } catch (e: Exception) {
            Timber.e(e, "Failed to show overlay")
            cleanupView()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        timerTextView = TextView(serviceContext).apply {
            textSize = 18f // sp
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)

            val paddingH = dpToPx(20f)
            val paddingV = dpToPx(12f)
            setPadding(paddingH, paddingV, paddingH, paddingV)

            background = GradientDrawable().apply {
                setColor(timerOverlayBackgroundColor.toArgb())
                cornerRadius = dpToPx(24f).toFloat()
            }
            elevation = dpToPx(8f).toFloat()
            gravity = Gravity.CENTER
        }

        // Let the whole timer handle dragging, including touches that start on the text.
        rootView = DragInterceptFrameLayout(serviceContext).apply {
            addView(
                timerTextView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )

            setOnTouchListener { _, event ->
                handleTouch(event)
            }
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> keepTimerInAllowedArea() }
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = userSettingsStore.getTimerOverlayPositionX().value
            y = userSettingsStore.getTimerOverlayPositionY().value
        }
    }

    fun hide(sessionStartAt: Long, sessionEndAt: Long) {

        Timber.d("Hiding overlay view")
        // A delayed exit from an older session must not hide the timer for a newer session.
        if (sessionStartTime != sessionStartAt) {
            Timber.v("Ignoring stale overlay hide for session=%d, current=%d", sessionStartAt, sessionStartTime)
            return
        }

        timerJob?.cancel()
        timerJob = null

        // Freeze at the actual exit time
        timerTextView?.text = displayedDurationAt(sessionEndAt).formatAsTime()

        startWiggleAnimation()

        exitAnimationJob?.cancel()
        exitAnimationJob = coroutineScope.launch {
            delay(SUMMARY_DISPLAY_DURATION_MS.milliseconds)
            startExitAnimation()
        }
    }

    /** Hide the timer when the video is covered, without leaving a summary on top of the blocker. */
    fun dismissImmediately() {
        cleanupView()
    }

    fun cleanup() {
        cleanupView()
        coroutineScope.cancel()
    }

    private fun cleanupView() {
        val view = rootView
        rootView = null // Repeated cleanup must not try to remove the same view twice.
        timerTextView = null

        stopOverlayAnimations(view)
        if (view != null) {
            try {
                view.visibility = View.GONE
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove overlay")
            }
        }

        timerJob?.cancel()
        timerJob = null
        resetDragState()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (true) {
                // showOverlay already displayed the current total.
                delay(1000.milliseconds)
                timerTextView?.text = displayedDurationAt(System.currentTimeMillis()).formatAsTime()
            }
        }
    }

    private fun stopOverlayAnimations(view: View?) {
        exitAnimationJob?.cancel()
        exitAnimationJob = null
        wiggleAnimator?.cancel()
        wiggleAnimator = null
        cancelSnapAnimation()
        if (view == null) return
        view.removeCallbacks(enterAnimationRunnable)
        // Cancelling an exit animation also calls onAnimationEnd. Detach it before cancelling
        // so it cannot remove the timer we are about to reuse.
        view.animate().setListener(null)
        view.animate().cancel()
    }

    private fun cancelSnapAnimation() {
        // cancel() also calls onAnimationEnd; an interrupted snap must not save or vibrate.
        snapAnimator?.removeAllListeners()
        snapAnimator?.cancel()
        snapAnimator = null
    }

    // Combine saved usage with this session, counting only the current day or interval.
    private fun displayedDurationAt(nowMillis: Long): Long = usage.plusSession(
        sessionStartMillis = sessionStartTime,
        sessionEndMillis = nowMillis,
        lengthMillis = windowLengthMillis,
    ).usageMillis

    private fun startEnterAnimation() {
        val view = rootView ?: return
        val direction = if (isAnchoredRight()) 1 else -1
        val distance = view.width.takeIf { it > 0 } ?: view.measuredWidth

        view.translationX = (direction * distance).toFloat()
        view.alpha = 0f

        view.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun startExitAnimation() {
        val view = rootView ?: return
        val direction = if (isAnchoredRight()) 1 else -1
        val distance = (view.width.takeIf { it > 0 } ?: view.measuredWidth).toFloat().coerceAtLeast(1f)

        view.animate()
            .translationX(direction * distance)
            .alpha(0f)
            .setDuration(EXIT_ANIMATION_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        cleanupView()
                    }
                },
            )
            .start()
    }

    private fun startWiggleAnimation() {
        val view = rootView ?: return
        // Draw attention to the final usage total before the timer disappears.

        wiggleAnimator?.cancel()
        wiggleAnimator = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, 8f, -8f, 5f, -5f, 3f, -3f, 0f).apply {
            duration = 500
            start()
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Stop any previous snap so the timer follows the new drag immediately.
                cancelSnapAnimation()
                resetDragState()
                // Rotation or resizing may have changed the available space since the timer appeared.
                screenBounds = calculateScreenBounds()
                keepTimerInAllowedArea()
                velocityTracker = android.view.VelocityTracker.obtain()
                trackScreenMovement(event)

                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                trackScreenMovement(event)
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()

                // Ignore small finger movements until the user clearly starts dragging.
                if (!isDragging && abs(deltaX) <= touchSlop && abs(deltaY) <= touchSlop) return true
                isDragging = true

                // This drag calculation measures x from the right edge, so moving right reduces x.
                moveTimerTo(initialX - deltaX, initialY + deltaY)
                return true
            }

            MotionEvent.ACTION_UP -> {
                trackScreenMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)

                if (isDragging) {
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    val velocityY = velocityTracker?.yVelocity ?: 0f

                    snapAfterDrag(velocityX, velocityY)
                }

                resetDragState()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                // Android interrupted the gesture; discard its velocity instead of flinging.
                resetDragState()
                return true
            }
        }
        return false
    }

    private fun trackScreenMovement(event: MotionEvent) {
        val tracker = velocityTracker ?: return
        // The window follows the finger, so local coordinates hide most of its movement.
        // Measure velocity on the screen, just like the drag position above.
        val screenEvent = MotionEvent.obtain(event)
        screenEvent.offsetLocation(event.rawX - event.x, event.rawY - event.y)
        tracker.addMovement(screenEvent)
        screenEvent.recycle()
    }

    private fun resetDragState() {
        isDragging = false
        velocityTracker?.recycle()
        velocityTracker = null
    }

    /** Keeps the existing fling direction or chooses the nearest edge for a slow release. */
    private fun snapAfterDrag(velocityX: Float, velocityY: Float) {
        val params = layoutParams ?: return
        val bounds = screenBounds ?: return
        val viewWidth = rootView?.width ?: 0
        val viewHeight = rootView?.height ?: 0
        val minX = 0
        val maxX = (bounds.width - viewWidth).coerceAtLeast(0)
        val minY = 0
        val maxY = maximumTimerY(viewHeight)

        val currentX = params.x.coerceIn(minX, maxX)
        val currentY = params.y.coerceIn(minY, maxY)

        val flingThreshold = 1000f // pixels/sec

        var targetX = currentX
        var targetY = currentY

        // A fast release continues toward an edge; a slow release snaps to the nearest one.
        if (abs(velocityX) > flingThreshold || abs(velocityY) > flingThreshold) {
            // Work out how long this movement would take to reach a left or right edge.
            val tX = if (velocityX > 0) {
                currentX.toFloat() / velocityX // Time to reach 0
            } else if (velocityX < 0) {
                (currentX - maxX).toFloat() / velocityX // Time to reach maxX
            } else {
                Float.POSITIVE_INFINITY
            }

            // Do the same for the top and bottom edges.
            val tY = if (velocityY > 0) {
                (maxY - currentY).toFloat() / velocityY // Time to reach maxY
            } else if (velocityY < 0) {
                -currentY.toFloat() / velocityY // Time to reach 0
            } else {
                Float.POSITIVE_INFINITY
            }

            // Stop at whichever edge the fling would reach first.
            val t = minOf(tX, tY)

            // Keep the landing point inside the screen so the timer stays reachable.
            targetX = (currentX - velocityX * t).toInt().coerceIn(minX, maxX)
            targetY = (currentY + velocityY * t).toInt().coerceIn(minY, maxY)
        } else {
            // For a slow release, move the shortest distance to an edge.
            val distRight = currentX // x=0
            val distLeft = maxX - currentX // x=maxX
            val distTop = currentY // y=0
            val distBottom = maxY - currentY // y=maxY

            val minDist = minOf(distRight, distLeft, distTop, distBottom)

            when (minDist) {
                distRight -> targetX = minX
                distLeft -> targetX = maxX
                distTop -> targetY = minY
                distBottom -> targetY = maxY
            }
        }

        snapToPosition(targetX, targetY)
    }

    private fun snapToPosition(targetX: Int, targetY: Int) {
        val params = layoutParams ?: return
        val startX = params.x
        val startY = params.y

        cancelSnapAnimation()
        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                moveTimerTo(
                    x = (startX + (targetX - startX) * fraction).toInt(),
                    y = (startY + (targetY - startY) * fraction).toInt(),
                )
            }
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        persistOverlayPosition(params.x, params.y)
                        rootView?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                },
            )
            start()
        }
    }

    /** Keep the bottom of the timer above the reserved lower fifth of the usable screen. */
    private fun maximumTimerY(timerHeight: Int): Int {
        val screenHeight = screenBounds?.height ?: return 0
        return ((screenHeight * TIMER_ALLOWED_HEIGHT_FRACTION).toInt() - timerHeight).coerceAtLeast(0)
    }

    private fun keepTimerInAllowedArea() {
        val params = layoutParams ?: return
        moveTimerTo(params.x, params.y)
    }

    /** Every position change stays within the allowed area, including before attachment. */
    private fun moveTimerTo(x: Int, y: Int) {
        val view = rootView ?: return
        val params = layoutParams ?: return
        val bounds = screenBounds ?: return
        val width = view.width.takeIf { it > 0 } ?: view.measuredWidth
        val height = view.height.takeIf { it > 0 } ?: view.measuredHeight
        val allowedX = x.coerceIn(0, (bounds.width - width).coerceAtLeast(0))
        val allowedY = y.coerceIn(0, maximumTimerY(height))
        if (params.x == allowedX && params.y == allowedY) return

        params.x = allowedX
        params.y = allowedY
        if (view.isAttachedToWindow) {
            try {
                windowManager?.updateViewLayout(view, params)
            } catch (e: Exception) {
                Timber.w(e, "Could not move timer overlay")
            }
        }
    }

    // Save the final resting position, not every animation frame.
    private fun persistOverlayPosition(x: Int, y: Int) {
        coroutineScope.launch {
            userSettingsStore.setTimerOverlayPosition(x, y)
        }
    }

    private fun isAnchoredRight(): Boolean {
        val params = layoutParams ?: return true
        val bounds = screenBounds ?: return true
        val viewWidth = rootView?.width ?: 0
        val maxX = (bounds.width - viewWidth).coerceAtLeast(0)
        if (maxX == 0) return true
        return params.x <= maxX / 2
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            serviceContext.resources.displayMetrics,
        ).toInt()
    }

    @Suppress("DEPRECATION")
    private fun calculateScreenBounds(): ScreenBounds? {
        val wm = windowManager ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            val windowInsets = WindowInsetsCompat.toWindowInsetsCompat(metrics.windowInsets, null)
            // Reserve space for system bars even when hidden, so the timer stays reachable when they return.
            val insets = windowInsets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
            val bounds = metrics.bounds
            ScreenBounds(
                width = bounds.width() - insets.left - insets.right,
                height = bounds.height() - insets.top - insets.bottom,
            )
        } else {
            val display = wm.defaultDisplay ?: return null
            val point = android.graphics.Point()
            display.getRealSize(point)
            ScreenBounds(width = point.x, height = point.y)
        }
    }

    companion object {
        private const val EXIT_ANIMATION_DURATION_MS = 250L
        private const val SUMMARY_DISPLAY_DURATION_MS = 1200L
        private const val TIMER_ALLOWED_HEIGHT_FRACTION = 0.8f
    }

    /**
     * Keeps drag events on the timer container instead of handing them to its text view.
     * This makes the whole timer draggable, including touches that start on the text.
     */
    private inner class DragInterceptFrameLayout(context: Context) : FrameLayout(context) {
        override fun onConfigurationChanged(newConfig: Configuration) {
            super.onConfigurationChanged(newConfig)
            screenBounds = calculateScreenBounds()
            keepTimerInAllowedArea()
        }

        override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
            return true
        }
    }

    /**
     * Holds the area used to keep the timer on screen.
     * Drag and snap calculations share these dimensions so the timer stays reachable.
     */
    private data class ScreenBounds(val width: Int, val height: Int)
}
