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
package com.scrolless.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.scrolless.app.designsystem.theme.ScrollessShapes
import com.scrolless.app.designsystem.theme.ScrollessTypography
import com.scrolless.app.designsystem.theme.backgroundDark
import com.scrolless.app.designsystem.theme.backgroundDarkHighContrast
import com.scrolless.app.designsystem.theme.backgroundDarkMediumContrast
import com.scrolless.app.designsystem.theme.backgroundLight
import com.scrolless.app.designsystem.theme.backgroundLightHighContrast
import com.scrolless.app.designsystem.theme.backgroundLightMediumContrast
import com.scrolless.app.designsystem.theme.errorContainerDark
import com.scrolless.app.designsystem.theme.errorContainerDarkHighContrast
import com.scrolless.app.designsystem.theme.errorContainerDarkMediumContrast
import com.scrolless.app.designsystem.theme.errorContainerLight
import com.scrolless.app.designsystem.theme.errorContainerLightHighContrast
import com.scrolless.app.designsystem.theme.errorContainerLightMediumContrast
import com.scrolless.app.designsystem.theme.errorDark
import com.scrolless.app.designsystem.theme.errorDarkHighContrast
import com.scrolless.app.designsystem.theme.errorDarkMediumContrast
import com.scrolless.app.designsystem.theme.errorLight
import com.scrolless.app.designsystem.theme.errorLightHighContrast
import com.scrolless.app.designsystem.theme.errorLightMediumContrast
import com.scrolless.app.designsystem.theme.inverseOnSurfaceDark
import com.scrolless.app.designsystem.theme.inverseOnSurfaceDarkHighContrast
import com.scrolless.app.designsystem.theme.inverseOnSurfaceDarkMediumContrast
import com.scrolless.app.designsystem.theme.inverseOnSurfaceLight
import com.scrolless.app.designsystem.theme.inverseOnSurfaceLightHighContrast
import com.scrolless.app.designsystem.theme.inverseOnSurfaceLightMediumContrast
import com.scrolless.app.designsystem.theme.inversePrimaryDark
import com.scrolless.app.designsystem.theme.inversePrimaryDarkHighContrast
import com.scrolless.app.designsystem.theme.inversePrimaryDarkMediumContrast
import com.scrolless.app.designsystem.theme.inversePrimaryLight
import com.scrolless.app.designsystem.theme.inversePrimaryLightHighContrast
import com.scrolless.app.designsystem.theme.inversePrimaryLightMediumContrast
import com.scrolless.app.designsystem.theme.inverseSurfaceDark
import com.scrolless.app.designsystem.theme.inverseSurfaceDarkHighContrast
import com.scrolless.app.designsystem.theme.inverseSurfaceDarkMediumContrast
import com.scrolless.app.designsystem.theme.inverseSurfaceLight
import com.scrolless.app.designsystem.theme.inverseSurfaceLightHighContrast
import com.scrolless.app.designsystem.theme.inverseSurfaceLightMediumContrast
import com.scrolless.app.designsystem.theme.onBackgroundDark
import com.scrolless.app.designsystem.theme.onBackgroundDarkHighContrast
import com.scrolless.app.designsystem.theme.onBackgroundDarkMediumContrast
import com.scrolless.app.designsystem.theme.onBackgroundLight
import com.scrolless.app.designsystem.theme.onBackgroundLightHighContrast
import com.scrolless.app.designsystem.theme.onBackgroundLightMediumContrast
import com.scrolless.app.designsystem.theme.onErrorContainerDark
import com.scrolless.app.designsystem.theme.onErrorContainerDarkHighContrast
import com.scrolless.app.designsystem.theme.onErrorContainerDarkMediumContrast
import com.scrolless.app.designsystem.theme.onErrorContainerLight
import com.scrolless.app.designsystem.theme.onErrorContainerLightHighContrast
import com.scrolless.app.designsystem.theme.onErrorContainerLightMediumContrast
import com.scrolless.app.designsystem.theme.onErrorDark
import com.scrolless.app.designsystem.theme.onErrorDarkHighContrast
import com.scrolless.app.designsystem.theme.onErrorDarkMediumContrast
import com.scrolless.app.designsystem.theme.onErrorLight
import com.scrolless.app.designsystem.theme.onErrorLightHighContrast
import com.scrolless.app.designsystem.theme.onErrorLightMediumContrast
import com.scrolless.app.designsystem.theme.onPrimaryContainerDark
import com.scrolless.app.designsystem.theme.onPrimaryContainerDarkHighContrast
import com.scrolless.app.designsystem.theme.onPrimaryContainerDarkMediumContrast
import com.scrolless.app.designsystem.theme.onPrimaryContainerLight
import com.scrolless.app.designsystem.theme.onPrimaryContainerLightHighContrast
import com.scrolless.app.designsystem.theme.onPrimaryContainerLightMediumContrast
import com.scrolless.app.designsystem.theme.onPrimaryDark
import com.scrolless.app.designsystem.theme.onPrimaryDarkHighContrast
import com.scrolless.app.designsystem.theme.onPrimaryDarkMediumContrast
import com.scrolless.app.designsystem.theme.onPrimaryLight
import com.scrolless.app.designsystem.theme.onPrimaryLightHighContrast
import com.scrolless.app.designsystem.theme.onPrimaryLightMediumContrast
import com.scrolless.app.designsystem.theme.onSecondaryContainerDark
import com.scrolless.app.designsystem.theme.onSecondaryContainerDarkHighContrast
import com.scrolless.app.designsystem.theme.onSecondaryContainerDarkMediumContrast
import com.scrolless.app.designsystem.theme.onSecondaryContainerLight
import com.scrolless.app.designsystem.theme.onSecondaryContainerLightHighContrast
import com.scrolless.app.designsystem.theme.onSecondaryContainerLightMediumContrast
import com.scrolless.app.designsystem.theme.onSecondaryDark
import com.scrolless.app.designsystem.theme.onSecondaryDarkHighContrast
import com.scrolless.app.designsystem.theme.onSecondaryDarkMediumContrast
import com.scrolless.app.designsystem.theme.onSecondaryLight
import com.scrolless.app.designsystem.theme.onSecondaryLightHighContrast
import com.scrolless.app.designsystem.theme.onSecondaryLightMediumContrast
import com.scrolless.app.designsystem.theme.onSurfaceDark
import com.scrolless.app.designsystem.theme.onSurfaceDarkHighContrast
import com.scrolless.app.designsystem.theme.onSurfaceDarkMediumContrast
import com.scrolless.app.designsystem.theme.onSurfaceLight
import com.scrolless.app.designsystem.theme.onSurfaceLightHighContrast
import com.scrolless.app.designsystem.theme.onSurfaceLightMediumContrast
import com.scrolless.app.designsystem.theme.onSurfaceVariantDark
import com.scrolless.app.designsystem.theme.onSurfaceVariantDarkHighContrast
import com.scrolless.app.designsystem.theme.onSurfaceVariantDarkMediumContrast
import com.scrolless.app.designsystem.theme.onSurfaceVariantLight
import com.scrolless.app.designsystem.theme.onSurfaceVariantLightHighContrast
import com.scrolless.app.designsystem.theme.onSurfaceVariantLightMediumContrast
import com.scrolless.app.designsystem.theme.onTertiaryContainerDark
import com.scrolless.app.designsystem.theme.onTertiaryContainerDarkHighContrast
import com.scrolless.app.designsystem.theme.onTertiaryContainerDarkMediumContrast
import com.scrolless.app.designsystem.theme.onTertiaryContainerLight
import com.scrolless.app.designsystem.theme.onTertiaryContainerLightHighContrast
import com.scrolless.app.designsystem.theme.onTertiaryContainerLightMediumContrast
import com.scrolless.app.designsystem.theme.onTertiaryDark
import com.scrolless.app.designsystem.theme.onTertiaryDarkHighContrast
import com.scrolless.app.designsystem.theme.onTertiaryDarkMediumContrast
import com.scrolless.app.designsystem.theme.onTertiaryLight
import com.scrolless.app.designsystem.theme.onTertiaryLightHighContrast
import com.scrolless.app.designsystem.theme.onTertiaryLightMediumContrast
import com.scrolless.app.designsystem.theme.outlineDark
import com.scrolless.app.designsystem.theme.outlineDarkHighContrast
import com.scrolless.app.designsystem.theme.outlineDarkMediumContrast
import com.scrolless.app.designsystem.theme.outlineLight
import com.scrolless.app.designsystem.theme.outlineLightHighContrast
import com.scrolless.app.designsystem.theme.outlineLightMediumContrast
import com.scrolless.app.designsystem.theme.outlineVariantDark
import com.scrolless.app.designsystem.theme.outlineVariantDarkHighContrast
import com.scrolless.app.designsystem.theme.outlineVariantDarkMediumContrast
import com.scrolless.app.designsystem.theme.outlineVariantLight
import com.scrolless.app.designsystem.theme.outlineVariantLightHighContrast
import com.scrolless.app.designsystem.theme.outlineVariantLightMediumContrast
import com.scrolless.app.designsystem.theme.primaryContainerDark
import com.scrolless.app.designsystem.theme.primaryContainerDarkHighContrast
import com.scrolless.app.designsystem.theme.primaryContainerDarkMediumContrast
import com.scrolless.app.designsystem.theme.primaryContainerLight
import com.scrolless.app.designsystem.theme.primaryContainerLightHighContrast
import com.scrolless.app.designsystem.theme.primaryContainerLightMediumContrast
import com.scrolless.app.designsystem.theme.primaryDark
import com.scrolless.app.designsystem.theme.primaryDarkHighContrast
import com.scrolless.app.designsystem.theme.primaryDarkMediumContrast
import com.scrolless.app.designsystem.theme.primaryLight
import com.scrolless.app.designsystem.theme.primaryLightHighContrast
import com.scrolless.app.designsystem.theme.primaryLightMediumContrast
import com.scrolless.app.designsystem.theme.scrimDark
import com.scrolless.app.designsystem.theme.scrimDarkHighContrast
import com.scrolless.app.designsystem.theme.scrimDarkMediumContrast
import com.scrolless.app.designsystem.theme.scrimLight
import com.scrolless.app.designsystem.theme.scrimLightHighContrast
import com.scrolless.app.designsystem.theme.scrimLightMediumContrast
import com.scrolless.app.designsystem.theme.secondaryContainerDark
import com.scrolless.app.designsystem.theme.secondaryContainerDarkHighContrast
import com.scrolless.app.designsystem.theme.secondaryContainerDarkMediumContrast
import com.scrolless.app.designsystem.theme.secondaryContainerLight
import com.scrolless.app.designsystem.theme.secondaryContainerLightHighContrast
import com.scrolless.app.designsystem.theme.secondaryContainerLightMediumContrast
import com.scrolless.app.designsystem.theme.secondaryDark
import com.scrolless.app.designsystem.theme.secondaryDarkHighContrast
import com.scrolless.app.designsystem.theme.secondaryDarkMediumContrast
import com.scrolless.app.designsystem.theme.secondaryLight
import com.scrolless.app.designsystem.theme.secondaryLightHighContrast
import com.scrolless.app.designsystem.theme.secondaryLightMediumContrast
import com.scrolless.app.designsystem.theme.surfaceBrightDark
import com.scrolless.app.designsystem.theme.surfaceBrightDarkHighContrast
import com.scrolless.app.designsystem.theme.surfaceBrightDarkMediumContrast
import com.scrolless.app.designsystem.theme.surfaceBrightLight
import com.scrolless.app.designsystem.theme.surfaceBrightLightHighContrast
import com.scrolless.app.designsystem.theme.surfaceBrightLightMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerDark
import com.scrolless.app.designsystem.theme.surfaceContainerDarkHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerDarkMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerHighDark
import com.scrolless.app.designsystem.theme.surfaceContainerHighDarkHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerHighDarkMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerHighLight
import com.scrolless.app.designsystem.theme.surfaceContainerHighLightHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerHighLightMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerHighestDark
import com.scrolless.app.designsystem.theme.surfaceContainerHighestDarkHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerHighestDarkMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerHighestLight
import com.scrolless.app.designsystem.theme.surfaceContainerHighestLightHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerHighestLightMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLight
import com.scrolless.app.designsystem.theme.surfaceContainerLightHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLightMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLowDark
import com.scrolless.app.designsystem.theme.surfaceContainerLowDarkHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLowDarkMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLowLight
import com.scrolless.app.designsystem.theme.surfaceContainerLowLightHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLowLightMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLowestDark
import com.scrolless.app.designsystem.theme.surfaceContainerLowestDarkHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLowestDarkMediumContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLowestLight
import com.scrolless.app.designsystem.theme.surfaceContainerLowestLightHighContrast
import com.scrolless.app.designsystem.theme.surfaceContainerLowestLightMediumContrast
import com.scrolless.app.designsystem.theme.surfaceDark
import com.scrolless.app.designsystem.theme.surfaceDarkHighContrast
import com.scrolless.app.designsystem.theme.surfaceDarkMediumContrast
import com.scrolless.app.designsystem.theme.surfaceDimDark
import com.scrolless.app.designsystem.theme.surfaceDimDarkHighContrast
import com.scrolless.app.designsystem.theme.surfaceDimDarkMediumContrast
import com.scrolless.app.designsystem.theme.surfaceDimLight
import com.scrolless.app.designsystem.theme.surfaceDimLightHighContrast
import com.scrolless.app.designsystem.theme.surfaceDimLightMediumContrast
import com.scrolless.app.designsystem.theme.surfaceLight
import com.scrolless.app.designsystem.theme.surfaceLightHighContrast
import com.scrolless.app.designsystem.theme.surfaceLightMediumContrast
import com.scrolless.app.designsystem.theme.surfaceVariantDark
import com.scrolless.app.designsystem.theme.surfaceVariantDarkHighContrast
import com.scrolless.app.designsystem.theme.surfaceVariantDarkMediumContrast
import com.scrolless.app.designsystem.theme.surfaceVariantLight
import com.scrolless.app.designsystem.theme.surfaceVariantLightHighContrast
import com.scrolless.app.designsystem.theme.surfaceVariantLightMediumContrast
import com.scrolless.app.designsystem.theme.tertiaryContainerDark
import com.scrolless.app.designsystem.theme.tertiaryContainerDarkHighContrast
import com.scrolless.app.designsystem.theme.tertiaryContainerDarkMediumContrast
import com.scrolless.app.designsystem.theme.tertiaryContainerLight
import com.scrolless.app.designsystem.theme.tertiaryContainerLightHighContrast
import com.scrolless.app.designsystem.theme.tertiaryContainerLightMediumContrast
import com.scrolless.app.designsystem.theme.tertiaryDark
import com.scrolless.app.designsystem.theme.tertiaryDarkHighContrast
import com.scrolless.app.designsystem.theme.tertiaryDarkMediumContrast
import com.scrolless.app.designsystem.theme.tertiaryLight
import com.scrolless.app.designsystem.theme.tertiaryLightHighContrast
import com.scrolless.app.designsystem.theme.tertiaryLightMediumContrast

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

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

