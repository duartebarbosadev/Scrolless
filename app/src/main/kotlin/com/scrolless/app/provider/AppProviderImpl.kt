/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.provider

import android.content.Context
import com.scrolless.app.features.home.BlockConfig
import com.scrolless.app.features.home.BlockOption
import com.scrolless.framework.core.pref.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class AppProviderImpl(context: Context) : AppProvider {

    companion object {
        private const val PREF_PACKAGE_NAME = "com.scrolless.app.provider"
        private const val PREF_KEY_THEME_COLOR = "theme_color"
        private const val PREF_BLOCK_OPTION = "block_option"
        private const val PREF_TIME_LIMIT = "time_limit"
        private const val PREF_INTERVAL_LENGTH = "interval_length"
        private const val PREF_LAST_RESET_DAY = "last_reset_day"
        private const val PREF_TOTAL_DAILY_USAGE = "total_daily_usage"

        // Timer overlay
        private const val PREF_TIMER_OVERLAY_ENABLED = "timer_overlay_enabled"
        private const val PREF_TIMER_POSITION_X = "timer_overlay_position_x"
        private const val PREF_TIMER_POSITION_Y = "timer_overlay_position_y"
    }

    override val cacheManager = CacheManager(context, PREF_PACKAGE_NAME)

    private val _blockConfigFlow = MutableStateFlow(readBlockConfigFromCache())
    override val blockConfigFlow: StateFlow<BlockConfig> = _blockConfigFlow

    override var blockConfig: BlockConfig
        get() = _blockConfigFlow.value
        set(value) {
            if (value != _blockConfigFlow.value) {
                writeBlockConfigToCache(value)
                _blockConfigFlow.value = value
            }
        }

    override var themeMode: ThemeProvider.Theme
        get() {
            val themeName = cacheManager.read(PREF_KEY_THEME_COLOR, ThemeProvider.Theme.SYSTEM.name)
            return try {
                ThemeProvider.Theme.valueOf(themeName.uppercase())
            } catch (e: IllegalArgumentException) {

                Timber.e(e, "Invalid theme name: $themeName")
                ThemeProvider.Theme.SYSTEM
            }
        }
        set(theme) {
            cacheManager.write(PREF_KEY_THEME_COLOR, theme.name)
        }

    override var lastResetDay: String
        get() = cacheManager.read(PREF_LAST_RESET_DAY, "")
        set(value) = cacheManager.write(PREF_LAST_RESET_DAY, value)

    override var totalDailyUsage: Long
        get() = cacheManager.read(PREF_TOTAL_DAILY_USAGE, 0)
        set(value) = cacheManager.write(PREF_TOTAL_DAILY_USAGE, value)

    override var timerOverlayEnabled: Boolean
        get() = cacheManager.read(PREF_TIMER_OVERLAY_ENABLED, false)
        set(value) = cacheManager.write(PREF_TIMER_OVERLAY_ENABLED, value)

    override var timerOverlayPositionX: Int
        get() = cacheManager.read(PREF_TIMER_POSITION_X, 16)
        set(value) = cacheManager.write(PREF_TIMER_POSITION_X, value)

    override var timerOverlayPositionY: Int
        get() = cacheManager.read(PREF_TIMER_POSITION_Y, 16)
        set(value) = cacheManager.write(PREF_TIMER_POSITION_Y, value)

    private fun readBlockConfigFromCache(): BlockConfig {
        val blockOption = cacheManager.read(PREF_BLOCK_OPTION, BlockOption.NothingSelected)
        val timeLimit = cacheManager.read(PREF_TIME_LIMIT, 20000L)
        val intervalLength = cacheManager.read(PREF_INTERVAL_LENGTH, 1000L)
        return BlockConfig(blockOption, timeLimit, intervalLength)
    }

    private fun writeBlockConfigToCache(config: BlockConfig) {
        cacheManager.write(PREF_BLOCK_OPTION, config.blockOption)
        cacheManager.write(PREF_TIME_LIMIT, config.timeLimit)
        cacheManager.write(PREF_INTERVAL_LENGTH, config.intervalLength)
    }
}
