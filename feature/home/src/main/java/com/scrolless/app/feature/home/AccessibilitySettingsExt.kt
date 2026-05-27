/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.feature.home

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings

internal fun Context.isAccessibilityServiceEnabled(service: Class<out AccessibilityService>?): Boolean {
    if (service == null) return false

    val expectedComponentName = "$packageName/${service.name}"
    val enabledServicesSetting = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    return enabledServicesSetting.split(':').any { it.equals(expectedComponentName, ignoreCase = true) }
}

internal fun Context.openActivityAccessibilitySettings() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    if (isDebuggable) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
    startActivity(intent)
}

private val Context.isDebuggable: Boolean
    get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