private val mediumContrastLightColorScheme = lightColorScheme(
    primary = primaryLightMediumContrast,
    onPrimary = onPrimaryLightMediumContrast,
    primaryContainer = primaryContainerLightMediumContrast,
    onPrimaryContainer = onPrimaryContainerLightMediumContrast,
    secondary = secondaryLightMediumContrast,
    onSecondary = onSecondaryLightMediumContrast,
    secondaryContainer = secondaryContainerLightMediumContrast,
    onSecondaryContainer = onSecondaryContainerLightMediumContrast,
    tertiary = tertiaryLightMediumContrast,
    onTertiary = onTertiaryLightMediumContrast,
    tertiaryContainer = tertiaryContainerLightMediumContrast,
    onTertiaryContainer = onTertiaryContainerLightMediumContrast,
    error = errorLightMediumContrast,
    onError = onErrorLightMediumContrast,
    errorContainer = errorContainerLightMediumContrast,
    onErrorContainer = onErrorContainerLightMediumContrast,
    background = backgroundLightMediumContrast,
    onBackground = onBackgroundLightMediumContrast,
    surface = surfaceLightMediumContrast,
    onSurface = onSurfaceLightMediumContrast,
    surfaceVariant = surfaceVariantLightMediumContrast,
    onSurfaceVariant = onSurfaceVariantLightMediumContrast,
    outline = outlineLightMediumContrast,
    outlineVariant = outlineVariantLightMediumContrast,
    scrim = scrimLightMediumContrast,
    inverseSurface = inverseSurfaceLightMediumContrast,
    inverseOnSurface = inverseOnSurfaceLightMediumContrast,
    inversePrimary = inversePrimaryLightMediumContrast,
    surfaceDim = surfaceDimLightMediumContrast,
    surfaceBright = surfaceBrightLightMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestLightMediumContrast,
    surfaceContainerLow = surfaceContainerLowLightMediumContrast,
    surfaceContainer = surfaceContainerLightMediumContrast,
    surfaceContainerHigh = surfaceContainerHighLightMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestLightMediumContrast,
)

