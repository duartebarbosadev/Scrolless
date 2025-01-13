/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.scrolless.framework.extensions.helper.DividerItemDecorator
import timber.log.Timber

var RecyclerView.isScrollable: Boolean
    set(value) {
        ViewCompat.setNestedScrollingEnabled(this, value)
    }
    get() = ViewCompat.isNestedScrollingEnabled(this)

fun RecyclerView.getOrientation(): Int? =
    if (layoutManager is LinearLayoutManager) {
        val layoutManager = layoutManager as LinearLayoutManager
        layoutManager.orientation.orZero()
    } else {
        null
    }

fun RecyclerView.addDividerDrawable(dividerDrawable: Drawable) {
    getOrientation()?.let { orientation ->
        val dividerItemDecoration = DividerItemDecorator(dividerDrawable, orientation)
        addItemDecoration(dividerItemDecoration)
    }
}

fun RecyclerView.setItemDecoration(left: Int, top: Int, right: Int, bottom: Int) {
    addItemDecoration(
        object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.left = context.dp2px(left)
                outRect.top = context.dp2px(top)
                outRect.right = context.dp2px(right)
                outRect.bottom = context.dp2px(bottom)
            }
        },
    )
}

fun RecyclerView.setAppBarElevationListener(appBar: AppBarLayout?) {
    addOnScrollListener(
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                appBar?.let {
                    if (canScrollVertically(-1)) {
                        ViewCompat.setElevation(it, 6f)
                    } else {
                        ViewCompat.setElevation(it, 0f)
                    }
                }
            }
        },
    )
}

fun RecyclerView.setSnapHelper() {
    val snapHelper = LinearSnapHelper()
    snapHelper.attachToRecyclerView(this)
}

fun RecyclerView.setDefaultLayoutManager() {
    if (layoutManager != null) return
    layoutManager = LinearLayoutManager(context)
}

fun RecyclerView.smoothSnapToPosition(
    position: Int,
    snapMode: Int = LinearSmoothScroller.SNAP_TO_START
) {
    val currentItemCount = adapter?.itemCount ?: 0
    if (position !in 0 until currentItemCount) {
        Timber.w("Invalid scroll position: $position, current item count: $currentItemCount")
        return
    }

    val smoothScroller = object : LinearSmoothScroller(this.context) {
        override fun getVerticalSnapPreference(): Int = snapMode
        override fun getHorizontalSnapPreference(): Int = snapMode

        // Override this to add validation at the final step
        override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
            val itemCountAtFinalStep = adapter?.itemCount ?: 0
            if (targetPosition in 0 until itemCountAtFinalStep) {
                super.onTargetFound(targetView, state, action)
            } else {
                Timber.w("Target position is no longer valid: $targetPosition")
                // Handle cases where the target item is removed or moved within the adapter
            }
        }
    }

    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}
