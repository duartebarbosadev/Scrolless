/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.provider

import android.content.Context

interface ThemeProvider {
    enum class Theme {
        SYSTEM,
        NIGHT,
        LIGHT
    }

    /**
     * Whether the current configuration is a dark theme i.e. in Night configuration.
     */
    fun isDeviceUsingNightMode(context: Context): Boolean

    /**
     * Whether the current configuration is a light theme i.e. in Day configuration.
     */
    fun isLightTheme(context: Context): Boolean

    /**
     * Force [AppCompatDelegate] Mode to night/notnight.
     *
     * @param forceNight Boolean that force night mode otherwise notnight is configured.
     */
    fun setNightMode(theme: Theme)
}
