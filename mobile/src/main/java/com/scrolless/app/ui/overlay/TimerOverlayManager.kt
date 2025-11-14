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
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.scrolless.app.core.data.repository.UsageTracker
import com.scrolless.app.core.data.repository.UserSettingsStore
import com.scrolless.app.core.data.repository.setTimerOverlayPosition
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.util.formatAsTime
import jakarta.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Implementation of TimerOverlayManager that displays a timer overlay on top of the brain rot content.
 */
class TimerOverlayManager @Inject constructor(private val userSettingsStore: UserSettingsStore, private val usageTracker: UsageTracker) {

    private var composeView: ComposeView? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: WindowLifecycleOwner? = null

    private lateinit var serviceContext: Context
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var sessionStartTime = 0L
    private var velocityTracker: VelocityTracker? = null
    private var flingAnimator: ValueAnimator? = null
    private var exitAnimator: ValueAnimator? = null
    private var exitAnimationJob: Job? = null
    private val overlayModeState = mutableStateOf(OverlayMode.Timer)
    private val summaryTextState = mutableStateOf("")
    private var screenBounds: ScreenBounds? = null

    fun attachServiceContext(context: Context) {
        serviceContext = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun show() {
        if (composeView != null) {
            cancelPendingExitAnimations(resetViewState = false)
            cleanupView()
        }
        if (!::serviceContext.isInitialized) {
            Timber.w("Timer overlay requested before service context was attached")
            return
        }
        val wm = windowManager
        if (wm == null) {
            Timber.w("WindowManager not attached, cannot show timer overlay")
            return
        }

        // Subtract 1 second to account for initial delay in showing overlay
        // This ensures the timer starts closer to the actual session start
        sessionStartTime = System.currentTimeMillis() - 1000L

        // Create lifecycle owner
        lifecycleOwner = WindowLifecycleOwner()

        // Create Compose view
        composeView = ComposeView(serviceContext).apply {
            // Set up lifecycle owner before setContent
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                ScrollessTheme {
                    TimerOverlayContent(
                        sessionStartTime = sessionStartTime,
                        displayMode = overlayModeState.value,
                        summaryText = summaryTextState.value,
                    )
                }
            }
        }

        // Get saved position from StateFlow
        val positionX = (userSettingsStore.getTimerOverlayPositionX() as StateFlow<Int>).value
        val positionY = (userSettingsStore.getTimerOverlayPositionY() as StateFlow<Int>).value

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = positionX
            y = positionY
        }

        // Cache current screen bounds to avoid querying on every drag/update.
        screenBounds = calculateScreenBounds()

        overlayModeState.value = OverlayMode.Timer
        summaryTextState.value = ""

        // Attach drag listener immediately so touches during fade are still captured.
        setupDragListener()

        try {
            // Start invisible for fade-in
            composeView?.alpha = 0f
            wm.addView(composeView, layoutParams)

            // Move lifecycle to RESUMED state after view is attached
            lifecycleOwner
                ?.takeIf {
                    it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                        !it.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                }
                ?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            composeView?.post { startEnterAnimation() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to show overlay")
            cleanupView()
        }
    }

    fun hide() {
        composeView ?: return
        exitAnimationJob?.cancel()
        val totalMillis = usageTracker.getDailyUsage() + computeActiveSessionMillis()
        summaryTextState.value = totalMillis.formatAsTime()
        overlayModeState.value = OverlayMode.Summary
        exitAnimationJob = coroutineScope.launch {
            delay(SUMMARY_DISPLAY_DURATION_MS)
            startExitAnimation()
        }
    }

    fun cleanup() {
        cancelPendingExitAnimations(resetViewState = false)
        cleanupView()
        coroutineScope.cancel()
    }

