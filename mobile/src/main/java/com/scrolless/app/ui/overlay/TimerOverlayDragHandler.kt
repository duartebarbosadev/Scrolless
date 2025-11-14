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
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import timber.log.Timber

/**
 * Handles drag gestures, snapping, and fling animations for the timer overlay view.
 */
internal class TimerOverlayDragHandler(
    private val viewProvider: () -> View?,
    private val layoutParamsProvider: () -> WindowManager.LayoutParams?,
    private val windowManagerProvider: () -> WindowManager?,
    private val boundsProvider: () -> TimerOverlayManager.ScreenBounds?,
    private val persistPosition: (Int, Int) -> Unit,
) {

    private var velocityTracker: VelocityTracker? = null
    private var flingAnimator: ValueAnimator? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    fun attach() {
        viewProvider()?.setOnTouchListener { _, event ->
            val params = layoutParamsProvider() ?: return@setOnTouchListener false
            val wm = windowManagerProvider() ?: return@setOnTouchListener false
            val bounds = boundsProvider()
            val viewWidth = viewProvider()?.width ?: 0
            val viewHeight = viewProvider()?.height ?: 0
            val maxX = bounds?.let { (it.width - viewWidth).coerceAtLeast(0) } ?: Int.MAX_VALUE
            val maxY = bounds?.let { (it.height - viewHeight).coerceAtLeast(0) } ?: Int.MAX_VALUE

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    cancelFling()
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
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
                    tryUpdateLayout(wm, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    val velocityY = velocityTracker?.yVelocity ?: 0f
                    recycleVelocityTracker()
                    snapToNearestEdge(velocityX, velocityY)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    recycleVelocityTracker()
                    snapToNearestEdge(0f, 0f)
                    true
                }
                else -> false
            }
        }
    }

    fun detach() {
        viewProvider()?.setOnTouchListener(null)
        recycleVelocityTracker()
        cancelFling()
    }

    private fun tryUpdateLayout(windowManager: WindowManager, params: WindowManager.LayoutParams) {
        try {
            windowManager.updateViewLayout(viewProvider(), params)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update overlay position")
        }
    }

    private fun snapToNearestEdge(velocityX: Float, velocityY: Float) {
        val params = layoutParamsProvider() ?: return
        val wm = windowManagerProvider() ?: return
        val bounds = boundsProvider() ?: return
        val viewWidth = viewProvider()?.width ?: 0
        val viewHeight = viewProvider()?.height ?: 0
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

    private fun animateToPosition(windowManager: WindowManager, targetX: Int, targetY: Int, persist: Boolean) {
        val params = layoutParamsProvider() ?: return
        val startX = params.x
        val startY = params.y

        if (startX == targetX && startY == targetY) {
            if (persist) persistPosition(targetX, targetY)
            return
        }

        cancelFling()
        flingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                params.x = startX + ((targetX - startX) * fraction).toInt()
                params.y = startY + ((targetY - startY) * fraction).toInt()
                tryUpdateLayout(windowManager, params)
            }
            if (persist) {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        persistPosition(targetX, targetY)
                    }
                })
            }
            start()
        }
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun cancelFling() {
        flingAnimator?.cancel()
        flingAnimator = null
    }

    private companion object {
        private const val FLING_VELOCITY_THRESHOLD = 800f
        private const val FLING_VERTICAL_MULTIPLIER = 0.15f
    }
}
