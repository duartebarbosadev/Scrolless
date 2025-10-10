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

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.scrolless.app.core.data.repository.UserSettingsStore
import com.scrolless.app.core.data.repository.setTimerOverlayPosition
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.util.formatAsTime
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
class TimerOverlayManager @Inject constructor(private val userSettingsStore: UserSettingsStore) {

    private var composeView: ComposeView? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: MyLifecycleOwner? = null

    private lateinit var serviceContext: Context
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var sessionStartTime = 0L

    fun attachServiceContext(context: Context) {
        serviceContext = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun show() {
        if (composeView != null) return

        // Check overlay permission
        if (!canDrawOverlays()) {
            Timber.i("Overlay permission not granted")
            return
        }

        // Subtract 1 second to account for initial delay in showing overlay
        // This ensures the timer starts closer to the actual session start
        sessionStartTime = System.currentTimeMillis() - 1000L

        // Create lifecycle owner
        lifecycleOwner = MyLifecycleOwner()

        // Create Compose view
        composeView = ComposeView(serviceContext).apply {
            // Set up lifecycle owner before setContent
            setViewTreeLifecycleOwner(lifecycleOwner)

            setContent {
                ScrollessTheme {
                    TimerOverlayContent(
                        sessionStartTime = sessionStartTime,
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

        try {
            // Start invisible for fade-in
            composeView?.alpha = 0f
            windowManager?.addView(composeView, layoutParams)

            // Move lifecycle to RESUMED state after view is attached
            lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            // Fade in
            composeView?.animate()
                ?.alpha(1f)
                ?.setDuration(200)
                ?.withEndAction {
                    // Enable dragging after fade-in completes to avoid race conditions
                    enableDragging()
                }
                ?.start()
        } catch (e: Exception) {
            Timber.e(e, "Failed to show overlay")
            cleanupView()
        }
    }

    fun hide() {
        val view = composeView ?: return

        // Fade out
        view.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                cleanupView()
            }
            ?.start()
    }

    fun cleanup() {
        hide()
        coroutineScope.cancel()
    }

    private fun cleanupView() {
        // Destroy lifecycle
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleOwner = null

        try {
            composeView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove overlay")
        } finally {
            composeView = null
        }
    }

    private fun enableDragging() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        composeView?.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            val wm = windowManager ?: return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        wm.updateViewLayout(composeView, params)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update overlay position")
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Save position
                    coroutineScope.launch {
                        try {
                            userSettingsStore.setTimerOverlayPosition(params.x, params.y)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to save overlay position")
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(serviceContext)
    }
}

/**
 * Minimal lifecycle owner for ComposeView in overlay.
 * Only provides lifecycle management without ViewModel or SavedState support.
 */
private class MyLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}

@Composable
private fun TimerOverlayContent(sessionStartTime: Long) {
    var elapsedTime by remember { mutableLongStateOf(0L) }

    // Update timer every second
    LaunchedEffect(sessionStartTime) {
        while (isActive) {
            elapsedTime = System.currentTimeMillis() - sessionStartTime
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Timer text
            Text(
                text = elapsedTime.formatAsTime(),
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
        TimerOverlayContent(sessionStartTime = System.currentTimeMillis() - 83_000L)
    }
}
