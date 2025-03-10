/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.content.Context
import android.provider.Settings

/**
 * Checks if an accessibility service is enabled for the current application.
 *
 * @param serviceClass The class of the accessibility service to check.
 * @return `true` if the accessibility service is enabled, `false` otherwise.
 */
fun Context.isAccessibilityServiceEnabled(serviceClass: Class<*>): Boolean {
    val enabledServices = Settings.Secure.getString(
        this.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    )
    val serviceName = "${this.packageName}/${serviceClass.name}"
    return enabledServices?.contains(serviceName) == true
}
