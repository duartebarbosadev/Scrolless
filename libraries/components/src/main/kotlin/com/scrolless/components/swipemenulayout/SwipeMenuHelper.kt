/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.components.swipemenulayout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections

open class SwipeMenuHelper {
    private var mapStates = Collections.synchronizedMap(HashMap<String, MenuState>())
    private val mapLayouts = Collections.synchronizedMap(HashMap<String, SwipeMenuLayout>())
    private val stateChangeLock = Any()

    /**
     * Help to save and restore open/close state of the swipeLayout. Call this method
     * when you bind your view holder with the data object.
     *
     * @param swipeLayout swipeLayout of the current view.
     * @param id          a string that uniquely defines the data object of the current view.
     */
    fun bind(swipeLayout: SwipeMenuLayout, id: String) {
        mapLayouts.values.remove(swipeLayout)
        mapLayouts[id] = swipeLayout

        // first time binding.
        if (!mapStates.containsKey(id)) {
            mapStates[id] = MenuState.CLOSE
            swipeLayout.closeMenu()
        } else {
            val state = mapStates[id]!!
            if (state == MenuState.CLOSE) {
                swipeLayout.closeMenu()
            } else {
                if (swipeLayout.rightMenuEnable) {
                    swipeLayout.openRightMenu()
                } else {
                    swipeLayout.openLeftMenu()
                }
            }
        }
    }

    /**
     * Close all swipe layout.
     */
    fun closeAllItems() {
        synchronized(stateChangeLock) {
            for (entry in mapStates.entries) {
                entry.setValue(MenuState.CLOSE)
            }
            for (layout in mapLayouts.values) {
                layout.closeMenu()
            }
        }
    }

    /**
     * Open swipe first item.
     */
    private fun openFirstItem(target: MenuState) {
        val id = "0"
        synchronized(stateChangeLock) {
            mapStates[id] = target
            if (mapLayouts.containsKey(id)) {
                val layout = mapLayouts[id]
                if (target == MenuState.RIGHT_OPEN) {
                    layout?.openRightMenu()
                } else if (target == MenuState.LEFT_OPEN) {
                    layout?.openLeftMenu()
                }
            }
        }
    }

    /**
     * Close swipe first item.
     */
    fun showPreviewSwipe(target: MenuState) = CoroutineScope(Dispatchers.Main).launch {
        delay(500)
        openFirstItem(target)
        delay(1000)
        closeAllItems()
    }
}
