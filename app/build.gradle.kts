/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */

import com.android.build.api.dsl.BuildType
import com.scrolless.app.ScrollessBuildType

plugins {
    alias(libs.plugins.scrolless.android.application)
    alias(libs.plugins.scrolless.android.application.flavors)
    alias(libs.plugins.scrolless.android.application.firebase)
    alias(libs.plugins.scrolless.android.room)
    alias(libs.plugins.scrolless.hilt)
    alias(libs.plugins.baselineprofile)
    id("com.google.android.gms.oss-licenses-plugin")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.scrolless.app"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.scrolless.app"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0" // X.Y.Z; X = Major, Y = minor, Z = Patch level
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }

    buildTypes {

        release {

            applicationIdSuffix = ScrollessBuildType.RELEASE.applicationIdSuffix
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Ensure Baseline Profile is fresh for release builds.
            baselineProfile.automaticGenerationDuringBuild = true
        }
        debug {

            applicationIdSuffix = ScrollessBuildType.DEBUG.applicationIdSuffix
            isDebuggable = true
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

private fun BuildType.buildConfigStringField(
    name: String,
    value: String
) {
    this.buildConfigField("String", name, "\"$value\"")
}

dependencies {
    // Project modules
    implementation(projects.libraries.framework)
    implementation(projects.libraries.components)
    implementation(libs.androidx.preference.ktx)
    testImplementation(projects.libraries.testutils)

    implementation(libs.androidx.viewbinding)

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.security.crypto)

    implementation(libs.workmanager)

    // Other libraries
    implementation(libs.timber)
    implementation(libs.coil)
    implementation(libs.tink.android)
    implementation(libs.contacts)

    // Calendar libraries
    implementation(libs.calendar.core)
    implementation(libs.calendar.jsr310)
    implementation(libs.calendar.emoji)

    // Sheets libraries
    implementation(libs.sheets.core)
    implementation(libs.sheets.lottie)
    implementation(libs.sheets.duration)
    implementation(libs.sheets.clock)
    implementation(libs.sheets.option)
    implementation(libs.sheets.info)
    implementation(libs.sheets.input)

    // Debug dependencies
    debugImplementation(libs.debug.db)

    // Test dependencies
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.espresso.core)

    implementation(platform(libs.firebase.bom))

    implementation(libs.firebase.crashlytics)

    implementation(libs.google.oss.licenses)

    implementation(libs.facebook.shimmer)
}

baselineProfile {
    // Don't build on every iteration of a full assemble.
    // Instead enable generation directly for the release build variant.
    automaticGenerationDuringBuild = false

    // Make use of Dex Layout Optimizations via Startup Profiles
    dexLayoutOptimization = true
}
