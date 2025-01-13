/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.provider

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

class ThemeProviderImpl : ThemeProvider {

    override fun isDeviceUsingNightMode(context: Context): Boolean =
        context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    override fun isLightTheme(context: Context): Boolean = !isDeviceUsingNightMode(context)

    override fun setNightMode(theme: ThemeProvider.Theme) {
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                ThemeProvider.Theme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeProvider.Theme.NIGHT -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeProvider.Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            },
        )
    }
}
