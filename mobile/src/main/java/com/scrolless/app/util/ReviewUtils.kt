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
package com.scrolless.app.util

import android.app.Activity
import android.content.Context
import android.os.Build
import com.google.android.play.core.review.ReviewManagerFactory
import com.scrolless.app.BuildConfig
import timber.log.Timber

private const val GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending"

/**
 * Requests an in-app review using the Google Play In-App Review API.
 */
fun requestAppReview(activity: Activity, onResult: (Boolean) -> Unit) {

    if (!isActivityActive(activity)) {
        onResult(false)
        return
    }

    if (BuildConfig.DEBUG) {
        // Skip in-app review in debug builds
        onResult(false)
        return
    }

    val installerPackageName = getInstallerPackageName(activity)
    if (installerPackageName != GOOGLE_PLAY_STORE_PACKAGE) {
        Timber.w(
            "In-app review unavailable: installed by %s (expected %s);",
            installerPackageName ?: "unknown",
            GOOGLE_PLAY_STORE_PACKAGE,
        )
        onResult(false)
        return
    }

    val reviewManager = ReviewManagerFactory.create(activity)
    reviewManager.requestReviewFlow().addOnCompleteListener { request ->
        if (request.isSuccessful) {
            val reviewInfo = request.result
            if (!isActivityActive(activity)) {
                Timber.w("In-app review launch skipped: activity no longer active")
                onResult(false)
                return@addOnCompleteListener
            }
            reviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener { launch ->
                if (!launch.isSuccessful) {
                    Timber.w(
                        launch.exception,
                        "In-app review launch failed",
                    )
                }
                onResult(launch.isSuccessful)
            }
        } else {
            Timber.w(
                request.exception,
                "In-app review request failed",
            )
            onResult(false)
        }
    }
}

private fun isActivityActive(activity: Activity): Boolean = !activity.isFinishing && !activity.isDestroyed

private fun getInstallerPackageName(context: Context): String? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getInstallerPackageName(context.packageName)
    }
}.getOrNull()
