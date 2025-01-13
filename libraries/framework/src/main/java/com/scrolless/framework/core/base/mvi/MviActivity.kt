/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.mvi

import androidx.viewbinding.ViewBinding
import com.scrolless.framework.core.base.mvvm.MvvmActivity
import com.scrolless.framework.extensions.observeFlowStart

abstract class MviActivity<VB : ViewBinding, STATE, VM : MviViewModel<STATE, *>> :
    MvvmActivity<VB, VM>() {
    abstract fun renderViewState(viewState: STATE)

    override fun observeUi() {
        super.observeUi()
        observeFlowStart(viewModel.stateFlow, ::renderViewState)
    }
}
