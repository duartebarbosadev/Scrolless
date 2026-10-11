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
package com.scrolless.app.designsystem.util

/**
 * Store screenshot mode for debug builds, switched on with
 * `adb shell setprop debug.scrolless.screenshot 1`. It hides debug-only UI so the
 * Play Store screenshots show the app as users see it.
 */
object ScreenshotMode {
    private const val PROPERTY = "debug.scrolless.screenshot"

    val isEnabled: Boolean
        get() = runCatching {
            // android.os.SystemProperties is hidden, but readable through reflection.
            val systemProperties = Class.forName("android.os.SystemProperties")
            systemProperties.getMethod("get", String::class.java).invoke(null, PROPERTY) == "1"
        }.getOrDefault(false)
}