private val highContrastLightColorScheme = lightColorScheme(
    primary = primaryLightHighContrast,
    onPrimary = onPrimaryLightHighContrast,
    primaryContainer = primaryContainerLightHighContrast,
    onPrimaryContainer = onPrimaryContainerLightHighContrast,
    secondary = secondaryLightHighContrast,
    onSecondary = onSecondaryLightHighContrast,
    secondaryContainer = secondaryContainerLightHighContrast,
    onSecondaryContainer = onSecondaryContainerLightHighContrast,
    tertiary = tertiaryLightHighContrast,
    onTertiary = onTertiaryLightHighContrast,
    tertiaryContainer = tertiaryContainerLightHighContrast,
    onTertiaryContainer = onTertiaryContainerLightHighContrast,
    error = errorLightHighContrast,
    onError = onErrorLightHighContrast,
    errorContainer = errorContainerLightHighContrast,
    onErrorContainer = onErrorContainerLightHighContrast,
    background = backgroundLightHighContrast,
    onBackground = onBackgroundLightHighContrast,
    surface = surfaceLightHighContrast,
    onSurface = onSurfaceLightHighContrast,
    surfaceVariant = surfaceVariantLightHighContrast,
    onSurfaceVariant = onSurfaceVariantLightHighContrast,
    outline = outlineLightHighContrast,
    outlineVariant = outlineVariantLightHighContrast,
    scrim = scrimLightHighContrast,
    inverseSurface = inverseSurfaceLightHighContrast,
    inverseOnSurface = inverseOnSurfaceLightHighContrast,
    inversePrimary = inversePrimaryLightHighContrast,
    surfaceDim = surfaceDimLightHighContrast,
    surfaceBright = surfaceBrightLightHighContrast,
    surfaceContainerLowest = surfaceContainerLowestLightHighContrast,
    surfaceContainerLow = surfaceContainerLowLightHighContrast,
    surfaceContainer = surfaceContainerLightHighContrast,
    surfaceContainerHigh = surfaceContainerHighLightHighContrast,
    surfaceContainerHighest = surfaceContainerHighestLightHighContrast,
)

