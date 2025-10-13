/*
 * Copyright (C) 2025 Scrolless
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
package com.scrolless.app.core.logging

import android.util.Log
import timber.log.Timber

/**
 * Release logging tree that only logs warnings and errors.
 * Later we might want to integrate with crash reporting like Crashlytics.
 * For now, errors and warnings are logged to logcat.
 */
class ReleaseLogTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR || priority == Log.WARN) {
            if (t != null) {
                Log.println(priority, tag ?: "Scrolless", "$message\n${Log.getStackTraceString(t)}")
            } else {
                Log.println(priority, tag ?: "Scrolless", message)
            }
        }
    }
}
