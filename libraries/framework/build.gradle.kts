/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
plugins {
    alias(libs.plugins.scrolless.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.scrolless.android.room)
}

android {
    namespace = "com.scrolless.libraries.framework"
}

dependencies {
    implementation(libs.androidx.preference)
    testImplementation(projects.libraries.testutils)

    implementation(libs.firebase.crashlytics.ktx)

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

    // Moshi for JSON Parsing
    implementation(libs.moshi.kotlin)

    // Logging
    implementation(libs.timber)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Security
    implementation(libs.androidx.security.crypto)

    // Activity and Fragment KTX
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Sheets Libraries
    implementation(libs.sheets.core)
    implementation(libs.sheets.duration)
    implementation(libs.sheets.clock)
    implementation(libs.sheets.option)
    implementation(libs.sheets.info)
    implementation(libs.sheets.input)

    // Testing Libraries
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.espresso.core)
}
