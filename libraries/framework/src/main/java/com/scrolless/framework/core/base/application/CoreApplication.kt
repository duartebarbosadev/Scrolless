/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.application

import android.app.Application
import android.content.Intent
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.system.exitProcess

abstract class CoreApplication<T : CoreConfig> :
    Application(),
    LifecycleEventObserver,
    CoreConfigProvider<T> {
    var isAppInForeground: Boolean = true

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(ActivityLifecycleCallback())
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> Unit
            Lifecycle.Event.ON_START -> onAppForegrounded()
            Lifecycle.Event.ON_RESUME -> Unit
            Lifecycle.Event.ON_PAUSE -> Unit
            Lifecycle.Event.ON_STOP -> onAppBackgrounded()
            Lifecycle.Event.ON_DESTROY -> Unit
            Lifecycle.Event.ON_ANY -> Unit
        }
    }

    open fun onAppBackgrounded() {
        isAppInForeground = false
    }

    open fun onAppForegrounded() {
        isAppInForeground = true
    }

    open fun handleSSLHandshakeException() {}

    private fun handleUncaughtException() {
        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            object : Thread() {
                override fun run() {
                    Looper.prepare()
                    Toast.makeText(
                        applicationContext,
                        appConfig().uncaughtExceptionMessage(),
                        Toast.LENGTH_SHORT,
                    ).show()
                    Looper.loop()
                }
            }.start()

            Thread.sleep(2000)

            val intent = Intent(this, appConfig().uncaughtExceptionPage())
            // to custom behaviour, add extra params for intent
            intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    or Intent.FLAG_ACTIVITY_NEW_TASK,
            )
            startActivity(intent)
            try {
                exitProcess(2)
            } catch (e: Exception) {
                startActivity(intent)
            }
        }
    }
}
