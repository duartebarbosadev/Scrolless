/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
plugins {
    alias(libs.plugins.scrolless.android.library)
}

android {
    namespace = "com.scrolless.libraries.components"

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Project Modules
    implementation(projects.libraries.framework)
    testImplementation(projects.libraries.testutils)

    // AndroidX Libraries
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.paging.runtime.ktx)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Sheets Libraries
    implementation(libs.sheets.core)

    // Testing Libraries
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.espresso.core)
}
