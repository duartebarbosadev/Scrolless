/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.provider

import com.scrolless.app.features.home.BlockConfig
import com.scrolless.framework.core.pref.CacheManager
import kotlinx.coroutines.flow.StateFlow

interface AppProvider {
    var themeMode: ThemeProvider.Theme
    val cacheManager: CacheManager

    var lastResetDay: String
    var totalDailyUsage: Long

    /**
     * Observes changes in blockConfig
     */
    var blockConfig: BlockConfig
    val blockConfigFlow: StateFlow<BlockConfig>
}
