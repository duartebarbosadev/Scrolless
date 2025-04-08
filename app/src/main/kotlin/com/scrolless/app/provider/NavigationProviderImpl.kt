/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.provider

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import com.scrolless.app.features.dialogs.AccessibilitySuccessDialog
import com.scrolless.app.features.dialogs.HelpDialog
import com.scrolless.app.features.main.MainActivity
import com.scrolless.framework.extensions.launchActivity

class NavigationProviderImpl(private val context: Context) : NavigationProvider {
    override fun launchMainActivity() {
        context.launchActivity<MainActivity> {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    override fun launchHelpDialog(childFragmentManager: FragmentManager) {
        HelpDialog.newInstance().show(childFragmentManager, HelpDialog.TAG)
    }

    override fun launchAccessibilityGrantedDialog(childFragmentManager: FragmentManager) {
        AccessibilitySuccessDialog.newInstance().show(childFragmentManager, AccessibilitySuccessDialog.TAG)
    }
}
