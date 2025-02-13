/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */

import com.scrolless.app.ScrollessBuildType

plugins {
    alias(libs.plugins.scrolless.android.application)
    alias(libs.plugins.scrolless.android.application.flavors)
    alias(libs.plugins.scrolless.hilt)
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

    lint {
        lintConfig = file("lint.xml")
    }

    packaging {
        resources { // Make sure to exclude the license files as for some reason they are probing project from compilation
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

    // Other libraries
    implementation(libs.timber)

    // Sheets libraries
    implementation(libs.sheets.core)
    implementation(libs.sheets.duration)

    // Test dependencies
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.espresso.core)
}