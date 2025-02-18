/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.app

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import com.scrolless.app.provider.AppProvider
import com.scrolless.app.provider.ThemeProvider
import com.scrolless.framework.core.base.application.AppInitializer
import com.scrolless.framework.core.base.application.CoreApplication
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * When using Hilt, the Application class must
 * be annotated with @HiltAndroidApp for kick
 * off the code generation.
 */
@HiltAndroidApp
class ScrollessApp : CoreApplication<ScrollessAppConfig>() {
    @Inject
    lateinit var initializer: AppInitializer

    @Inject
    lateinit var appProvider: AppProvider

    @Inject
    lateinit var themeProvider: ThemeProvider

    override fun appConfig(): ScrollessAppConfig = ScrollessAppConfig()

    override fun onCreate() {
        super.onCreate()
        initializer.init(this)

        setStrictModePolicy()
        initNightMode()
    }

    /**
     * Initialize Night Mode based on user last saved state (day/night themes).
     */
    private fun initNightMode() {
        var theme = appProvider.themeMode

        if (theme == ThemeProvider.Theme.SYSTEM) {
            theme = if (themeProvider.isDeviceUsingNightMode(applicationContext)) {
                ThemeProvider.Theme.NIGHT
            } else {
                ThemeProvider.Theme.LIGHT
            }
        }
        themeProvider.setNightMode(theme)
    }

    /**
     * Enables strict mode to detect and penalize main thread violations
     *  (e.g., network or disk access) during debugging.
     * Violations are logged and cause the app to crash. Only active in debuggable builds.
     */
    private fun setStrictModePolicy() {

        if (appConfig().isDev()) {
            StrictMode.setThreadPolicy(
                Builder().detectAll().penaltyLog().build(),
            )
        }
    }
}
