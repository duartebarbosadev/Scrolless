/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.mvi

import androidx.viewbinding.ViewBinding
import com.scrolless.framework.core.base.mvvm.MvvmSheetDialog
import com.scrolless.framework.extensions.observeFlowStart

abstract class MviSheetDialog<VB : ViewBinding, STATE, VM : MviViewModel<STATE, *>> :
    MvvmSheetDialog<VB, VM>() {
    abstract fun renderViewState(viewState: STATE)

    override fun observeUi() {
        super.observeUi()
        observeFlowStart(viewModel.stateFlow, ::renderViewState)
    }
}
