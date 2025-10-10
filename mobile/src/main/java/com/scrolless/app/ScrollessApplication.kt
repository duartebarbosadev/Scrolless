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
package com.scrolless.app

import android.app.Application
import com.scrolless.app.core.logging.DebugLogTree
import com.scrolless.app.core.logging.ReleaseLogTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class
 */
@HiltAndroidApp
class ScrollessApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugLogTree())
        } else {
            Timber.plant(ReleaseLogTree())
        }
    }
}
