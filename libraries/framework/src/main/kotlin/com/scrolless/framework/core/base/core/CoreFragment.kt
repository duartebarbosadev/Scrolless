/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.core

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.scrolless.framework.core.base.binding.BindingFragment

abstract class CoreFragment<VB : ViewBinding> : BindingFragment<VB>() {
    private var viewId: Int = -1

    abstract fun onViewReady(bundle: Bundle?)

    open fun onViewListener() {}

    open fun observeUi() {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewId = binding.root.id
        observeUi()
        onViewReady(savedInstanceState)
        onViewListener()
    }

    /**
     * Return the [AppCompatActivity] this fragment is currently associated with.
     *
     * @throws IllegalStateException if not currently associated with an activity or if associated
     * only with a context.
     * @throws TypeCastException if the currently associated activity don't extend [AppCompatActivity].
     *
     * @see requireActivity
     */
    protected fun requireCompatActivity(): AppCompatActivity {
        requireActivity()
        val activity = requireActivity()
        if (activity is AppCompatActivity) {
            return activity
        } else {
            throw TypeCastException("Main activity should extend from 'AppCompatActivity'")
        }
    }

    protected fun checkArgument(argsKey: String): Boolean = requireArguments().containsKey(argsKey)

    protected fun setupTabTitles(
        tabLayout: TabLayout,
        viewPager2: ViewPager2,
        titles: MutableList<String>
    ) {
        TabLayoutMediator(tabLayout, viewPager2) { tab: TabLayout.Tab, position: Int ->
            tab.text = titles[position]
        }.attach()
    }

    protected fun setupEventEmptyView(view: View, isEmpty: Boolean) {
        if (isEmpty) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }
}