    private fun cleanupView() {
        cancelPendingExitAnimations(resetViewState = false)
        destroyLifecycleOwner()

        try {
            composeView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove overlay")
        } finally {
            exitAnimator?.cancel()
            exitAnimator = null
            exitAnimationJob?.cancel()
            exitAnimationJob = null
            flingAnimator?.cancel()
            flingAnimator = null
            velocityTracker?.recycle()
            velocityTracker = null
            screenBounds = null
            composeView?.setOnTouchListener(null)
            composeView = null
            layoutParams = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        composeView?.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            val wm = windowManager ?: return@setOnTouchListener false
            val bounds = resolveScreenBounds()
            val viewWidth = composeView?.width ?: 0
            val viewHeight = composeView?.height ?: 0
            val maxX = bounds?.let { (it.width - viewWidth).coerceAtLeast(0) } ?: Int.MAX_VALUE
            val maxY = bounds?.let { (it.height - viewHeight).coerceAtLeast(0) } ?: Int.MAX_VALUE

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    flingAnimator?.cancel()
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(event)
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    params.x = (initialX - deltaX).coerceIn(0, maxX)
                    params.y = (initialY + deltaY).coerceIn(0, maxY)
                    try {
                        wm.updateViewLayout(composeView, params)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update overlay position")
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    val velocityY = velocityTracker?.yVelocity ?: 0f
                    velocityTracker?.recycle()
                    velocityTracker = null
                    snapToNearestEdge(velocityX, velocityY)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.recycle()
                    velocityTracker = null
                    snapToNearestEdge(0f, 0f)
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToNearestEdge(velocityX: Float, velocityY: Float) {
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val bounds = resolveScreenBounds() ?: return
        val viewWidth = composeView?.width ?: 0
        val viewHeight = composeView?.height ?: 0
        val maxX = (bounds.width - viewWidth).coerceAtLeast(0)
        val maxY = (bounds.height - viewHeight).coerceAtLeast(0)

        val distanceToRight = params.x
        val distanceToLeft = (maxX - params.x).coerceAtLeast(0)
        val snapToRight = when {
            abs(velocityX) > FLING_VELOCITY_THRESHOLD -> velocityX >= 0f
            else -> distanceToRight <= distanceToLeft
        }

        val projectedY = (params.y + (velocityY * FLING_VERTICAL_MULTIPLIER)).toInt()
        val targetX = if (snapToRight) 0 else maxX
        val targetY = projectedY.coerceIn(0, maxY)

        animateToPosition(wm, targetX, targetY, persist = true)
    }

    private fun animateToPosition(wm: WindowManager, targetX: Int, targetY: Int, persist: Boolean) {
        val params = layoutParams ?: return
        val startX = params.x
        val startY = params.y

        if (startX == targetX && startY == targetY) {
            if (persist) {
                persistOverlayPosition(targetX, targetY)
            }
            return
        }

        flingAnimator?.cancel()
        flingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                params.x = startX + ((targetX - startX) * fraction).toInt()
                params.y = startY + ((targetY - startY) * fraction).toInt()
                try {
                    wm.updateViewLayout(composeView, params)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to animate overlay position")
                }
            }
            if (persist) {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        persistOverlayPosition(targetX, targetY)
                    }
                })
            }
        }
        flingAnimator?.start()
    }

    private fun persistOverlayPosition(x: Int, y: Int) {
        coroutineScope.launch {
            try {
                userSettingsStore.setTimerOverlayPosition(x, y)
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist overlay position")
            }
        }
    }

    private fun cancelPendingExitAnimations(resetViewState: Boolean) {
        exitAnimationJob?.cancel()
        exitAnimationJob = null
        exitAnimator?.cancel()
        exitAnimator = null
        if (resetViewState) {
            composeView?.apply {
                translationX = 0f
                alpha = 1f
            }
        }
    }

    private fun startEnterAnimation() {
        val view = composeView ?: return
        val direction = if (isAnchoredRight()) 1 else -1
        val distance = view.width.takeIf { it > 0 } ?: view.measuredWidth
        view.translationX = direction * distance.toFloat()
        view.alpha = 0f
        view.animate()
            ?.translationX(0f)
            ?.alpha(1f)
            ?.setDuration(250)
            ?.setInterpolator(DecelerateInterpolator())
            ?.start()
    }

    private fun startExitAnimation() {
        val view = composeView ?: return
        val direction = if (isAnchoredRight()) 1 else -1
        val distance = (view.width.takeIf { it > 0 } ?: view.measuredWidth).toFloat().coerceAtLeast(1f)
        val startTranslation = view.translationX
        val targetTranslation = direction * distance
        val startAlpha = if (view.alpha > 0f) view.alpha else 1f

        exitAnimator?.cancel()
        exitAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = EXIT_ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                view.translationX = startTranslation + ((targetTranslation - startTranslation) * fraction)
                view.alpha = startAlpha * (1f - fraction)
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    exitAnimator = null
                    if (!cancelled) {
                        cleanupView()
                    }
                }
            })
            start()
        }
    }

    private fun isAnchoredRight(): Boolean {
        val params = layoutParams ?: return true
        val bounds = resolveScreenBounds() ?: return true
        val viewWidth = composeView?.width ?: 0
        val maxX = (bounds.width - viewWidth).coerceAtLeast(0)
        if (maxX == 0) return true
        return params.x <= maxX / 2
    }

    private fun resolveScreenBounds(): ScreenBounds? {
        val cached = screenBounds
        if (cached != null) return cached
        val calculated = calculateScreenBounds()
        screenBounds = calculated
        return calculated
    }

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
            val point = Point()
            display.getRealSize(point)
            ScreenBounds(width = point.x, height = point.y)
        }
    }

    private fun computeActiveSessionMillis(): Long = (System.currentTimeMillis() - sessionStartTime).coerceAtLeast(0L)

    private data class ScreenBounds(val width: Int, val height: Int)

    companion object {
        private const val FLING_VELOCITY_THRESHOLD = 800f
        private const val FLING_VERTICAL_MULTIPLIER = 0.15f
        private const val EXIT_ANIMATION_DURATION_MS = 250L
        private const val SUMMARY_DISPLAY_DURATION_MS = 1200L
    }

    private fun destroyLifecycleOwner() {
        lifecycleOwner?.let { owner ->
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                owner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            }
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
            owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        lifecycleOwner = null
    }
}

