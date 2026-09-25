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

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.net.toUri
import timber.log.Timber

internal data class SettingsComponent(val packageName: String, val className: String)

/**
 * OEM skins that kill background apps (and with them the accessibility service) unless the
 * user explicitly allows autostart / background activity for the app.
 */
internal enum class BackgroundRestrictionOem(@StringRes val instructionRes: Int, val settingsComponents: List<SettingsComponent>) {
    Xiaomi(
        instructionRes = R.string.background_instruction_xiaomi,
        settingsComponents = listOf(
            SettingsComponent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            SettingsComponent("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"),
        ),
    ),
    Huawei(
        instructionRes = R.string.background_instruction_huawei,
        settingsComponents = listOf(
            SettingsComponent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            SettingsComponent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        ),
    ),
    Honor(
        instructionRes = R.string.background_instruction_huawei,
        settingsComponents = listOf(
            SettingsComponent("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            SettingsComponent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ),
    ),
    Oppo(
        instructionRes = R.string.background_instruction_oppo,
        settingsComponents = listOf(
            SettingsComponent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            SettingsComponent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            SettingsComponent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        ),
    ),
    OnePlus(
        instructionRes = R.string.background_instruction_oneplus,
        settingsComponents = listOf(
            SettingsComponent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            SettingsComponent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ),
    ),
    Vivo(
        instructionRes = R.string.background_instruction_vivo,
        settingsComponents = listOf(
            SettingsComponent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            SettingsComponent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            SettingsComponent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        ),
    ),
    Samsung(
        instructionRes = R.string.background_instruction_samsung,
        settingsComponents = emptyList(),
    ),
    ;

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

        fun current(): BackgroundRestrictionOem? = from(Build.MANUFACTURER, Build.BRAND)
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

    for (intent in candidates) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Timber.i("Opened background settings: %s", intent.component ?: intent.action)
            return
        } catch (e: ActivityNotFoundException) {
            Timber.d("Background settings not available: %s", intent.component)
        } catch (e: SecurityException) {
            Timber.d(e, "Background settings not accessible: %s", intent.component)
        }
    }
    Timber.w("No background settings screen could be opened")
}
