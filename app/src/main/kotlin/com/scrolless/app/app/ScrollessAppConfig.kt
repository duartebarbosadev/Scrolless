/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.app

import android.content.Context
import com.scrolless.app.BuildConfig
import com.scrolless.app.features.main.MainActivity
import com.scrolless.framework.core.base.application.CoreConfig
import com.scrolless.framework.core.base.application.CoreEnvironment

class ScrollessAppConfig : CoreConfig() {
    override fun appName(): String = "Scrolless"

    override fun environment(): CoreEnvironment =
        if (isDev()) {
            CoreEnvironment.DEV
        } else {
            CoreEnvironment.PROD
        }

    override fun timeOut(): Long = 30L

    override fun isDev(): Boolean = BuildConfig.DEBUG

    override fun uncaughtExceptionPage(): Class<*> = MainActivity::class.java

    override fun uncaughtExceptionMessage(): String = "Unknown Error"

    override fun getPlayStoreUrl(context: Context): String {
        // Get app namespace
        val packageName = context.packageName
        val baseNamespace = packageName
            .replace(".dev", "")
            .replace(".debug", "")

        return "https://play.google.com/store/apps/details?id=$baseNamespace"
    }
}