/**
 * A lifecycle owner for a view that is added directly to the WindowManager.
 *
 * This is required for Compose views that are not part of an Activity.
 */
private class WindowLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val _viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        // Overlay state is ephemeral, so we intentionally skip restoring from a Bundle.
        // Provide a persisted bundle here if we ever need to survive process death.
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
        if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) {
            savedStateRegistryController.performSave(Bundle())
        }
        if (event == Lifecycle.Event.ON_DESTROY) {
            viewModelStore.clear()
        }
    }
}

@Composable
private fun TimerOverlayContent(sessionStartTime: Long, displayMode: OverlayMode, summaryText: String) {
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val wiggleRotation = remember { Animatable(0f) }

    LaunchedEffect(sessionStartTime, displayMode) {
        if (displayMode == OverlayMode.Timer) {
            while (isActive) {
                elapsedTime = System.currentTimeMillis() - sessionStartTime
                delay(1000)
            }
        } else {
            elapsedTime = System.currentTimeMillis() - sessionStartTime
        }
    }

    LaunchedEffect(displayMode) {
        if (displayMode == OverlayMode.Summary) {
            wiggleRotation.snapTo(0f)
            val targets = listOf(8f, -8f, 5f, -5f, 3f, -3f, 0f)
            targets.forEach { angle ->
                wiggleRotation.animateTo(angle, animationSpec = tween(durationMillis = 70))
            }
        } else {
            wiggleRotation.animateTo(0f, animationSpec = tween(durationMillis = 150))
        }
    }

    val textToDisplay = when (displayMode) {
        OverlayMode.Timer -> elapsedTime.formatAsTime()
        OverlayMode.Summary -> summaryText
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                rotationZ = wiggleRotation.value
            }
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = textToDisplay,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Preview(name = "Duration (01:23)")
@Composable
private fun TimerOverlayContentPreviewShort() {
    ScrollessTheme {
        TimerOverlayContent(
            sessionStartTime = System.currentTimeMillis() - 83_000L,
            displayMode = OverlayMode.Timer,
            summaryText = "",
        )
    }
}

@Preview(name = "Summary")
@Composable
private fun TimerOverlaySummaryPreview() {
    ScrollessTheme {
        TimerOverlayContent(
            sessionStartTime = System.currentTimeMillis() - 83_000L,
            displayMode = OverlayMode.Summary,
            summaryText = "12:34",
        )
    }
}

private enum class OverlayMode {
    Timer,
    Summary,
}
