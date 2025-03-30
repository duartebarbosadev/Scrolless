/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.provider

import androidx.fragment.app.FragmentManager

interface NavigationProvider {

    fun launchMainActivity()
    fun launchHelpDialog(childFragmentManager: FragmentManager)
}
