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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.scrolless.app.accessibility.ScrollessBlockAccessibilityService
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.feature.home.HomeScreen
import com.scrolless.app.feature.settings.SettingsScreen
import com.scrolless.app.util.requestAppReview
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val appState: ScrollessAppState = rememberScrollessAppState()

            ScrollessTheme {
                SharedTransitionLayout {
                    NavDisplay(
                        appState.backStack,
                        onBack = { appState.navigateBack() },
                        entryProvider = entryProvider {
                            entry<ScrollessRoute.Home> {
                                val animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                HomeScreen(
                                    onNavigateToSettings = appState::navigateToSettings,
                                    accessibilityServiceClass = ScrollessBlockAccessibilityService::class.java,
                                    onRequestAppReview = ::requestAppReview,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                            }
                            entry<ScrollessRoute.Settings> {
                                val animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                SettingsScreen(
                                    onNavigateBack = appState::navigateBack,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
