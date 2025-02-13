/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.splash

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import com.scrolless.app.R
import com.scrolless.app.base.mvi.BaseMviActivity
import com.scrolless.app.databinding.ActivitySplashBinding
import com.scrolless.app.provider.NavigationProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity :
    BaseMviActivity<ActivitySplashBinding, SplashContract.State, SplashViewModel>() {
    @Inject
    lateinit var navigationProvider: NavigationProvider

    override val viewModel: SplashViewModel by viewModels()
    override fun onViewReady(bundle: Bundle?) {
        openMainActivityHomeScreen()
    }

    override fun renderViewState(viewState: SplashContract.State) {
    }

    private fun openMainActivityHomeScreen() {
        navigationProvider.launchMainActivity()
        overridePendingTransition(
            R.anim.fade_in,
            R.anim.splash_fade_out,
        )
    }
}
