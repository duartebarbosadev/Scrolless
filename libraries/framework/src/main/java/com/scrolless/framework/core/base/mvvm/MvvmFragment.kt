/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.mvvm

import androidx.viewbinding.ViewBinding
import com.scrolless.framework.core.base.core.CoreFragment
import com.scrolless.framework.extensions.observeFlowStart
import com.scrolless.framework.extensions.observeLiveData

abstract class MvvmFragment<VB : ViewBinding, VM : MvvmViewModel> : CoreFragment<VB>() {

    abstract val viewModel: VM

    open fun showProgress() {}

    open fun hideProgress() {}

    open fun showError(throwable: Throwable) {}

    override fun observeUi() {
        super.observeUi()
        observeProgress()
        observeError()
    }

    private fun observeProgress() {
        observeFlowStart(viewModel.progress) { state ->
            state?.let {
                if (it) {
                    showProgress()
                } else {
                    hideProgress()
                }
            }
        }
    }

    private fun observeError() {
        observeLiveData(viewModel.error, ::showError)
    }
}
