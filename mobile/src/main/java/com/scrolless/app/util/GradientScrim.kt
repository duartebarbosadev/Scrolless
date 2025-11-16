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

/**
 * Applies a radial gradient scrim in the foreground emanating from the top
 * center quarter of the element.
 */
@Composable
fun Modifier.radialGradientScrim(color: Color): Modifier {
    val transition = rememberInfiniteTransition(label = "GradientScrimPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
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

    val baseAlpha = if (color.alpha == 1f) 0.16f else color.alpha
    val highlightColor = lerp(
        start = color.copy(alpha = baseAlpha),
        stop = Color.White.copy(alpha = baseAlpha),
        fraction = 0.2f,
    )
    val animatedColor = lerp(
        start = color.copy(alpha = baseAlpha),
        stop = highlightColor,
        fraction = tintShift * 0.7f,
    )
    val innerAlpha = (baseAlpha * pulse).coerceAtMost(0.28f)
    val midAlpha = (innerAlpha * 0.55f).coerceAtMost(0.18f)

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
                radius = largerDimension / 2 * pulse,
                colorStops = listOf(0f, 0.55f, 0.95f),
            )
        }
    }
    return this.background(radialGradient)
}
