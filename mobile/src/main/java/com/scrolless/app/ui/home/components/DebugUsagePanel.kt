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
package com.scrolless.app.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.scrolless.app.designsystem.theme.instagramReelsColor
import com.scrolless.app.designsystem.theme.tiktokColor
import com.scrolless.app.designsystem.theme.youtubeShortsColor
import com.scrolless.app.ui.home.PerAppUsage
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.ui.utils.formatMinutes
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val DEBUG_USAGE_MAX_MINUTES = 180

@Composable
internal fun FloatingDebugUsagePanel(
    perAppUsage: PerAppUsage,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onUsageChanged: (Int, Int, Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val paddingPx = with(density) { 16.dp.roundToPx() }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    var offsetPx by remember(paddingPx) { mutableStateOf(IntOffset(-paddingPx, -paddingPx)) }

    Box(
        modifier = modifier.onSizeChanged { newSize ->
            containerSize = newSize
        },
    ) {
        val maxWidthPx = containerSize.width
        val maxHeightPx = containerSize.height

        fun clampOffset(candidate: IntOffset): IntOffset {
            if (panelSize == IntSize.Zero || maxWidthPx == 0 || maxHeightPx == 0) {
                return candidate
            }
            val minX = panelSize.width + paddingPx - maxWidthPx
            val maxX = -paddingPx
            val minY = panelSize.height + paddingPx - maxHeightPx
            val maxY = -paddingPx
            val clampedX = if (minX <= maxX) candidate.x.coerceIn(minX, maxX) else maxX
            val clampedY = if (minY <= maxY) candidate.y.coerceIn(minY, maxY) else maxY
            return IntOffset(clampedX, clampedY)
        }

        LaunchedEffect(containerSize) {
            offsetPx = clampOffset(offsetPx)
        }

        Column(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    val newSize = coordinates.size
                    if (panelSize != newSize) {
                        panelSize = newSize
                        offsetPx = clampOffset(offsetPx)
                    }
                }
                .align(Alignment.BottomEnd)
                .offset { offsetPx }
                .pointerInput(panelSize, maxWidthPx, maxHeightPx) {
                    detectDragGestures { _, dragAmount ->
                        val nextOffset = IntOffset(
                            x = offsetPx.x + dragAmount.x.roundToInt(),
                            y = offsetPx.y + dragAmount.y.roundToInt(),
                        )
                        offsetPx = clampOffset(nextOffset)
                    }
                }
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Bottom,
                    animationSpec = tween(220),
                ) + fadeIn(animationSpec = tween(160)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Bottom,
                    animationSpec = tween(220),
                ) + fadeOut(animationSpec = tween(160)),
            ) {
                DebugUsageTuner(
                    perAppUsage = perAppUsage,
                    isExpanded = isExpanded,
                    onUsageChanged = onUsageChanged,
                    onReset = onReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.98f),
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onToggleExpanded),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = if (isExpanded) "Hide" else "Debug Window",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugUsageTuner(
    perAppUsage: PerAppUsage,
    isExpanded: Boolean,
    onUsageChanged: (Int, Int, Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reelsMinutes by remember(perAppUsage.reelsUsage) {
        mutableIntStateOf(perAppUsage.reelsUsage.toWholeMinutes())
    }
    var shortsMinutes by remember(perAppUsage.shortsUsage) {
        mutableIntStateOf(perAppUsage.shortsUsage.toWholeMinutes())
    }
    var tiktokMinutes by remember(perAppUsage.tiktokUsage) {
        mutableIntStateOf(perAppUsage.tiktokUsage.toWholeMinutes())
    }
    val headerBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
        ),
    )

    fun commitUsage() {
        onUsageChanged(reelsMinutes, shortsMinutes, tiktokMinutes)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBrush)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        reelsMinutes = 0
                        shortsMinutes = 0
                        tiktokMinutes = 0
                        onReset()
                    },
                ) {
                    Text(text = "Reset")
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(200),
                ) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(200),
                ) + fadeOut(animationSpec = tween(150)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DebugUsageSlider(
                        appName = "Reels",
                        color = instagramReelsColor,
                        minutes = reelsMinutes,
                        onMinutesChange = { newValue ->
                            reelsMinutes = newValue
                            commitUsage()
                        },
                    )
                    DebugUsageSlider(
                        appName = "Shorts",
                        color = youtubeShortsColor,
                        minutes = shortsMinutes,
                        onMinutesChange = { newValue ->
                            shortsMinutes = newValue
                            commitUsage()
                        },
                    )
                    DebugUsageSlider(
                        appName = "TikTok",
                        color = tiktokColor,
                        minutes = tiktokMinutes,
                        onMinutesChange = { newValue ->
                            tiktokMinutes = newValue
                            commitUsage()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugUsageSlider(appName: String, color: Color, minutes: Int, onMinutesChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val clampedMinutes = minutes.coerceIn(0, DEBUG_USAGE_MAX_MINUTES)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(color),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = appName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = clampedMinutes.formatMinutes(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = clampedMinutes.toFloat(),
            onValueChange = { rawValue ->
                onMinutesChange(rawValue.roundToInt().coerceIn(0, DEBUG_USAGE_MAX_MINUTES))
            },
            valueRange = 0f..DEBUG_USAGE_MAX_MINUTES.toFloat(),
            steps = DEBUG_USAGE_MAX_MINUTES - 1,
            colors = SliderDefaults.colors(
                activeTrackColor = color,
                thumbColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f),
            ),
        )
    }
}

private fun Long.toWholeMinutes(): Int = TimeUnit.MILLISECONDS.toMinutes(this).toInt()

@Preview(name = "Debug Usage Panel")
@Composable
private fun PreviewDebugUsagePanel() {
    ScrollessTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            FloatingDebugUsagePanel(
                perAppUsage = PerAppUsage(
                    reelsUsage = TimeUnit.MINUTES.toMillis(42),
                    shortsUsage = TimeUnit.MINUTES.toMillis(18),
                    tiktokUsage = TimeUnit.MINUTES.toMillis(65),
                ),
                isExpanded = true,
                onToggleExpanded = {},
                onUsageChanged = { _, _, _ -> },
                onReset = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
