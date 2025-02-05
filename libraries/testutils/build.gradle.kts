/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
plugins {
    alias(libs.plugins.scrolless.android.library)
}

android {
    namespace = "com.scrolless.framework.testutils"
}

dependencies {
    // Main Dependencies
    implementation(libs.androidx.appcompat)
    implementation(libs.test.okhttp.mockwebserver)

    // Testing Dependencies
    api(libs.test.kotlinx.coroutines)
    api(libs.test.robolectric)
    api(libs.test.mockk)
    api(libs.test.assertj)
    api(libs.test.core.testing)
    api(libs.test.runner)
    api(libs.test.rules)
    api(libs.test.hamcrest)
    api(libs.test.turbine)
    api(libs.test.fragment.test)
    api(libs.test.truth)
    api(libs.test.jupiter.api)
    api(libs.test.core.testing)
}
