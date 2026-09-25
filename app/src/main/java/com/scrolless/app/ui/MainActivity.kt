/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.ui

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.scrolless.app.accessibility.ScrollessBlockAccessibilityService
import com.scrolless.app.debug.DebugOverlayConfig
import com.scrolless.app.designsystem.theme.LocalSharedTransitionScope
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.feature.home.HomeScreen
import com.scrolless.app.feature.settings.SettingsScreen
import com.scrolless.app.util.requestAppReview
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hold the splash until Home has its persisted state, so the first visible frame
        // is the real content instead of defaults that immediately jump (seen as flicker).
        var isContentReady = false
        val splashStartedAt = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            !isContentReady && SystemClock.uptimeMillis() - splashStartedAt < MAX_SPLASH_HOLD_MILLIS
        }

        setContent {

            val appState: ScrollessAppState = rememberScrollessAppState()
            val forceLegacyOverlay by DebugOverlayConfig.forceLegacyOverlay.collectAsStateWithLifecycle()

            ScrollessTheme {
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavDisplay(
                            appState.backStack,
                            onBack = { appState.navigateBack() },
                            entryProvider = entryProvider {
                                entry<ScrollessRoute.Home> {
                                    HomeScreen(
                                        onNavigateToSettings = appState::navigateToSettings,
                                        accessibilityServiceClass = ScrollessBlockAccessibilityService::class.java,
                                        onRequestAppReview = ::requestAppReview,
                                        onContentReady = { isContentReady = true },
                                        forceLegacyOverlay = forceLegacyOverlay,
                                        onForceLegacyOverlayChanged = {
                                            DebugOverlayConfig.forceLegacyOverlay.value = it
                                        },
                                    )
                                }
                                entry<ScrollessRoute.Settings> {
                                    SettingsScreen(
                                        onNavigateBack = appState::navigateBack,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_SPLASH_HOLD_MILLIS = 1_500L
    }
}
