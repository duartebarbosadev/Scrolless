/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

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

fun View.beInvisible() {
    visibility = View.INVISIBLE
}

fun View.beVisible() {
    visibility = View.VISIBLE
}

fun View.beGone() {
    visibility = View.GONE
}

fun View.onGlobalLayout(callback: () -> Unit) {
    viewTreeObserver?.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (viewTreeObserver != null) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    callback()
                }
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

fun View.fadeIn() {
    animate()
        .alpha(1f)
        .setDuration(150L)
        .withStartAction { beVisible() }
        .start()
}

fun View.fadeOut() {
    animate()
        .alpha(0f)
        .setDuration(150L)
        .withEndAction { beGone() }
        .start()
}

val Context.inflater: LayoutInflater get() = LayoutInflater.from(this)

fun ViewGroup.inflate(layoutRes: Int): View =
    LayoutInflater.from(context).inflate(layoutRes, this, false)

fun View.onFocusChanged(func: (Boolean) -> Unit) {
    this.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus -> func.invoke(hasFocus) }
}

fun View.setSafeOnClickListener(debounceTime: Long = 600L, action: () -> Unit) {
    this.setOnClickListener(
        object : View.OnClickListener {
            private var lastClickTime: Long = 0

            override fun onClick(v: View) {
                if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) {
                    return
                } else {
                    action()
                }

                lastClickTime = SystemClock.elapsedRealtime()
            }
        },
    )
}

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

fun View.updateMargins(
    start: Int? = null,
    top: Int? = null,
    end: Int? = null,
    bottom: Int? = null
) {
    if (this.layoutParams is ViewGroup.MarginLayoutParams) {
        val params = this.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(
            start ?: params.leftMargin,
            top ?: params.topMargin,
            end ?: params.rightMargin,
            bottom ?: params.bottomMargin,
        )
    }
}

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
            val lastTopPadding = view.getTag(R.id.view_add_insets_padding_top_tag) as? Int ?: 0
            val newTopPadding = inset.top
            view.setTag(R.id.view_add_insets_padding_top_tag, newTopPadding)
            view.updatePadding(top = view.paddingTop - lastTopPadding + newTopPadding)
        }
        if (bottom) {
            val lastBottomPadding =
                view.getTag(R.id.view_add_insets_padding_bottom_tag) as? Int ?: 0
            val newBottomPadding = inset.bottom
            view.setTag(R.id.view_add_insets_padding_bottom_tag, newBottomPadding)
            view.updatePadding(bottom = view.paddingBottom - lastBottomPadding + newBottomPadding)
        }
        if (left) {
            val lastLeftPadding = view.getTag(R.id.view_add_insets_padding_left_tag) as? Int ?: 0
            val newLeftPadding = inset.left
            view.setTag(R.id.view_add_insets_padding_left_tag, newLeftPadding)
            view.updatePadding(left = view.paddingLeft - lastLeftPadding + newLeftPadding)
        }
        if (right) {
            val lastRightPadding = view.getTag(R.id.view_add_insets_padding_right_tag) as? Int ?: 0
            val newRightPadding = inset.right
            view.setTag(R.id.view_add_insets_padding_right_tag, newRightPadding)
            view.updatePadding(right = view.paddingRight - lastRightPadding + newRightPadding)
        }
        return@setOnApplyWindowInsetsListener insets
    }
}
