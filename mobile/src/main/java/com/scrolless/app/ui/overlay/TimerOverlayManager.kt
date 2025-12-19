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
package com.scrolless.app.ui.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import com.scrolless.app.R
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.repository.UsageTracker
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.core.repository.setTimerOverlayPosition
import com.scrolless.app.designsystem.theme.timerOverlayBackgroundColor
import com.scrolless.app.util.formatAsTime
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A View-based implementation of TimerOverlayManager.
 * Replaces the Compose-based version to resolve drag lag issues.
 */
class TimerOverlayManager @Inject constructor(private val userSettingsStore: UserSettingsStore, private val usageTracker: UsageTracker) {

    private var rootView: DragInterceptFrameLayout? = null
    private var timerTextView: TextView? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var snapAnimator: ValueAnimator? = null
    private var velocityTracker: android.view.VelocityTracker? = null

    private lateinit var serviceContext: Context
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var sessionStartTime = 0L
    private var timerJob: Job? = null
    private var exitAnimationJob: Job? = null
    private var screenBounds: ScreenBounds? = null
    private var activeBlockOption: BlockOption = BlockOption.NothingSelected
    private var intervalUsageMillis: Long = 0L

    // Drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    init {
        coroutineScope.launch {
            userSettingsStore.getActiveBlockOption().collect { option ->
                activeBlockOption = option
            }
        }
        coroutineScope.launch {
            userSettingsStore.getIntervalUsage().collect { usage ->
                intervalUsageMillis = usage
            }
        }
    }

    fun attachServiceContext(context: Context) {
        serviceContext = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun show() {
        if (rootView != null) {
            cleanupView()
        }
        if (!::serviceContext.isInitialized) {
            Timber.w("Timer overlay requested before service context was attached")
            return
        }
        val wm = windowManager ?: return

        // Subtract 1 second to account for initial delay
        sessionStartTime = System.currentTimeMillis() - 1000L

        // Create TextView with polished styling
        timerTextView = TextView(serviceContext).apply {
            text = resources.getText(R.string.timer_default_value)
            textSize = 18f // sp
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)

            val paddingH = dpToPx(20f)
            val paddingV = dpToPx(12f)
            setPadding(paddingH, paddingV, paddingH, paddingV)

            background = GradientDrawable().apply {
                setColor(timerOverlayBackgroundColor)
                cornerRadius = dpToPx(24f).toFloat()
            }
            elevation = dpToPx(8f).toFloat()
            gravity = Gravity.CENTER
        }

        // Wrap in DragInterceptFrameLayout
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
        }

        // Get saved position
        val positionX = (userSettingsStore.getTimerOverlayPositionX() as StateFlow<Int>).value
        val positionY = (userSettingsStore.getTimerOverlayPositionY() as StateFlow<Int>).value

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = positionX
            y = positionY
        }

        // Cache current screen bounds to avoid querying on every drag/update.
        screenBounds = calculateScreenBounds()

        try {
            // Start invisible for enter animation
            rootView?.alpha = 0f
            wm.addView(rootView, layoutParams)

            startTimer()
            rootView?.post { startEnterAnimation() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to show overlay")
            cleanupView()
        }
    }

    fun hide() {

        Timber.d("Hiding overlay view")
        timerJob?.cancel()

        val sessionMillis = (System.currentTimeMillis() - sessionStartTime).coerceAtLeast(0L)
        val totalMillis = if (activeBlockOption == BlockOption.IntervalTimer) {
            intervalUsageMillis + sessionMillis
        } else {
            usageTracker.getDailyUsage() + sessionMillis
        }

        timerTextView?.text = totalMillis.formatAsTime()
        startWiggleAnimation()

        exitAnimationJob?.cancel()
        exitAnimationJob = coroutineScope.launch {
            delay(SUMMARY_DISPLAY_DURATION_MS)
            startExitAnimation()
        }
    }

    fun cleanup() {
        cleanupView()
        coroutineScope.cancel()
    }

    private fun cleanupView() {
        val view = rootView
        rootView = null // Prevent re-entry
        timerTextView = null

        if (view != null) {
            try {
                view.animate().cancel()
                view.visibility = View.GONE
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove overlay")
            }
        }

        timerJob?.cancel()
        timerJob = null
        exitAnimationJob?.cancel()
        exitAnimationJob = null
        snapAnimator?.cancel()
        snapAnimator = null
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - sessionStartTime
                timerTextView?.text = elapsed.formatAsTime()
                delay(1000)
            }
        }
    }

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
        // Wiggle rotation targets: 8f, -8f, 5f, -5f, 3f, -3f, 0f
        // We can use a Keyframe-based ObjectAnimator or just a sequence.
        // PropertyValuesHolder with Keyframes is cleanest.

        val rotation = PropertyValuesHolder.ofFloat(View.ROTATION, 0f, 8f, -8f, 5f, -5f, 3f, -3f, 0f)
        ObjectAnimator.ofPropertyValuesHolder(view, rotation).apply {
            duration = 500 // ~71ms per keyframe (7 steps)
            start()
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false
        val wm = windowManager ?: return false
        val bounds = screenBounds ?: calculateScreenBounds().also { screenBounds = it } ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                snapAnimator?.cancel()
                velocityTracker?.recycle()
                velocityTracker = android.view.VelocityTracker.obtain()
                velocityTracker?.addMovement(event)

                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()

                params.x = initialX - deltaX
                params.y = initialY + deltaY

                try {
                    wm.updateViewLayout(rootView, params)
                } catch (e: Exception) {
                    Timber.e(e)
                }
                isDragging = true
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)

                if (isDragging) {
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    val velocityY = velocityTracker?.yVelocity ?: 0f

                    val viewWidth = rootView?.width ?: 0
                    val viewHeight = rootView?.height ?: 0
                    val minX = 0
                    val maxX = (bounds.width - viewWidth).coerceAtLeast(0)
                    val minY = 0
                    val maxY = (bounds.height - viewHeight).coerceAtLeast(0)

                    val currentX = params.x.coerceIn(minX, maxX)
                    val currentY = params.y.coerceIn(minY, maxY)

                    val flingThreshold = 1000f // pixels/sec

                    var targetX = currentX
                    var targetY = currentY

                    // Check for fling
                    if (abs(velocityX) > flingThreshold || abs(velocityY) > flingThreshold) {
                        // Fling detected. Calculate trajectory to find which edge is hit first.

                        // Time to hit horizontal edges (X axis)
                        // x(t) = x0 - vx * t
                        // Target x is 0 (Right) or maxX (Left)
                        val tX = if (velocityX > 0) {
                            currentX.toFloat() / velocityX // Time to reach 0
                        } else if (velocityX < 0) {
                            (currentX - maxX).toFloat() / velocityX // Time to reach maxX
                        } else {
                            Float.POSITIVE_INFINITY
                        }

                        // Time to hit vertical edges (Y axis)
                        // y(t) = y0 + vy * t
                        // Target y is 0 (Top) or maxY (Bottom)
                        val tY = if (velocityY > 0) {
                            (maxY - currentY).toFloat() / velocityY // Time to reach maxY
                        } else if (velocityY < 0) {
                            -currentY.toFloat() / velocityY // Time to reach 0
                        } else {
                            Float.POSITIVE_INFINITY
                        }

                        // Find the earliest impact time
                        // We only care about positive times (future), but the formulas above guarantee positive t
                        // for the correct direction.
                        val t = minOf(tX, tY)

                        // Calculate intersection point at time t
                        // x = x0 - vx * t
                        // y = y0 + vy * t
                        targetX = (currentX - velocityX * t).toInt().coerceIn(minX, maxX)
                        targetY = (currentY + velocityY * t).toInt().coerceIn(minY, maxY)
                    } else {
                        // No fling, snap to nearest edge
                        // Distances to edges
                        // Remember Gravity.END: x is distance from right edge.
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

                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return false
    }

    private fun snapToPosition(targetX: Int, targetY: Int) {
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val startX = params.x
        val startY = params.y

        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                params.x = (startX + (targetX - startX) * fraction).toInt()
                params.y = (startY + (targetY - startY) * fraction).toInt()
                try {
                    wm.updateViewLayout(rootView, params)
                } catch (_: Exception) {
                    // Ignore
                }
            }
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        persistOverlayPosition(params.x, params.y)
                    }
                },
            )
            start()
        }
    }

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
    }

    /**
     * Intercepts touches to ensure exclusive handling by the OnTouchListener.
     */
    private class DragInterceptFrameLayout(context: Context) : FrameLayout(context) {
        override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
            return true
        }
    }

    private data class ScreenBounds(val width: Int, val height: Int)
}
