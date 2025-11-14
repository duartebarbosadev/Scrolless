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
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
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
import com.scrolless.app.core.data.database.model.BlockOption
import com.scrolless.app.core.data.repository.UsageTracker
import com.scrolless.app.core.data.repository.UserSettingsStore
import com.scrolless.app.core.data.repository.setTimerOverlayPosition
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.util.formatAsTime
import jakarta.inject.Inject
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
    private var exitAnimator: ValueAnimator? = null
    private var exitAnimationJob: Job? = null
    private val overlayModeState = mutableStateOf(OverlayMode.Timer)
    private val summaryTextState = mutableStateOf("")
    private var screenBounds: ScreenBounds? = null
    private var activeBlockOption: BlockOption = BlockOption.NothingSelected
    private var intervalUsageMillis: Long = 0L
    private val dragHandler = TimerOverlayDragHandler(
        viewProvider = { composeView },
        layoutParamsProvider = { layoutParams },
        windowManagerProvider = { windowManager },
        boundsProvider = { resolveScreenBounds() },
        persistPosition = { x, y -> persistOverlayPosition(x, y) },
    )

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
        dragHandler.attach()

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
        val sessionMillis = computeActiveSessionMillis()
        val totalMillis = if (activeBlockOption == BlockOption.IntervalTimer) {
            intervalUsageMillis + sessionMillis
        } else {
            usageTracker.getDailyUsage() + sessionMillis
        }
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
            dragHandler.detach()
            screenBounds = null
            composeView = null
            layoutParams = null
        }
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

    internal data class ScreenBounds(val width: Int, val height: Int)

    companion object {
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
