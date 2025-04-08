/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.main

import android.os.Bundle
import androidx.core.view.WindowCompat
import com.scrolless.app.base.BaseActivity
import com.scrolless.app.databinding.ActivityMainBinding
import com.scrolless.app.features.home.HomeFragment
import com.scrolless.framework.navigation.navigateFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    companion object {
        const val EXTRA_SHOW_ACCESSIBILITY_PERMISSION_GRANTED = "EXTRA_SHOW_ACCESSIBILITY_PERMISSION_GRANTED"
    }

    override fun onViewReady(bundle: Bundle?) {
        val accessibilityServiceGranted = intent.getBooleanExtra(EXTRA_SHOW_ACCESSIBILITY_PERMISSION_GRANTED, false)
        navigateFragment(
            HomeFragment.newInstance(accessibilityServiceGranted),
            addToBackStack = false,
            clearBackStack = true,
        )

        // Disable system insets from resizing your layout
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
