/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.core

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import androidx.viewbinding.ViewBinding
import com.scrolless.framework.core.base.binding.BindingComponent

abstract class CoreComponent<VB : ViewBinding>(
    context: Context,
    attrs: AttributeSet? = null
) : BindingComponent<VB>(context, attrs) {
    abstract fun setupAttribute(attrs: AttributeSet?)

    init {
        this.setupAttribute(attrs)
    }

    fun disableAutoFocusOfTextField() {
        this.isFocusable = true
        this.isFocusableInTouchMode = true
        this.requestFocus()
    }

    fun closeKeyboard() {
        // Check if no view has focus:
        val view = (context as Activity).currentFocus

        if (view != null) {
            val service = context.getSystemService(Context.INPUT_METHOD_SERVICE)
            val manager = service as InputMethodManager
            manager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