private val mediumContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkMediumContrast,
    onPrimary = onPrimaryDarkMediumContrast,
    primaryContainer = primaryContainerDarkMediumContrast,
    onPrimaryContainer = onPrimaryContainerDarkMediumContrast,
    secondary = secondaryDarkMediumContrast,
    onSecondary = onSecondaryDarkMediumContrast,
    secondaryContainer = secondaryContainerDarkMediumContrast,
    onSecondaryContainer = onSecondaryContainerDarkMediumContrast,
    tertiary = tertiaryDarkMediumContrast,
    onTertiary = onTertiaryDarkMediumContrast,
    tertiaryContainer = tertiaryContainerDarkMediumContrast,
    onTertiaryContainer = onTertiaryContainerDarkMediumContrast,
    error = errorDarkMediumContrast,
    onError = onErrorDarkMediumContrast,
    errorContainer = errorContainerDarkMediumContrast,
    onErrorContainer = onErrorContainerDarkMediumContrast,
    background = backgroundDarkMediumContrast,
    onBackground = onBackgroundDarkMediumContrast,
    surface = surfaceDarkMediumContrast,
    onSurface = onSurfaceDarkMediumContrast,
    surfaceVariant = surfaceVariantDarkMediumContrast,
    onSurfaceVariant = onSurfaceVariantDarkMediumContrast,
    outline = outlineDarkMediumContrast,
    outlineVariant = outlineVariantDarkMediumContrast,
    scrim = scrimDarkMediumContrast,
    inverseSurface = inverseSurfaceDarkMediumContrast,
    inverseOnSurface = inverseOnSurfaceDarkMediumContrast,
    inversePrimary = inversePrimaryDarkMediumContrast,
    surfaceDim = surfaceDimDarkMediumContrast,
    surfaceBright = surfaceBrightDarkMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkMediumContrast,
    surfaceContainerLow = surfaceContainerLowDarkMediumContrast,
    surfaceContainer = surfaceContainerDarkMediumContrast,
    surfaceContainerHigh = surfaceContainerHighDarkMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkMediumContrast,
)

