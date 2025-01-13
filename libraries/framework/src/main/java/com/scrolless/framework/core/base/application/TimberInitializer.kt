/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.application

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class TimberInitializer : AppInitializer {
    override fun init(application: CoreApplication<*>) {
        if (application.appConfig().isDev()) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(FireBaseCrashlyticsTree())
        }
    }

    internal class FireBaseCrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            when (priority) {
                Log.VERBOSE, Log.DEBUG -> return
            }

            val exception = t ?: Exception(message)

            // Initialize FirebaseCrashlytics instance
            val crashlytics = FirebaseCrashlytics.getInstance()

            // Log the message
            crashlytics.log("$tag: $message")

            // Set custom key (optional but useful for filtering)
            tag?.let { crashlytics.setCustomKey("TAG", it) }

            // Record the exception
            crashlytics.recordException(exception)
        }
    }
}
