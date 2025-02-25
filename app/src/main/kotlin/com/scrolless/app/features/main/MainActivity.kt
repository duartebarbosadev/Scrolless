/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.main

import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.scrolless.app.R
import com.scrolless.app.base.BaseActivity
import com.scrolless.app.databinding.ActivityMainBinding
import com.scrolless.app.features.home.HomeFragment
import com.scrolless.framework.extensions.showSnackBar
import com.scrolless.framework.navigation.navigateFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var backPressedOnce = false

    override fun onViewReady(bundle: Bundle?) {
        navigateFragment(
            HomeFragment.newInstance(),
            clearBackStack = true,
        )

        // Disable system insets from resizing your layout
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            super.onBackPressed()
        } else {
            if (backPressedOnce) {
                finish()
                return
            }

            navigateFragment(
                HomeFragment.newInstance(),
                clearBackStack = true,
            )

            backPressedOnce = true
            showSnackBar(
                binding.rootView,
                getString(R.string.app_exit_label),
                R.id.rootView,
            )
            lifecycleScope.launch {
                delay(2000)
                backPressedOnce = false
            }
        }
    }
}
