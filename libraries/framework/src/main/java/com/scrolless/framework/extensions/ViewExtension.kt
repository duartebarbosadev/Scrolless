/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.scrolless.libraries.framework.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Animation durations and scale factors
private const val FADE_DURATION_MS = 150L
private const val SCALE_DOWN_DURATION_MS = 100L
private const val PAUSE1_DURATION_MS = 100L
private const val SCALE_UP_DURATION_MS = 200L
private const val PAUSE2_DURATION_MS = 400L
private const val FADE_OUT_DURATION_MS = 200L

private const val INITIAL_SCALE = 1f
private const val SCALE_DOWN_FACTOR = 0.9f
private const val SCALE_UP_FACTOR = 1.1f
private const val FINAL_SCALE = 0f

// Default debounce time for safe click listener
private const val DEFAULT_DEBOUNCE_TIME_MS = 600L

// Extension functions on View for visibility handling
fun View.beInvisible() {
    visibility = View.INVISIBLE
}

fun View.beVisible() {
    visibility = View.VISIBLE
}

fun View.beGone() {
    visibility = View.GONE
}

// Execute callback after the global layout
fun View.onGlobalLayout(callback: () -> Unit) {
    viewTreeObserver?.addOnGlobalLayoutListener(
        object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver?.removeOnGlobalLayoutListener(this)
                callback()
            }
        },
    )
}

fun View.isVisible() = visibility == View.VISIBLE

fun View.isInvisible() = visibility == View.INVISIBLE

fun View.isGone() = visibility == View.GONE

fun View.performHapticFeedback() = performHapticFeedback(
    HapticFeedbackConstants.VIRTUAL_KEY,
    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
)

// Fade in the view using the defined duration
fun View.fadeIn() {
    animate()
        .alpha(1f)
        .setDuration(FADE_DURATION_MS)
        .withStartAction { beVisible() }
        .start()
}

// Fade out the view using the defined duration
fun View.fadeOut() {
    animate()
        .alpha(0f)
        .setDuration(FADE_DURATION_MS)
        .withEndAction { beGone() }
        .start()
}

/**
 * Extension function for View that runs a hide animation sequence:
 * 1. Scale down from INITIAL_SCALE to SCALE_DOWN_FACTOR over SCALE_DOWN_DURATION_MS.
 * 2. Pause for PAUSE1_DURATION_MS.
 * 3. Scale up from SCALE_DOWN_FACTOR to SCALE_UP_FACTOR over SCALE_UP_DURATION_MS.
 * 4. Pause for PAUSE2_DURATION_MS.
 * 5. Fade out and shrink (scale to FINAL_SCALE and alpha to 0) over FADE_OUT_DURATION_MS.
 *
 * @param onAnimationEnd Called when the entire animation sequence finishes.
 */
fun View.fadeOutWithBounceAnimation(onAnimationEnd: () -> Unit) {
    val view = this

    // Helper: create a dummy animator that effectively pauses.
    fun pause(duration: Long) =
        ObjectAnimator.ofFloat(this, "alpha", this.alpha, this.alpha).apply {
            this.duration = duration
        }

    // 1. Scale down
    val scaleDown = AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(view, "scaleX", INITIAL_SCALE, SCALE_DOWN_FACTOR)
                .apply { duration = SCALE_DOWN_DURATION_MS },
            ObjectAnimator.ofFloat(view, "scaleY", INITIAL_SCALE, SCALE_DOWN_FACTOR)
                .apply { duration = SCALE_DOWN_DURATION_MS },
        )
    }

    // 2. Pause for PAUSE1_DURATION_MS.
    val pause1 = pause(PAUSE1_DURATION_MS)

    // 3. Scale up
    val scaleUp = AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(view, "scaleX", SCALE_DOWN_FACTOR, SCALE_UP_FACTOR)
                .apply { duration = SCALE_UP_DURATION_MS },
            ObjectAnimator.ofFloat(view, "scaleY", SCALE_DOWN_FACTOR, SCALE_UP_FACTOR)
                .apply { duration = SCALE_UP_DURATION_MS },
        )
    }

    // 4. Pause for PAUSE2_DURATION_MS.
    val pause2 = pause(PAUSE2_DURATION_MS)

    // 5. Fade out and shrink.
    val fadeOut = AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(view, "scaleX", SCALE_UP_FACTOR, FINAL_SCALE)
                .apply { duration = FADE_OUT_DURATION_MS },
            ObjectAnimator.ofFloat(view, "scaleY", SCALE_UP_FACTOR, FINAL_SCALE)
                .apply { duration = FADE_OUT_DURATION_MS },
            ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
                .apply { duration = FADE_OUT_DURATION_MS },
        )
    }

    // Chain and start the animation sequence.
    AnimatorSet().apply {
        playSequentially(scaleDown, pause1, scaleUp, pause2, fadeOut)
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd()
                }
            },
        )
        start()
    }
}