private val highContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkHighContrast,
    onPrimary = onPrimaryDarkHighContrast,
    primaryContainer = primaryContainerDarkHighContrast,
    onPrimaryContainer = onPrimaryContainerDarkHighContrast,
    secondary = secondaryDarkHighContrast,
    onSecondary = onSecondaryDarkHighContrast,
    secondaryContainer = secondaryContainerDarkHighContrast,
    onSecondaryContainer = onSecondaryContainerDarkHighContrast,
    tertiary = tertiaryDarkHighContrast,
    onTertiary = onTertiaryDarkHighContrast,
    tertiaryContainer = tertiaryContainerDarkHighContrast,
    onTertiaryContainer = onTertiaryContainerDarkHighContrast,
    error = errorDarkHighContrast,
    onError = onErrorDarkHighContrast,
    errorContainer = errorContainerDarkHighContrast,
    onErrorContainer = onErrorContainerDarkHighContrast,
    background = backgroundDarkHighContrast,
    onBackground = onBackgroundDarkHighContrast,
    surface = surfaceDarkHighContrast,
    onSurface = onSurfaceDarkHighContrast,
    surfaceVariant = surfaceVariantDarkHighContrast,
    onSurfaceVariant = onSurfaceVariantDarkHighContrast,
    outline = outlineDarkHighContrast,
    outlineVariant = outlineVariantDarkHighContrast,
    scrim = scrimDarkHighContrast,
    inverseSurface = inverseSurfaceDarkHighContrast,
    inverseOnSurface = inverseOnSurfaceDarkHighContrast,
    inversePrimary = inversePrimaryDarkHighContrast,
    surfaceDim = surfaceDimDarkHighContrast,
    surfaceBright = surfaceBrightDarkHighContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkHighContrast,
    surfaceContainerLow = surfaceContainerLowDarkHighContrast,
    surfaceContainer = surfaceContainerDarkHighContrast,
    surfaceContainerHigh = surfaceContainerHighDarkHighContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkHighContrast,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScrollessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content:
    @Composable()
        () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = ScrollessTypography,
        motionScheme = MotionScheme.expressive(),
        shapes = ScrollessShapes,
        content = content,
    )
}
