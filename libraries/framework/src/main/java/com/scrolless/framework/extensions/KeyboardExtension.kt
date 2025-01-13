/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

private fun Activity.findFocusedView() = currentFocus ?: window.decorView

private fun Fragment.findFocusedView() = activity?.currentFocus ?: view

fun View.showSoftKeyboard() {
    this.requestFocus()

    if (this.isFocused) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun View.hideSoftKeyboard(clearFocus: Boolean = false) {
    if (clearFocus) this.clearFocus()

    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun Activity.showSoftKeyboard() = findFocusedView().showSoftKeyboard()

fun Fragment.showSoftKeyboard() = findFocusedView()?.showSoftKeyboard()

fun Activity.hideSoftKeyboard(clearFocus: Boolean = false) =
    findFocusedView().hideSoftKeyboard(clearFocus)

fun Fragment.hideSoftKeyboard() {
    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(requireView().windowToken, 0)
}
