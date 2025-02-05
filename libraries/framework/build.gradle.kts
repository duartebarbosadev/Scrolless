/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
plugins {
    alias(libs.plugins.scrolless.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.scrolless.libraries.framework"
}

dependencies {
    implementation(libs.androidx.preference)
    testImplementation(projects.libraries.testutils)

    // AndroidX Libraries
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.paging.runtime.ktx)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Material Design
    implementation(libs.androidx.material)

    // Lifecycle Components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Logging
    implementation(libs.timber)

    // Security
    implementation(libs.androidx.security.crypto)

    // Activity and Fragment KTX
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Sheets Libraries
    implementation(libs.sheets.core)
    implementation(libs.sheets.duration)

    // Testing Libraries
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.espresso.core)
}
