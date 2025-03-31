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

    override fun onViewReady(bundle: Bundle?) {
        navigateFragment(
            HomeFragment.newInstance(),
            addToBackStack = false,
            clearBackStack = true,
        )

        // Disable system insets from resizing your layout
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
