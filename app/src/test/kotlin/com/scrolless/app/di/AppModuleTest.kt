/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.di

import com.google.common.truth.Truth
import com.scrolless.app.app.ScrollessApp
import com.scrolless.framework.core.base.application.AppInitializerImpl
import com.scrolless.framework.core.base.application.TimberInitializer
import com.scrolless.testutils.MockkUnitTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class AppModuleTest : MockkUnitTest() {

    private lateinit var module: AppModule

    override fun onCreate() {
        super.onCreate()
        module = AppModule()
    }

    @Test
    fun verifyProvideApplication() {
        val mockScrollessApp = mockk<ScrollessApp>(relaxed = true)
        val scrollessApp = module.provideApplication()
        every { mockScrollessApp.toString() } returns scrollessApp.toString()

        Truth.assertThat(mockScrollessApp.toString())
            .isEqualTo(scrollessApp.toString())
    }

    @Test
    fun verifyProvideAppConfig() {
        val scrollessApp = mockk<ScrollessApp>(relaxed = true)
        val appConfig = module.provideAppConfig(scrollessApp)

        Assert.assertEquals(scrollessApp.appConfig(), appConfig)
    }

    @Test
    fun verifyProvideTimberInitializer() {
        val timberInitializer = module.provideTimberInitializer()
        Truth.assertThat(timberInitializer).isInstanceOf(TimberInitializer::class.java)
    }

    @Test
    fun verifyProvideAppInitializer() {
        val mockTimberInitializer = mockk<TimberInitializer>(relaxed = true)
        val appInitializer = module.provideAppInitializer(mockTimberInitializer)

        Truth.assertThat(appInitializer).isInstanceOf(AppInitializerImpl::class.java)
    }
}
