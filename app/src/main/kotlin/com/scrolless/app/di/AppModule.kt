/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.di

import com.scrolless.app.app.ScrollessApp
import com.scrolless.framework.core.base.application.AppInitializer
import com.scrolless.framework.core.base.application.AppInitializerImpl
import com.scrolless.framework.core.base.application.CoreConfig
import com.scrolless.framework.core.base.application.TimberInitializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideApplication(): ScrollessApp = ScrollessApp()

    @Provides
    @Singleton
    fun provideAppConfig(app: ScrollessApp): CoreConfig = app.appConfig()

    @Provides
    @Singleton
    fun provideTimberInitializer() = TimberInitializer()

    @Provides
    @Singleton
    fun provideAppInitializer(timberManager: TimberInitializer): AppInitializer =
        AppInitializerImpl(timberManager)
}
