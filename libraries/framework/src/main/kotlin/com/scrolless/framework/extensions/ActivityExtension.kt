/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import java.io.Serializable

fun Activity.getRootView(): View =
    (this.findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)

fun <T : Serializable?> getSerializable(
    activity: Activity,
    name: String,
    clazz: Class<T>
): T =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        activity.intent.getSerializableExtra(name, clazz)!!
    } else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        activity.intent.getSerializableExtra(name) as T
    }
