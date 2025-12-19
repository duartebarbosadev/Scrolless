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
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.google.android.play.core.review.ReviewManagerFactory
import com.scrolless.app.BuildConfig
import timber.log.Timber

private const val GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending"

fun requestAppReview(activity: Activity) {
    if (activity.isFinishing || activity.isDestroyed) {
        return
    }

    val reviewManager = ReviewManagerFactory.create(activity)

    if (BuildConfig.DEBUG) {
        // Skip in-app review in debug builds
        openPlayStore(activity, BuildConfig.APPLICATION_ID.removeSuffix(".debug"))
        return
    }

    val installerPackageName = getInstallerPackageName(activity)
    if (installerPackageName != GOOGLE_PLAY_STORE_PACKAGE) {
        Timber.w(
            "In-app review unavailable: installed by %s (expected %s); falling back to Play Store listing",
            installerPackageName ?: "unknown",
            GOOGLE_PLAY_STORE_PACKAGE,
        )
        openPlayStore(activity, BuildConfig.APPLICATION_ID)
        return
    }

    reviewManager.requestReviewFlow().addOnCompleteListener { request ->
        if (request.isSuccessful) {
            val reviewInfo = request.result
            reviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener { launch ->
                if (!launch.isSuccessful) {
                    Timber.w(
                        launch.exception,
                        "In-app review launch failed; falling back to Play Store listing",
                    )
                    openPlayStore(activity, BuildConfig.APPLICATION_ID)
                }
            }
        } else {
            Timber.w(
                request.exception,
                "In-app review request failed; falling back to Play Store listing",
            )
            openPlayStore(activity, BuildConfig.APPLICATION_ID)
        }
    }
}

private fun getInstallerPackageName(context: Context): String? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getInstallerPackageName(context.packageName)
    }
}.getOrNull()

private fun openPlayStore(context: Context, packageName: String) {
    try {
        val intent =
            Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        context.startActivity(intent)
    } catch (_: Exception) {
        val intent =
            Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        context.startActivity(intent)
    }
}
