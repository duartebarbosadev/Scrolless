/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.application

class AppInitializerImpl(private vararg val initializers: AppInitializer) : AppInitializer {
    override fun init(application: CoreApplication<*>) {
        initializers.forEach {
            it.init(application)
        }
    }
}
