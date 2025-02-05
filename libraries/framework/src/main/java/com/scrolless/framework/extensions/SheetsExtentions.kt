/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.maxkeppeler.sheets.duration.DurationSheet

/** Build and show [DurationSheet] directly. */
fun DurationSheet.show(
    context: Context,
    fragmentManager: FragmentManager,
    func: DurationSheet.() -> Unit
): DurationSheet {
    this.windowContext = context
    this.func()

    show(fragmentManager, dialogTag)
    return this
}