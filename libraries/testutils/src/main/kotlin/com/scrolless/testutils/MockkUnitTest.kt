/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.testutils

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule

@ExperimentalCoroutinesApi
open class MockkUnitTest {

    open fun onCreate() {}

    open fun onDestroy() {}

    @get:Rule
    var mainCoroutinesRule = MainCoroutinesRule()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        onCreate()
    }

    @After
    fun tearDown() {
        unmockkAll()
        clearAllMocks()
    }
}
