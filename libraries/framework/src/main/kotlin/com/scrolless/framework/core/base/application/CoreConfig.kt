/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.application

abstract class CoreConfig {
    abstract fun appName(): String

    abstract fun environment(): CoreEnvironment

    abstract fun timeOut(): Long

    open fun isDev() = false

    open fun uncaughtExceptionPage(): Class<*>? = null

    open fun uncaughtExceptionMessage(): String? = null
}
