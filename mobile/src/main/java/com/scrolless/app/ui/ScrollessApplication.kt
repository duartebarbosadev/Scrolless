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
@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.scrolless.app.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.scrolless.app.accessibility.ScrollessBlockAccessibilityService
import com.scrolless.app.feature.home.HomeScreen
import com.scrolless.app.feature.settings.SettingsScreen
import com.scrolless.app.util.requestAppReview

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun ScrollessApplication(appState: ScrollessAppState = rememberScrollessAppState()) {
    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this,
        ) {
            NavDisplay(
                appState.backStack,
                onBack = { appState.navigateBack() },
                entryProvider = entryProvider<NavKey> {
                    entry<ScrollessRoute.Home> {
                        HomeScreen(
                            onNavigateToSettings = appState::navigateToSettings,
                            accessibilityServiceClass = ScrollessBlockAccessibilityService::class.java,
                            onRequestAppReview = ::requestAppReview,
                        )
                    }
                    entry<ScrollessRoute.Settings> {
                        SettingsScreen(onNavigateBack = appState::navigateBack)
                    }
                }
            )
        }
    }
}

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
