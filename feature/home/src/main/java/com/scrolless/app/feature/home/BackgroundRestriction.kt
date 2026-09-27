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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import timber.log.Timber

internal data class SettingsComponent(val packageName: String, val className: String)

/**
 * OEM skins that kill background apps (and with them the accessibility service) unless the
 * user explicitly allows autostart / background activity for the app.
 */
internal enum class BackgroundRestrictionOem(val settingsComponents: List<SettingsComponent>) {
    Xiaomi(
        settingsComponents = listOf(
            SettingsComponent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            SettingsComponent("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"),
        ),
    ),
    Huawei(
        settingsComponents = listOf(
            SettingsComponent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            SettingsComponent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        ),
    ),
    Honor(
        settingsComponents = listOf(
            SettingsComponent("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            SettingsComponent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ),
    ),
    Oppo(
        settingsComponents = listOf(
            SettingsComponent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            SettingsComponent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            SettingsComponent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        ),
    ),
    OnePlus(
        settingsComponents = listOf(
            SettingsComponent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            SettingsComponent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ),
    ),
    Vivo(
        settingsComponents = listOf(
            SettingsComponent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            SettingsComponent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            SettingsComponent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        ),
    ),
    Samsung(settingsComponents = emptyList()),
    ;

    /** Whether this OEM has its own autostart switch on top of Android's battery optimization. */
    val hasAutostartManager: Boolean get() = settingsComponents.isNotEmpty()

    companion object {
        fun from(manufacturer: String?, brand: String?): BackgroundRestrictionOem? {
            val names = listOfNotNull(manufacturer, brand).map { it.trim().lowercase() }
            fun matches(vararg candidates: String) = names.any { it in candidates }
            return when {
                matches("xiaomi", "redmi", "poco") -> Xiaomi
                matches("honor") -> Honor
                matches("huawei") -> Huawei
                matches("oneplus") -> OnePlus
                matches("oppo", "realme") -> Oppo
                matches("vivo", "iqoo") -> Vivo
                matches("samsung") -> Samsung
                else -> null
            }
        }

        fun current(): BackgroundRestrictionOem? = if (BuildConfig.DEBUG && DebugPhoneBrand.isSimulating) {
            DebugPhoneBrand.simulatedOem
        } else {
            from(Build.MANUFACTURER, Build.BRAND)
        }
    }
}

/**
 * Debug builds only: pretend to be another phone brand so each OEM flow can be tried on any device.
 * OEM settings screens that don't exist on this phone fall back to the app details page.
 */
internal object DebugPhoneBrand {
    var isSimulating by mutableStateOf(false)
    var simulatedOem by mutableStateOf<BackgroundRestrictionOem?>(null)
}

internal fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) ?: true

/** True on phones known to kill background apps while something is still left for the user to allow. */
internal fun Context.needsBackgroundSetup(oem: BackgroundRestrictionOem? = BackgroundRestrictionOem.current()): Boolean =
    oem != null && (oem.hasAutostartManager || !isIgnoringBatteryOptimizations())

/**
 * Shows the system "Stop optimizing battery usage?" dialog, falling back to the battery
 * optimization list when the dialog isn't available.
 */
@SuppressLint("BatteryLife")
internal fun Context.requestIgnoreBatteryOptimizations() {
    val candidates = listOf(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:$packageName".toUri()),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    )
    if (!startFirstAvailable(candidates)) openBackgroundSettings(oem = null)
}

/** Fixes the most important remaining background restriction with a single action. */
internal fun Context.fixBackgroundRestrictions(oem: BackgroundRestrictionOem? = BackgroundRestrictionOem.current()) {
    if (!isIgnoringBatteryOptimizations()) {
        requestIgnoreBatteryOptimizations()
    } else {
        openBackgroundSettings(oem)
    }
}

/**
 * Opens the best available screen for letting Scrolless run in the background: the OEM autostart
 * manager when present, otherwise the app details page (battery / background settings).
 */
internal fun Context.openBackgroundSettings(oem: BackgroundRestrictionOem? = BackgroundRestrictionOem.current()) {
    val candidates = oem?.settingsComponents.orEmpty().map { component ->
        Intent().setComponent(ComponentName(component.packageName, component.className))
    } + Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())

    if (!startFirstAvailable(candidates)) Timber.w("No background settings screen could be opened")
}

private fun Context.startFirstAvailable(candidates: List<Intent>): Boolean {
    // Launch inside our own task when possible. With NEW_TASK these settings screens join the
    // Settings app's task, which brings back whatever settings page was open there before.
    val activity = findActivity()
    val launcher: Context = activity ?: this
    for (intent in candidates) {
        if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            launcher.startActivity(intent)
            Timber.i("Opened background settings: %s", intent.component ?: intent.action)
            return true
        } catch (e: ActivityNotFoundException) {
            Timber.d("Background settings not available: %s", intent.component ?: intent.action)
        } catch (e: SecurityException) {
            Timber.d(e, "Background settings not accessible: %s", intent.component ?: intent.action)
        }
    }
    return false
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
