/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.components.adapter.swipe

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.scrolless.components.swipemenulayout.SwipeMenuHelper
import com.scrolless.components.swipemenulayout.SwipeMenuItem
import com.scrolless.components.swipemenulayout.SwipeMenuLayout
import com.scrolless.framework.core.adapter.BasicRecyclerAdapter
import com.scrolless.framework.core.adapter.BasicViewHolder
import com.scrolless.libraries.components.R
import java.lang.reflect.Field

abstract class BasicSwipeAdapter<T : Any, Binding : ViewBinding> :
    BasicRecyclerAdapter<T, Binding>() {
    var menuOpenThreshold = 0.3f
    var leftMenuEnable: Boolean = true
    var rightMenuEnable: Boolean = true

    var leftSwipeMenus: List<SwipeMenuItem>? = null
    var rightSwipeMenus: List<SwipeMenuItem>? = null

    lateinit var rv: RecyclerView

    protected val binderHelper = SwipeMenuHelper()

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        rv = recyclerView
        rv.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> Unit
                MotionEvent.ACTION_DOWN -> binderHelper.closeAllItems()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> binderHelper.closeAllItems()
            }
            false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicViewHolder<Binding> {
        val vh = super.onCreateViewHolder(parent, viewType)
        val mainView = LayoutInflater.from(parent.context)
            .inflate(R.layout.libraries_components_row_swipe_recycler, parent, false)

        val swipeContent = mainView.findViewById<FrameLayout>(R.id.swipeContent)
        swipeContent.addView(vh.itemView)

        try {
            val itemView: Field = getSupperClass(vh.javaClass).getDeclaredField("itemView")
            if (!itemView.isAccessible) itemView.isAccessible = true
            itemView[vh] = mainView
        } catch (ignored: Exception) {
        }

        return vh
    }

    override fun onBindViewHolder(holder: BasicViewHolder<Binding>, position: Int) {
        super.onBindViewHolder(holder, position)
        val itemView = holder.itemView as SwipeMenuLayout
        binderHelper.bind(itemView, "$position")
        itemView.menuOpenThreshold = menuOpenThreshold
        // create left menu
        if (leftSwipeMenus.isNullOrEmpty()) {
            itemView.leftMenuEnable = false
        } else {
            itemView.leftMenuEnable = leftMenuEnable
        }
        itemView.leftMenuView.createMenu(leftSwipeMenus, position)
        itemView.leftMenuView.setOnMenuItemClickListener { _, _ ->
            itemView.closeMenu()
        }

        // create right menu
        if (rightSwipeMenus.isNullOrEmpty()) {
            itemView.rightMenuEnable = false
        } else {
            itemView.rightMenuEnable = rightMenuEnable
        }
        itemView.rightMenuView.createMenu(rightSwipeMenus, position)
        itemView.rightMenuView.setOnMenuItemClickListener { _, _ ->
            itemView.closeMenu()
        }
    }

    private fun getSupperClass(aClass: Class<*>): Class<*> {
        val supperClass = aClass.superclass
        return if (supperClass != null && supperClass != Any::class.java) {
            getSupperClass(supperClass)
        } else {
            aClass
        }
    }
}
