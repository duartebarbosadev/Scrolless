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
package com.scrolless.app.core.accessibility

/**
 * Whether the blocking accessibility service is currently connected in this process.
 *
 * The enabled-services setting keeps listing a service after an OEM battery manager kills its
 * process, so this in-memory flag is the only reliable liveness signal. It resets to `false`
 * whenever the process restarts and becomes `true` again once the system rebinds the service.
 */
object AccessibilityServiceConnection {
    @Volatile
    var isConnected: Boolean = false
        private set

    fun onConnected() {
        isConnected = true
    }

    fun onDisconnected() {
        isConnected = false
    }
}
