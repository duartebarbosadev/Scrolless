/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.base.mvi

import androidx.viewbinding.ViewBinding
import com.scrolless.components.ProgressDialog
import com.scrolless.framework.core.base.mvi.MviActivity
import com.scrolless.framework.core.base.mvi.MviViewModel
import com.scrolless.framework.extensions.showSnackBar
import timber.log.Timber

abstract class BaseMviActivity<VB : ViewBinding, STATE, VM : MviViewModel<STATE, *>> :
    MviActivity<VB, STATE, VM>() {
    private var progressDialog: ProgressDialog? = null

    override fun showProgress() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
        }
        progressDialog?.show()
    }

    override fun hideProgress() {
        progressDialog?.dismiss()
    }

    override fun showError(throwable: Throwable) {
        handleErrorMessage(throwable.message.toString())
    }

    protected open fun handleErrorMessage(message: String?) {
        if (message.isNullOrBlank()) return
        hideProgress()
        Timber.e(message)
        showSnackBar(binding.root, message)
    }
}
