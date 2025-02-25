/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.base.mvvm

import androidx.viewbinding.ViewBinding
import com.scrolless.components.ProgressDialog
import com.scrolless.framework.core.base.mvvm.MvvmActivity
import com.scrolless.framework.core.base.mvvm.MvvmViewModel
import com.scrolless.framework.extensions.showSnackBar
import timber.log.Timber

abstract class BaseMvvmActivity<VB : ViewBinding, VM : MvvmViewModel> : MvvmActivity<VB, VM>() {
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
