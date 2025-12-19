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
package com.scrolless.app.util

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kotlin.math.max
import kotlin.math.min

/**
 * Applies a radial gradient scrim in the foreground emanating from the top
 * center quarter of the element.
 */
@Composable
fun Modifier.radialGradientScrim(
    baseColor: Color,
    accentColor: Color? = null,
    accentStrength: Float = 0f,
): Modifier {
    val transition = rememberInfiniteTransition(label = "GradientScrimPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ScrimPulse",
    )
    val tintShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ScrimTintShift",
    )

    val clampedAccentStrength = accentStrength.coerceIn(0f, 1f)
    val blendedBase = if (accentColor != null) {
        lerp(baseColor, accentColor, clampedAccentStrength)
    } else {
        baseColor
    }
    val baseAlpha = max(blendedBase.alpha, 0.32f)
    val highlightMix = (0.48f - (clampedAccentStrength * 0.12f)).coerceIn(0.28f, 0.55f)
    val palette = listOf(
        blendedBase.copy(alpha = baseAlpha),
        lerp(blendedBase, Color(0xFF58C7FF), highlightMix).copy(alpha = baseAlpha), // electric sky
        lerp(blendedBase, Color(0xFF9C7BFF), highlightMix).copy(alpha = baseAlpha), // violet glow
        lerp(blendedBase, Color(0xFF5DFFB3), highlightMix).copy(alpha = baseAlpha), // neon mint
        blendedBase.copy(alpha = baseAlpha * 0.9f),
    )
    val animatedColor = lerpPalette(
        palette = palette,
        fraction = 0.2f + (tintShift * 0.8f),
    )
    val innerAlpha = (baseAlpha * (0.82f + pulse * 0.6f) * (0.85f + (clampedAccentStrength * 0.35f)))
        .coerceAtMost(0.4f)
    val midAlpha = (innerAlpha * 0.45f).coerceAtMost(0.22f)

    val radialGradient = object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val largerDimension = max(size.height, size.width)
            return RadialGradientShader(
                center = size.center.copy(y = size.height / 4),
                colors = listOf(
                    animatedColor.copy(alpha = innerAlpha),
                    animatedColor.copy(alpha = midAlpha),
                    Color.Transparent,
                ),
                radius = largerDimension * 0.55f * pulse,
                colorStops = listOf(0f, 0.52f, 0.93f),
            )
        }
    }
    return this.background(radialGradient)
}

/**
 * Linearly interpolates across a list of [Color] entries.
 *
 * The [fraction] is clamped to `[0f, 1f]` and mapped across the palette indexes, so `0f`
 * returns the first color, `1f` the last, and intermediate values blend smoothly between
 * adjacent entries. Returns transparent when the palette is empty, or the single entry
 * when only one color is provided.
 */
private fun lerpPalette(palette: List<Color>, fraction: Float): Color {
    if (palette.isEmpty()) return Color.Transparent
    if (palette.size == 1) return palette.first()

    val clamped = fraction.coerceIn(0f, 1f)
    val scaled = clamped * (palette.size - 1)
    val startIndex = scaled.toInt()
    val endIndex = min(startIndex + 1, palette.lastIndex)
    val blend = scaled - startIndex

    return lerp(palette[startIndex], palette[endIndex], blend)
}
