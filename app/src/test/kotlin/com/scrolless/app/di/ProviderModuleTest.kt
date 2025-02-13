/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.di

import android.content.Context
import com.google.common.truth.Truth
import com.scrolless.app.provider.NavigationProviderImpl
import com.scrolless.app.provider.ThemeProviderImpl
import com.scrolless.testutils.MockkUnitTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class ProviderModuleTest : MockkUnitTest() {

    private lateinit var module: ProviderModule

    override fun onCreate() {
        super.onCreate()
        module = ProviderModule()
    }

    @Test
    fun verifyProvideNavigationProviderImpl() {
        val context = mockk<Context>(relaxed = true)
        val navigationProvider = module.provideNavigationProviderImpl(context)

        Truth.assertThat(navigationProvider).isInstanceOf(NavigationProviderImpl::class.java)
    }

    @Test
    fun verifyProvideResourceProviderImpl() {
        val string = ""
        val context = mockk<Context>(relaxed = true)
        val resourceManager = module.provideResourceProviderImpl(context)

        every { resourceManager.getString(any()) } returns string

        Assert.assertEquals(string, resourceManager.getString(0))
    }

    @Test
    fun verifyProvideThemeProviderImpl() {
        val themeProvider = module.provideThemeProviderImpl()
        Truth.assertThat(themeProvider).isInstanceOf(ThemeProviderImpl::class.java)
    }
}
