/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.maxkeppeler.sheets.clock.ClockSheet
import com.maxkeppeler.sheets.duration.DurationSheet
import com.maxkeppeler.sheets.info.InfoSheet
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.option.OptionSheet

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

fun ClockSheet.show(
    context: Context,
    fragmentManager: FragmentManager,
    func: ClockSheet.() -> Unit
): ClockSheet {
    this.windowContext = context
    this.func()

    show(fragmentManager, dialogTag)
    return this
}

fun OptionSheet.show(
    context: Context,
    fragmentManager: FragmentManager,
    func: OptionSheet.() -> Unit
): OptionSheet {
    this.windowContext = context
    this.func()

    show(fragmentManager, dialogTag)
    return this
}

fun InfoSheet.show(
    context: Context,
    fragmentManager: FragmentManager,
    func: InfoSheet.() -> Unit
): InfoSheet {
    this.windowContext = context
    this.func()

    show(fragmentManager, dialogTag)
    return this
}

fun InputSheet.show(
    context: Context,
    fragmentManager: FragmentManager,
    func: InputSheet.() -> Unit
): InputSheet {
    this.windowContext = context
    this.func()

    show(fragmentManager, dialogTag)
    return this
}
