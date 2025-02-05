/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.di

import android.content.Context
import com.scrolless.app.overlay.TimerOverlayManager
import com.scrolless.app.overlay.TimerOverlayManagerImpl
import com.scrolless.app.provider.AppProvider
import com.scrolless.app.provider.AppProviderImpl
import com.scrolless.app.provider.NavigationProvider
import com.scrolless.app.provider.NavigationProviderImpl
import com.scrolless.app.provider.ResourceProvider
import com.scrolless.app.provider.ResourceProviderImpl
import com.scrolless.app.provider.ThemeProvider
import com.scrolless.app.provider.ThemeProviderImpl
import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.provider.UsageTrackerImpl
import com.scrolless.app.services.BlockController
import com.scrolless.app.services.BlockControllerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ProviderModule {
    @Provides
    @Singleton
    fun provideNavigationProviderImpl(
        @ApplicationContext context: Context
    ): NavigationProvider = NavigationProviderImpl(context)

    @Provides
    @Singleton
    fun provideResourceProviderImpl(
        @ApplicationContext context: Context
    ): ResourceProvider = ResourceProviderImpl(context)

    @Provides
    @Singleton
    fun provideThemeProviderImpl(): ThemeProvider = ThemeProviderImpl()

    @Provides
    @Singleton
    fun provideAppProviderImpl(
        @ApplicationContext context: Context
    ): AppProvider = AppProviderImpl(context)

    @Provides
    @Singleton
    fun provideUsageTrackerImpl(
        appProvider: AppProvider
    ): UsageTracker = UsageTrackerImpl(appProvider)

    @Provides
    @Singleton
    fun provideBlockController(
        usageTracker: UsageTracker
    ): BlockController = BlockControllerImpl(usageTracker)

    @Provides
    @Singleton
    fun provideTimerOverlayManager(
        usageTracker: UsageTracker,
        appProvider: AppProvider
    ): TimerOverlayManager = TimerOverlayManagerImpl(usageTracker, appProvider)
}
