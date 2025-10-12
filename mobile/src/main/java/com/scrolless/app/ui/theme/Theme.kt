// spotless:off
/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2025 Scrolless
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
// spotless:on
package com.scrolless.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.scrolless.app.designsystem.theme.ScrollessShapes
import com.scrolless.app.designsystem.theme.ScrollessTypography
import com.scrolless.app.designsystem.theme.backgroundDark
import com.scrolless.app.designsystem.theme.errorContainerDark
import com.scrolless.app.designsystem.theme.errorDark
import com.scrolless.app.designsystem.theme.inverseOnSurfaceDark
import com.scrolless.app.designsystem.theme.inversePrimaryDark
import com.scrolless.app.designsystem.theme.inverseSurfaceDark
import com.scrolless.app.designsystem.theme.onBackgroundDark
import com.scrolless.app.designsystem.theme.onErrorContainerDark
import com.scrolless.app.designsystem.theme.onErrorDark
import com.scrolless.app.designsystem.theme.onPrimaryContainerDark
import com.scrolless.app.designsystem.theme.onPrimaryDark
import com.scrolless.app.designsystem.theme.onSecondaryContainerDark
import com.scrolless.app.designsystem.theme.onSecondaryDark
import com.scrolless.app.designsystem.theme.onSurfaceDark
import com.scrolless.app.designsystem.theme.onSurfaceVariantDark
import com.scrolless.app.designsystem.theme.onTertiaryContainerDark
import com.scrolless.app.designsystem.theme.onTertiaryDark
import com.scrolless.app.designsystem.theme.outlineDark
import com.scrolless.app.designsystem.theme.outlineVariantDark
import com.scrolless.app.designsystem.theme.primaryContainerDark
import com.scrolless.app.designsystem.theme.primaryDark
import com.scrolless.app.designsystem.theme.scrimDark
import com.scrolless.app.designsystem.theme.secondaryContainerDark
import com.scrolless.app.designsystem.theme.secondaryDark
import com.scrolless.app.designsystem.theme.surfaceBrightDark
import com.scrolless.app.designsystem.theme.surfaceContainerDark
import com.scrolless.app.designsystem.theme.surfaceContainerHighDark
import com.scrolless.app.designsystem.theme.surfaceContainerHighestDark
import com.scrolless.app.designsystem.theme.surfaceContainerLowDark
import com.scrolless.app.designsystem.theme.surfaceContainerLowestDark
import com.scrolless.app.designsystem.theme.surfaceDark
import com.scrolless.app.designsystem.theme.surfaceDimDark
import com.scrolless.app.designsystem.theme.surfaceVariantDark
import com.scrolless.app.designsystem.theme.tertiaryContainerDark
import com.scrolless.app.designsystem.theme.tertiaryDark

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScrollessTheme(darkTheme : Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> darkScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ScrollessShapes,
        typography = ScrollessTypography,
        content = content,
    )
}