// Extension property on Context to obtain a LayoutInflater.
val Context.inflater: LayoutInflater
    get() = LayoutInflater.from(this)

// Extension function to inflate a layout from a ViewGroup.
fun ViewGroup.inflate(layoutRes: Int): View =
    LayoutInflater.from(context).inflate(layoutRes, this, false)

// Extension to handle focus change.
fun View.onFocusChanged(func: (Boolean) -> Unit) {
    onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        func(hasFocus)
    }
}

// Safe click listener with a debounce to avoid rapid multiple clicks.
fun View.setSafeOnClickListener(
    debounceTime: Long = DEFAULT_DEBOUNCE_TIME_MS,
    action: () -> Unit
) {
    setOnClickListener(
        object : View.OnClickListener {
            private var lastClickTime: Long = 0

            override fun onClick(v: View) {
                if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) return
                action()
                lastClickTime = SystemClock.elapsedRealtime()
            }
        },
    )
}

// Extension to delay a block execution, tied to the view's lifecycle.
fun View.delayOnLifecycle(
    durationInMillis: Long,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    block: () -> Unit
): Job? = findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
    lifecycleOwner.lifecycle.coroutineScope.launch(dispatcher) {
        delay(durationInMillis)
        block()
    }
}

// Extension function to update the margins of a view.
fun View.updateMargins(
    start: Int? = null,
    top: Int? = null,
    end: Int? = null,
    bottom: Int? = null
) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val params = layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(
            start ?: params.leftMargin,
            top ?: params.topMargin,
            end ?: params.rightMargin,
            bottom ?: params.bottomMargin,
        )
    }
}

// Extension to add window insets to the view's padding.
fun View.addInsetsByPadding(
    top: Boolean = false,
    bottom: Boolean = false,
    left: Boolean = false,
    right: Boolean = false
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val inset = Insets.max(
            insets.getInsets(WindowInsetsCompat.Type.systemBars()),
            insets.getInsets(WindowInsetsCompat.Type.displayCutout()),
        )
        if (top) {
            val lastTopPadding =
                view.getTag(R.id.view_add_insets_padding_top_tag) as? Int ?: 0
            val newTopPadding = inset.top
            view.setTag(R.id.view_add_insets_padding_top_tag, newTopPadding)
            view.updatePadding(top = view.paddingTop - lastTopPadding + newTopPadding)
        }
        if (bottom) {
            val lastBottomPadding =
                view.getTag(R.id.view_add_insets_padding_bottom_tag) as? Int ?: 0
            val newBottomPadding = inset.bottom
            view.setTag(R.id.view_add_insets_padding_bottom_tag, newBottomPadding)
            view.updatePadding(
                bottom = view.paddingBottom - lastBottomPadding + newBottomPadding,
            )
        }
        if (left) {
            val lastLeftPadding =
                view.getTag(R.id.view_add_insets_padding_left_tag) as? Int ?: 0
            val newLeftPadding = inset.left
            view.setTag(R.id.view_add_insets_padding_left_tag, newLeftPadding)
            view.updatePadding(left = view.paddingLeft - lastLeftPadding + newLeftPadding)
        }
        if (right) {
            val lastRightPadding =
                view.getTag(R.id.view_add_insets_padding_right_tag) as? Int ?: 0
            val newRightPadding = inset.right
            view.setTag(R.id.view_add_insets_padding_right_tag, newRightPadding)
            view.updatePadding(
                right = view.paddingRight - lastRightPadding + newRightPadding,
            )
        }
        insets
    }
}
