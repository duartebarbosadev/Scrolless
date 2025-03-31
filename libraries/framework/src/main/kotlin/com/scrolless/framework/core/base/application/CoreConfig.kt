/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.application

import android.content.Context

abstract class CoreConfig {
    abstract fun appName(): String

    abstract fun environment(): CoreEnvironment

    abstract fun timeOut(): Long

    open fun isDev() = false

    open fun uncaughtExceptionPage(): Class<*>? = null

    open fun uncaughtExceptionMessage(): String? = null

    open fun getPlayStoreUrl(context: Context): String? = null
}
