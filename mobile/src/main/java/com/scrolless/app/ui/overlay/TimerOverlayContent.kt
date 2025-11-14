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
package com.scrolless.app.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.util.formatAsTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Visual layer for the floating timer overlay.
 */
@Composable
internal fun TimerOverlayContent(sessionStartTime: Long, displayMode: OverlayMode, summaryText: String) {
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val wiggleRotation = remember { Animatable(0f) }

    LaunchedEffect(sessionStartTime, displayMode) {
        if (displayMode == OverlayMode.Timer) {
            while (isActive) {
                elapsedTime = System.currentTimeMillis() - sessionStartTime
                delay(1000)
            }
        } else {
            elapsedTime = System.currentTimeMillis() - sessionStartTime
        }
    }

    LaunchedEffect(displayMode) {
        if (displayMode == OverlayMode.Summary) {
            wiggleRotation.snapTo(0f)
            val targets = listOf(8f, -8f, 5f, -5f, 3f, -3f, 0f)
            targets.forEach { angle ->
                wiggleRotation.animateTo(angle, animationSpec = tween(durationMillis = 70))
            }
        } else {
            wiggleRotation.animateTo(0f, animationSpec = tween(durationMillis = 150))
        }
    }

    val textToDisplay = when (displayMode) {
        OverlayMode.Timer -> elapsedTime.formatAsTime()
        OverlayMode.Summary -> summaryText
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                rotationZ = wiggleRotation.value
            }
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = textToDisplay,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Preview(name = "Timer")
@Composable
private fun TimerOverlayContentPreview() {
    ScrollessTheme {
        TimerOverlayContent(
            sessionStartTime = System.currentTimeMillis() - 83_000L,
            displayMode = OverlayMode.Timer,
            summaryText = "",
        )
    }
}

@Preview(name = "Summary")
@Composable
private fun TimerOverlaySummaryPreview() {
    ScrollessTheme {
        TimerOverlayContent(
            sessionStartTime = System.currentTimeMillis() - 83_000L,
            displayMode = OverlayMode.Summary,
            summaryText = "12:34",
        )
    }
}

internal enum class OverlayMode {
    Timer,
    Summary,
}
