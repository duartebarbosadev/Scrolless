/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.navigation

import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class FragNav(
    val manager: FragmentManager?,
    val fragment: Fragment?,
    val dialogFragment: DialogFragment?,
    val animationType: AnimationType,
    val view: View?,
    val transitionType: TransitionType,
    val addToBackStack: Boolean,
    val viewId: Int,
    val tag: String?,
    val clearBackStack: Boolean
) {

    private constructor(builder: Builder) : this(
        builder.manager,
        builder.fragment,
        builder.dialogFragment,
        builder.animationType,
        builder.view,
        builder.transitionType,
        builder.addToBackStack,
        builder.viewId,
        builder.tag,
        builder.clearBackStack,
    )

    companion object {
        inline fun create(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        var manager: FragmentManager? = null
            private set
        var fragment: Fragment? = null
            private set
        var dialogFragment: DialogFragment? = null
            private set
        var animationType: AnimationType = AnimationType.NO_ANIM
            private set
        var view: View? = null
            private set
        var transitionType: TransitionType = TransitionType.REPLACE
            private set
        var addToBackStack: Boolean = false
            private set
        var viewId: Int = -1
            private set
        var tag: String? = null
            private set
        var clearBackStack: Boolean = false
            private set

        fun setViewId(viewId: Int) = apply { this.viewId = viewId }

        fun setView(view: View) = apply { this.view = view }

        fun setTag(tag: String) = apply { this.tag = tag }

        fun setAddToBackStack(addToBackStack: Boolean) =
            apply { this.addToBackStack = addToBackStack }

        fun setAnimation(animationType: AnimationType) =
            apply { this.animationType = animationType }

        fun setTransitionType(type: TransitionType) = apply { this.transitionType = type }

        fun setClearBackStack(clearBackStack: Boolean) =
            apply { this.clearBackStack = clearBackStack }

        fun setFragmentManager(manager: FragmentManager?) = apply { this.manager = manager }

        fun setFragment(fragment: Fragment) = apply {
            if (fragment is DialogFragment) {
                this.dialogFragment = fragment
            } else {
                this.fragment = fragment
                this.tag = fragment.javaClass.simpleName
            }
        }

        fun build() = FragNav(this)
    }
}
