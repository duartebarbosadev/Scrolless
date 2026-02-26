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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val DAY_TOTAL_MINUTES = 24 * 60
private const val DEFAULT_NEW_SESSION_MINUTES = 10
private val DURATION_OPTIONS = listOf(5, 10, 15, 30, 45, 60)
private val DEBUG_PANEL_MAX_WIDTH = 320.dp
private val TIMELINE_HOUR_WIDTH = 56.dp
private val TIMELINE_LABEL_ROW_HEIGHT = 18.dp
private val TIMELINE_TRACK_WIDTH = TIMELINE_HOUR_WIDTH * 24
private val TIMELINE_TRACK_HEIGHT = 64.dp
private val TIMELINE_VIEWPORT_HEIGHT = 116.dp
private val TIMELINE_SEGMENT_HEIGHT = 34.dp
private val MIN_TIMELINE_SEGMENT_WIDTH = 12.dp
private val TIMELINE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

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
                DebugDayTimelinePanel(
                    sessionSegments = sessionSegments,
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
private fun DebugDayTimelinePanel(
    sessionSegments: List<SessionSegment>,
    onUsageChanged: (List<SessionSegment>) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
    var newSessionDurationMinutes by remember { mutableIntStateOf(DEFAULT_NEW_SESSION_MINUTES) }
    var selectedApp by remember { mutableStateOf(BlockableApp.REELS) }
    var selectedStartMinutes by remember { mutableIntStateOf(nowMinutes) }
    var requestedCenterMinutes by remember { mutableIntStateOf(nowMinutes) }
    var centerRequestToken by remember { mutableIntStateOf(1) }
    val today = LocalDate.now()
    val todaySegments = remember(sessionSegments, today) {
        sessionSegments
            .filter { it.startDateTime.toLocalDate() == today }
            .sortedBy { it.startDateTime }
    }
    val totalMinutes = todaySegments.sumOf { it.durationMillis.toWholeMinutes().coerceAtLeast(0) }

    fun requestCenteredSelection(minutes: Int) {
        val normalizedMinutes = minutes.addMinutesOfDay(0)
        selectedStartMinutes = normalizedMinutes
        requestedCenterMinutes = normalizedMinutes
        centerRequestToken++
    }

    fun addSession() {
        val newSegment = SessionSegment(
            app = selectedApp,
            durationMillis = TimeUnit.MINUTES.toMillis(newSessionDurationMinutes.toLong()),
            startDateTime = today.atStartOfDay().plusMinutes(selectedStartMinutes.toLong()),
        )
        onUsageChanged((todaySegments + newSegment).sortedBy { it.startDateTime })
    }

    Card(
        modifier = modifier.heightIn(max = 460.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today Timeline",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = totalMinutes.formatMinutes(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DURATION_OPTIONS.forEach { option ->
                    DurationOptionChip(
                        minutes = option,
                        selected = option == newSessionDurationMinutes,
                        onClick = { newSessionDurationMinutes = option },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "App",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                BlockableApp.entries.forEach { app ->
                    DurationOptionChip(
                        label = app.displayName(),
                        selected = app == selectedApp,
                        selectedColor = app.timelineColor(),
                        useSoftSelection = true,
                        onClick = { selectedApp = app },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Start ${selectedStartMinutes.toTimeLabel()}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(96.dp),
                    maxLines = 1,
                )
                DurationOptionChip(
                    label = "-1h",
                    selected = false,
                    onClick = { requestCenteredSelection(selectedStartMinutes.addMinutesOfDay(-60)) },
                )
                DurationOptionChip(
                    label = "-15m",
                    selected = false,
                    onClick = { requestCenteredSelection(selectedStartMinutes.addMinutesOfDay(-15)) },
                )
                DurationOptionChip(
                    label = "Now",
                    selected = false,
                    onClick = { requestCenteredSelection(LocalTime.now().hour * 60 + LocalTime.now().minute) },
                )
                DurationOptionChip(
                    label = "+15m",
                    selected = false,
                    onClick = { requestCenteredSelection(selectedStartMinutes.addMinutesOfDay(15)) },
                )
                DurationOptionChip(
                    label = "+1h",
                    selected = false,
                    onClick = { requestCenteredSelection(selectedStartMinutes.addMinutesOfDay(60)) },
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextButton(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    onClick = { addSession() },
                ) {
                    Text(text = "Add Session", style = MaterialTheme.typography.labelLarge)
                }

                TextButton(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    onClick = { onReset() },
                ) {
                    Text(text = "Reset", style = MaterialTheme.typography.labelLarge)
                }
            }

            DayTimeline(
                sessionSegments = todaySegments,
                selectedStartMinutes = selectedStartMinutes,
                requestedCenterMinutes = requestedCenterMinutes,
                centerRequestToken = centerRequestToken,
                onSelectedStartMinutesChange = { updatedMinutes ->
                    selectedStartMinutes = updatedMinutes
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DurationOptionChip(minutes: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DurationOptionChip(
        label = minutes.formatMinutes(),
        selected = selected,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun DurationOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    useSoftSelection: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    val backgroundColor = if (selected) {
        if (useSoftSelection) selectedColor.copy(alpha = 0.20f) else selectedColor
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        if (useSoftSelection) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        selectedColor.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        color = backgroundColor,
        contentColor = contentColor,
        shape = shape,
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DayTimeline(
    sessionSegments: List<SessionSegment>,
    selectedStartMinutes: Int,
    requestedCenterMinutes: Int,
    centerRequestToken: Int,
    onSelectedStartMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val timelineWidthPx = with(density) { TIMELINE_TRACK_WIDTH.roundToPx() }
    val dayStart = LocalDate.now().atStartOfDay()
    val nowMinutes = remember(dayStart) { LocalDateTime.now().minutesSince(dayStart).coerceIn(0, DAY_TOTAL_MINUTES) }

    LaunchedEffect(centerRequestToken, viewportWidthPx, timelineWidthPx, scrollState.maxValue) {
        if (viewportWidthPx <= 0) return@LaunchedEffect
        val selectedMinutes = requestedCenterMinutes.coerceIn(0, DAY_TOTAL_MINUTES - 1)
        val selectedX = (timelineWidthPx * (selectedMinutes / DAY_TOTAL_MINUTES.toFloat())).roundToInt()
        val centeredTarget = (selectedX - (viewportWidthPx / 2)).coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(centeredTarget)
    }

    LaunchedEffect(scrollState, viewportWidthPx, timelineWidthPx) {
        if (viewportWidthPx <= 0 || timelineWidthPx <= 0) return@LaunchedEffect
        snapshotFlow { scrollState.value }.collect { scrollValue ->
            val centerX = (scrollValue + (viewportWidthPx / 2f)).coerceIn(0f, timelineWidthPx.toFloat())
            val centeredMinutes = ((centerX / timelineWidthPx.toFloat()) * DAY_TOTAL_MINUTES)
                .roundToInt()
                .coerceIn(0, DAY_TOTAL_MINUTES - 1)
            if (centeredMinutes != selectedStartMinutes) {
                onSelectedStartMinutesChange(centeredMinutes)
            }
        }
    }

    Column(
        modifier = modifier
            .height(TIMELINE_VIEWPORT_HEIGHT)
            .onSizeChanged { viewportWidthPx = it.width }
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
            .horizontalScroll(scrollState)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.width(TIMELINE_TRACK_WIDTH)) {
            repeat(24) { hour ->
                Box(
                    modifier = Modifier
                        .width(TIMELINE_HOUR_WIDTH)
                        .height(TIMELINE_LABEL_ROW_HEIGHT),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = hour.toHourLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(TIMELINE_TRACK_WIDTH)
                .height(TIMELINE_TRACK_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                repeat(24) { hour ->
                    if (hour > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                        )
                    }
                    Spacer(
                        modifier = Modifier
                            .width(TIMELINE_HOUR_WIDTH - if (hour > 0) 1.dp else 0.dp)
                            .fillMaxHeight(),
                    )
                }
            }

            val nowOffset = TIMELINE_TRACK_WIDTH * (nowMinutes / DAY_TOTAL_MINUTES.toFloat())
            Box(
                modifier = Modifier
                    .offset(x = nowOffset)
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)),
            )

            val selectedOffset = TIMELINE_TRACK_WIDTH * (selectedStartMinutes.coerceIn(0, DAY_TOTAL_MINUTES - 1) / DAY_TOTAL_MINUTES.toFloat())
            Box(
                modifier = Modifier
                    .offset(x = selectedOffset)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
            )

            sessionSegments.forEachIndexed { index, segment ->
                TimelineSessionBlock(
                    segment = segment,
                    index = index,
                    dayStart = dayStart,
                )
            }
        }
    }
}

@Composable
private fun TimelineSessionBlock(segment: SessionSegment, index: Int, dayStart: LocalDateTime) {
    val startMinutes = segment.startDateTime.minutesSince(dayStart).coerceIn(0, DAY_TOTAL_MINUTES - 1)
    val durationMinutes = segment.durationMillis.toWholeMinutes().coerceAtLeast(1)
    val endMinutes = (startMinutes + durationMinutes).coerceAtMost(DAY_TOTAL_MINUTES)
    val leftOffset = TIMELINE_TRACK_WIDTH * (startMinutes / DAY_TOTAL_MINUTES.toFloat())
    val rawWidth = TIMELINE_TRACK_WIDTH * ((endMinutes - startMinutes) / DAY_TOTAL_MINUTES.toFloat())
    val blockWidth = rawWidth.coerceAtLeast(MIN_TIMELINE_SEGMENT_WIDTH)
    val appColor = segment.app.timelineColor()
    val backgroundColor = appColor.copy(alpha = 0.22f + ((index % 2) * 0.08f))

    Box(
        modifier = Modifier
            .offset(
                x = leftOffset,
                y = (TIMELINE_TRACK_HEIGHT - TIMELINE_SEGMENT_HEIGHT) / 2,
            )
            .width(blockWidth)
            .height(TIMELINE_SEGMENT_HEIGHT)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, appColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "${segment.app.displayName()} • ${segment.timelineLabel(durationMinutes)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

private fun Int.toHourLabel(): String = LocalTime.of(this, 0).format(TIMELINE_TIME_FORMATTER)

private fun LocalDateTime.minutesSince(start: LocalDateTime): Int = java.time.Duration.between(start, this).toMinutes().toInt()

private fun SessionSegment.timelineLabel(durationMinutes: Int): String {
    val start = startDateTime
    val end = start.plusMinutes(durationMinutes.toLong())
    return "${start.toLocalTime().format(TIMELINE_TIME_FORMATTER)}-${end.toLocalTime().format(TIMELINE_TIME_FORMATTER)} • ${durationMinutes.formatMinutes()}"
}

private fun Int.toTimeLabel(): String {
    val safe = coerceIn(0, DAY_TOTAL_MINUTES - 1)
    return LocalTime.of(safe / 60, safe % 60).format(TIMELINE_TIME_FORMATTER)
}

private fun Int.addMinutesOfDay(delta: Int): Int {
    val total = (this + delta) % DAY_TOTAL_MINUTES
    return if (total < 0) total + DAY_TOTAL_MINUTES else total
}

private fun BlockableApp.displayName(): String = when (this) {
    BlockableApp.REELS -> "Reels"
    BlockableApp.SHORTS -> "Shorts"
    BlockableApp.TIKTOK -> "TikTok"
}

private fun BlockableApp.timelineColor(): Color = when (this) {
    BlockableApp.REELS -> instagramReelsColor
    BlockableApp.SHORTS -> youtubeShortsColor
    BlockableApp.TIKTOK -> tiktokColor
}

private fun Long.toWholeMinutes(): Int = TimeUnit.MILLISECONDS.toMinutes(this).toInt()

@Preview(name = "Debug Usage Timeline")
@Composable
private fun PreviewDebugUsagePanel() {
    ScrollessTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            FloatingDebugUsagePanel(
                sessionSegments = listOf(
                    SessionSegment(
                        BlockableApp.REELS,
                        TimeUnit.MINUTES.toMillis(18),
                        LocalDate.now().atTime(9, 20),
                    ),
                    SessionSegment(
                        BlockableApp.REELS,
                        TimeUnit.MINUTES.toMillis(35),
                        LocalDate.now().atTime(12, 10),
                    ),
                    SessionSegment(
                        BlockableApp.REELS,
                        TimeUnit.MINUTES.toMillis(22),
                        LocalDate.now().atTime(20, 5),
                    ),
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
