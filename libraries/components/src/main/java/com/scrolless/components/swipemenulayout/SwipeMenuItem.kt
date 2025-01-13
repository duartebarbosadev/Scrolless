/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.components.swipemenulayout

import android.graphics.Typeface
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.LinearLayoutCompat

@SuppressWarnings("TooManyFunctions", "MagicNumber")
data class SwipeMenuItem @JvmOverloads constructor(
    @ColorRes var backgroundColor: Int = 0,
    @DrawableRes var icon: Int = 0,
    @ColorRes var iconColor: Int = 0,
    var title: CharSequence = "",
    @ColorRes var titleColor: Int = 0,
    var titleSize: Int = 10,
    var textTypeface: Typeface? = null,
    var textAppearance: Int = 0,
    var width: Int = -2,
    var height: Int = -2,
    var weight: Int = 0,
    @LinearLayoutCompat.OrientationMode
    var orientation: Int = LinearLayoutCompat.VERTICAL,
    var tag: Any? = null,
    var menuItemClick: (position: Int) -> Unit = {}
) {

    class Builder {
        private val item = SwipeMenuItem()

        fun setBackgroundColor(@ColorRes idRes: Int): Builder {
            item.backgroundColor = idRes
            return this
        }

        fun setTitle(title: CharSequence): Builder {
            item.title = title
            return this
        }

        fun setTitleSize(size: Int): Builder {
            item.titleSize = size
            return this
        }

        fun setTitleColor(@ColorRes idRes: Int): Builder {
            item.titleColor = idRes
            return this
        }

        fun setIcon(@DrawableRes idRes: Int): Builder {
            item.icon = idRes
            return this
        }

        fun setIconColor(@ColorRes idRes: Int): Builder {
            item.iconColor = idRes
            return this
        }

        fun setWidth(width: Int): Builder {
            item.width = width
            return this
        }

        fun setHeight(height: Int): Builder {
            item.height = height
            return this
        }

        fun setWeight(weight: Int): Builder {
            item.weight = weight
            return this
        }

        fun setOrientation(@LinearLayoutCompat.OrientationMode orientation: Int): Builder {
            item.orientation = orientation
            return this
        }

        fun setTextTypeface(textTypeface: Typeface): Builder {
            item.textTypeface = textTypeface
            return this
        }

        fun setTextAppearance(@StyleRes textAppearance: Int): Builder {
            item.textAppearance = textAppearance
            return this
        }

        fun setTag(tag: Any): Builder {
            item.tag = tag
            return this
        }

        fun setMenuItemClick(clicked: (position: Int) -> Unit): Builder {
            item.menuItemClick = clicked
            return this
        }

        fun build(): SwipeMenuItem = item
    }
}

@DslMarker
annotation class SwipeItemDsl

@SwipeItemDsl
class SwipeMenuItemDslBuilder {
    @ColorRes
    var backgroundColor: Int = 0

    @DrawableRes
    var iconRes: Int = 0

    @ColorRes
    var iconColor: Int = 0
    var title: CharSequence = ""

    @ColorRes
    var titleColor: Int = 0
    var titleSize: Int = 10
    var textTypeface: Typeface? = null
    var textAppearance: Int = 0
    var width: Int = -2
    var height: Int = -2
    var weight: Int = 0

    @LinearLayoutCompat.OrientationMode
    var orientation: Int = LinearLayoutCompat.VERTICAL
    var tag: Any? = null
    var menuItemClick: (position: Int) -> Unit = {}

    fun build(): SwipeMenuItem =
        SwipeMenuItem(
            backgroundColor,
            iconRes,
            iconColor,
            title,
            titleColor,
            titleSize,
            textTypeface,
            textAppearance,
            width,
            height,
            weight,
            orientation,
            tag,
            menuItemClick,
        )
}

@SwipeItemDsl
class ListItemConfigAdder : ArrayList<SwipeMenuItem>() {
    inline fun menuItem(block: SwipeMenuItemDslBuilder.() -> Unit) {
        add(SwipeMenuItemDslBuilder().apply(block).build())
    }

    fun menuItem(config: SwipeMenuItem) {
        add(config)
    }

    fun listOfMenuItems(configs: List<SwipeMenuItem>) {
        addAll(configs)
    }
}

inline fun menuItems(block: ListItemConfigAdder.() -> Unit): List<SwipeMenuItem> =
    ListItemConfigAdder().apply(block)
