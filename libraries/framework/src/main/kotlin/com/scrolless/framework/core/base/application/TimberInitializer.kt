/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.application

import timber.log.Timber

class TimberInitializer : AppInitializer {
    override fun init(application: CoreApplication<*>) {
        if (application.appConfig().isDev()) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
