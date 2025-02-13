/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.application

interface CoreConfigProvider<T : CoreConfig> {
    fun appConfig(): T
}
