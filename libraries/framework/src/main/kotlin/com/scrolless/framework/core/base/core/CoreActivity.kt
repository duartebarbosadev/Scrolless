/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.core

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.scrolless.framework.core.base.binding.BindingActivity
import com.scrolless.framework.extensions.hideSoftKeyboard

abstract class CoreActivity<VB : ViewBinding> : BindingActivity<VB>() {
    abstract fun onViewReady(bundle: Bundle?)

    open fun onViewListener() {}

    open fun observeUi() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeUi()
        onViewReady(savedInstanceState)
        onViewListener()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        takeIf { shouldHideKeyboardOnTouch() }?.let { handleFocus(ev) }
        return super.dispatchTouchEvent(ev)
    }

    private fun handleFocus(event: MotionEvent?) {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val view = currentFocus
                if (view is EditText) {
                    val outRect = Rect()
                    view.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        view.hideSoftKeyboard(shouldClearCurrentFocusOnTouch())
                    }
                }
            }
        }
    }

    protected open fun shouldHideKeyboardOnTouch() = true

    protected open fun shouldClearCurrentFocusOnTouch() = false

    fun setupEventEmptyView(view: View, isEmpty: Boolean) {
        if (isEmpty) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

    protected fun setupTabTitles(
        tabLayout: TabLayout,
        viewPager2: ViewPager2,
        titles: MutableList<String>
    ) {
        TabLayoutMediator(tabLayout, viewPager2) { tab: TabLayout.Tab, position: Int ->
            tab.text = titles[position]
        }.attach()
    }
}
