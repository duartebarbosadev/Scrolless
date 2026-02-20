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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.designsystem.theme.instagramReelsColor
import com.scrolless.app.designsystem.theme.tiktokColor
import com.scrolless.app.designsystem.theme.youtubeShortsColor
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.ui.utils.formatMinutes
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val DEBUG_USAGE_MAX_MINUTES = 180
private const val DEBUG_USAGE_MAX_SEGMENTS_PER_APP = 6
private const val DEBUG_USAGE_NEW_SEGMENT_MINUTES = 10
private val DEBUG_PANEL_MAX_WIDTH = 320.dp

@Composable
internal fun FloatingDebugUsagePanel(
    sessionSegments: List<SessionSegment>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onUsageChanged: (List<SessionSegment>) -> Unit,
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
                .widthIn(max = DEBUG_PANEL_MAX_WIDTH),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                    sessionSegments = sessionSegments,
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = if (isExpanded) "Hide" else "Debug Window",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugUsageTuner(
    sessionSegments: List<SessionSegment>,
    isExpanded: Boolean,
    onUsageChanged: (List<SessionSegment>) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reelsSegments = remember(sessionSegments) {
        sessionSegments.segmentMinutesFor(BlockableApp.REELS).ifEmpty { listOf(0) }.toStateList()
    }
    val shortsSegments = remember(sessionSegments) {
        sessionSegments.segmentMinutesFor(BlockableApp.SHORTS).ifEmpty { listOf(0) }.toStateList()
    }
    val tiktokSegments = remember(sessionSegments) {
        sessionSegments.segmentMinutesFor(BlockableApp.TIKTOK).ifEmpty { listOf(0) }.toStateList()
    }
    val headerBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
        ),
    )
    val baseTime = remember { LocalDateTime.now() }

    fun commitUsage() {
        onUsageChanged(
            buildUsageSegments(
                reelsSegments = reelsSegments,
                shortsSegments = shortsSegments,
                tiktokSegments = tiktokSegments,
                baseTime = baseTime,
            ),
        )
    }

    Card(
        modifier = modifier.heightIn(max = 420.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBrush)
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    onClick = {
                        reelsSegments.clear()
                        shortsSegments.clear()
                        tiktokSegments.clear()
                        reelsSegments.add(0)
                        shortsSegments.add(0)
                        tiktokSegments.add(0)
                        commitUsage()
                        onReset()
                    },
                ) {
                    Text(text = "Reset", style = MaterialTheme.typography.labelSmall)
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DebugUsageSegmentGroup(
                        appName = "Reels",
                        color = instagramReelsColor,
                        segments = reelsSegments,
                        onSegmentsChange = { commitUsage() },
                    )
                    DebugUsageSegmentGroup(
                        appName = "Shorts",
                        color = youtubeShortsColor,
                        segments = shortsSegments,
                        onSegmentsChange = { commitUsage() },
                    )
                    DebugUsageSegmentGroup(
                        appName = "TikTok",
                        color = tiktokColor,
                        segments = tiktokSegments,
                        onSegmentsChange = { commitUsage() },
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugUsageSegmentGroup(
    appName: String,
    color: Color,
    segments: SnapshotStateList<Int>,
    onSegmentsChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalMinutes = segments.sum()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(color),
            )
            Text(
                text = appName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = totalMinutes.formatMinutes(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                enabled = segments.size < DEBUG_USAGE_MAX_SEGMENTS_PER_APP,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                onClick = {
                    if (segments.size < DEBUG_USAGE_MAX_SEGMENTS_PER_APP) {
                        segments.add(DEBUG_USAGE_NEW_SEGMENT_MINUTES)
                        onSegmentsChange()
                    }
                },
            ) {
                Text(text = "Add", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(
                enabled = segments.size > 1,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                onClick = {
                    if (segments.size > 1) {
                        segments.removeAt(segments.lastIndex)
                        onSegmentsChange()
                    }
                },
            ) {
                Text(text = "Remove", style = MaterialTheme.typography.labelMedium)
            }
        }

        segments.forEachIndexed { index, minutes ->
            DebugUsageSlider(
                label = "Segment ${index + 1}",
                color = color,
                minutes = minutes,
                onMinutesChange = { newValue ->
                    segments[index] = newValue
                    onSegmentsChange()
                },
            )
        }
    }
}

@Composable
private fun DebugUsageSlider(label: String, color: Color, minutes: Int, onMinutesChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val clampedMinutes = minutes.coerceIn(0, DEBUG_USAGE_MAX_MINUTES)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = clampedMinutes.formatMinutes(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
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

private fun List<SessionSegment>.segmentMinutesFor(app: BlockableApp): List<Int> = this.filter { it.app == app }
    .sortedBy { it.startDateTime }
    .map { it.durationMillis.toWholeMinutes().coerceAtLeast(0) }

private fun List<Int>.toStateList(): SnapshotStateList<Int> = mutableStateListOf<Int>().also { list -> list.addAll(this) }

private fun buildUsageSegments(
    reelsSegments: List<Int>,
    shortsSegments: List<Int>,
    tiktokSegments: List<Int>,
    baseTime: LocalDateTime,
): List<SessionSegment> {
    val result = mutableListOf<SessionSegment>()
    result += buildUsageSegmentsForApp(BlockableApp.REELS, reelsSegments, baseTime)
    result += buildUsageSegmentsForApp(BlockableApp.SHORTS, shortsSegments, baseTime.minusHours(1))
    result += buildUsageSegmentsForApp(BlockableApp.TIKTOK, tiktokSegments, baseTime.minusHours(2))
    return result
}

private fun buildUsageSegmentsForApp(app: BlockableApp, minutesSegments: List<Int>, baseTime: LocalDateTime): List<SessionSegment> =
    minutesSegments.mapIndexed { index, minutes ->
        val offsetMinutes = (minutesSegments.size - 1 - index).coerceAtLeast(0)
        SessionSegment(
            app = app,
            durationMillis = TimeUnit.MINUTES.toMillis(minutes.coerceAtLeast(0).toLong()),
            startDateTime = baseTime.minusMinutes(offsetMinutes.toLong()),
        )
    }

@Preview(name = "Debug Usage Panel")
@Composable
private fun PreviewDebugUsagePanel() {
    ScrollessTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            FloatingDebugUsagePanel(
                sessionSegments = listOf(
                    SessionSegment(BlockableApp.REELS, TimeUnit.MINUTES.toMillis(42), LocalDateTime.now()),
                    SessionSegment(BlockableApp.SHORTS, TimeUnit.MINUTES.toMillis(18), LocalDateTime.now()),
                    SessionSegment(BlockableApp.TIKTOK, TimeUnit.MINUTES.toMillis(65), LocalDateTime.now()),
                ),
                isExpanded = true,
                onToggleExpanded = {},
                onUsageChanged = {},
                onReset = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
