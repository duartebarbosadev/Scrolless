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
package com.scrolless.app.feature.home.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.designsystem.theme.LocalIsLightTheme
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.theme.facebookColor
import com.scrolless.app.designsystem.theme.facebookLiteColor
import com.scrolless.app.designsystem.theme.instagramReelsColor
import com.scrolless.app.designsystem.theme.snapchatColor
import com.scrolless.app.designsystem.theme.tiktokColor
import com.scrolless.app.designsystem.theme.tiktokLightThemeColor
import com.scrolless.app.designsystem.theme.tiktokLiteColor
import com.scrolless.app.designsystem.theme.tiktokLiteLightThemeColor
import com.scrolless.app.designsystem.theme.youtubeShortsColor
import com.scrolless.app.feature.home.AppUsageTotal
import com.scrolless.app.feature.home.R
import com.scrolless.app.feature.home.UsageAnalyticsDayUiState
import com.scrolless.app.feature.home.UsageAnalyticsUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

val ANALYTICS_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
val ANALYTICS_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun InlineUsageAnalyticsPanel(analytics: UsageAnalyticsUiState, sessionChunksExpanded: Boolean, onToggleSessionChunks: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        UsageAnalyticsDayPage(
            analytics = analytics,
            sessionChunksExpanded = sessionChunksExpanded,
            onToggleSessionChunks = onToggleSessionChunks,
        )
    }
}

@Composable
fun UsageAnalyticsDayPage(
    analytics: UsageAnalyticsUiState,
    sessionChunksExpanded: Boolean = false,
    onToggleSessionChunks: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        UsageTimelineSection(
            analytics = analytics,
            sessionChunksExpanded = sessionChunksExpanded,
            onToggleSessionChunks = onToggleSessionChunks,
        )
    }
}

@Composable
fun UsageTimelineSection(analytics: UsageAnalyticsUiState, sessionChunksExpanded: Boolean, onToggleSessionChunks: () -> Unit) {
    val sessionSegments = analytics.sessionSegments
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    enabled = sessionSegments.isNotEmpty(),
                    onClick = onToggleSessionChunks,
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                // Shrinks first so a long session label never pushes Expand/Collapse onto two lines.
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.usage_analytics_timeline_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (sessionSegments.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.usage_analytics_session_count,
                                    sessionSegments.size,
                                    sessionSegments.size,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )

                            Text(
                                text = analytics.dailyTotalMillis.formatAnalyticsDuration(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                if (sessionSegments.isNotEmpty()) {
                    val iconRotation by animateFloatAsState(
                        targetValue = if (sessionChunksExpanded) 180f else 0f,
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        label = "expandIconRotation",
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = if (sessionChunksExpanded) {
                                stringResource(R.string.usage_analytics_collapse)
                            } else {
                                stringResource(R.string.usage_analytics_expand)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = if (sessionChunksExpanded) {
                                stringResource(R.string.usage_analytics_collapse)
                            } else {
                                stringResource(R.string.usage_analytics_expand)
                            },
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .width(20.dp)
                                .rotate(iconRotation),
                        )
                    }
                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                UsageTimelineCanvas(
                    sessionSegments = sessionSegments,
                    selectedDate = analytics.selectedDate,
                )
                if (sessionSegments.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = sessionChunksExpanded,
                        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                            expandVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                            shrinkVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            sessionSegments.forEach { segment ->
                                SessionChunkRow(segment = segment)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsageTimelineCanvas(sessionSegments: List<SessionSegment>, selectedDate: LocalDate) {
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val trackColor = MaterialTheme.colorScheme.surface

    val transitionProgress = remember(selectedDate) { Animatable(0f) }
    LaunchedEffect(selectedDate) {
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioMediumBouncy,
            ),
        )
    }

    val window = remember(sessionSegments) { timelineWindowFor(sessionSegments) }
    val lightTheme = LocalIsLightTheme.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            val trackTop = size.height * 0.28f
            val trackHeight = size.height * 0.44f
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
            )

            window.tickHours.forEach { hour ->
                val x = size.width * window.fractionOf(hour * 60f)
                drawLine(
                    color = outline,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            sessionSegments.forEachIndexed { index, segment ->
                val progress = (transitionProgress.value * (1f + index * 0.05f) - index * 0.05f).coerceAtLeast(0f)
                val startMinutes = segment.startDateTime.toLocalTime().toSecondOfDay() / 60f
                val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(segment.durationMillis).coerceAtLeast(1).toFloat()
                val startX = size.width * window.fractionOf(startMinutes)
                val width = (size.width * (durationMinutes / window.spanMinutes))
                    .coerceAtMost(size.width - startX)
                    .coerceAtLeast(6.dp.toPx())
                // A session at the window's right edge starts slightly earlier so it keeps its minimum width
                val left = startX.coerceAtMost(size.width - width)

                val animatedHeight = trackHeight * progress
                val animatedTop = trackTop + (trackHeight - animatedHeight) / 2f

                drawRoundRect(
                    color = segment.app.analyticsColor(lightTheme),
                    topLeft = Offset(left, animatedTop),
                    size = Size(width, animatedHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
                )
            }
        }
        TimelineTickLabels(tickHours = window.tickHours)
    }
}

/**
 * The part of the day the timeline shows, in whole hours split into four equal steps.
 *
 * Zooming in on the hours with activity keeps short sessions readable instead of thin slivers on a
 * 24 hour bar. A day without sessions shows the full day.
 */
internal data class TimelineWindow(val startHour: Int, val endHour: Int) {
    val spanMinutes: Float get() = (endHour - startHour) * 60f
    val tickHours: List<Int> get() = (0..TIMELINE_TICK_STEPS).map { startHour + it * (endHour - startHour) / TIMELINE_TICK_STEPS }

    fun fractionOf(minuteOfDay: Float): Float = ((minuteOfDay - startHour * 60f) / spanMinutes).coerceIn(0f, 1f)
}

private const val TIMELINE_TICK_STEPS = 4
private const val HOURS_PER_DAY = 24

internal fun timelineWindowFor(sessionSegments: List<SessionSegment>): TimelineWindow {
    if (sessionSegments.isEmpty()) return TimelineWindow(startHour = 0, endHour = HOURS_PER_DAY)

    val firstStartHour = sessionSegments.minOf { it.startDateTime.hour }
    // Sessions count as at least a minute, like the bars drawn for them, so one that starts on the
    // hour still falls inside the window
    val lastEndMinute = sessionSegments.maxOf { segment ->
        segment.startDateTime.toLocalTime().toSecondOfDay() / 60 +
            TimeUnit.MILLISECONDS.toMinutes(segment.durationMillis).coerceAtLeast(1).toInt()
    }
    val lastEndHour = ((lastEndMinute + 59) / 60).coerceIn(firstStartHour + 1, HOURS_PER_DAY)

    // Round the span up to a multiple of the tick count so every tick lands on a whole hour
    val activeSpan = lastEndHour - firstStartHour
    val span = (((activeSpan + TIMELINE_TICK_STEPS - 1) / TIMELINE_TICK_STEPS) * TIMELINE_TICK_STEPS).coerceAtMost(HOURS_PER_DAY)
    val startHour = (firstStartHour - (span - activeSpan) / 2).coerceIn(0, HOURS_PER_DAY - span)
    return TimelineWindow(startHour = startHour, endHour = startHour + span)
}

@Composable
fun TimelineTickLabels(tickHours: List<Int>) {
    // Each label is centred under its tick line, with the first and last kept inside the bar
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            tickHours.map { "${it.toString().padStart(2, '0')}:00" }.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        val steps = (placeables.size - 1).coerceAtLeast(1)
        layout(width, placeables.maxOfOrNull { it.height } ?: 0) {
            placeables.forEachIndexed { index, placeable ->
                val centre = width * index / steps
                placeable.place((centre - placeable.width / 2).coerceIn(0, (width - placeable.width).coerceAtLeast(0)), 0)
            }
        }
    }
}

@Composable
fun SessionChunkRow(segment: SessionSegment) {
    val lightTheme = LocalIsLightTheme.current
    val startTime = segment.startDateTime.toLocalTime()
    val endTime = segment.startDateTime.plusNanos(TimeUnit.MILLISECONDS.toNanos(segment.durationMillis)).toLocalTime()
    Row(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(42.dp)
                .background(segment.app.analyticsColor(lightTheme), MaterialTheme.shapes.extraSmall),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = segment.app.analyticsDisplayName(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.usage_analytics_time_range,
                    startTime.format(ANALYTICS_TIME_FORMATTER),
                    endTime.format(ANALYTICS_TIME_FORMATTER),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = segment.app.analyticsColor(lightTheme).copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            AutoResizingText(
                text = segment.durationMillis.formatAnalyticsDuration(),
                modifier = Modifier
                    .widthIn(min = 54.dp, max = 92.dp)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minFontSize = 10.sp,
            )
        }
    }
}

// --- Analytics utility functions ---

fun Long.formatAnalyticsDuration(): String {
    val totalSeconds = (this / 1_000L).coerceAtLeast(0L)
    val totalMinutes = totalSeconds / 60L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    val seconds = totalSeconds % 60L

    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        minutes > 0L -> "${minutes}m"
        else -> "${seconds}s"
    }
}

fun analyticsForDate(analytics: UsageAnalyticsUiState, date: LocalDate): UsageAnalyticsUiState {
    val dayAnalytics = analytics.daySummaries[date] ?: UsageAnalyticsDayUiState(date = date)
    return analytics.copy(
        selectedDate = date,
        dailyTotalMillis = dayAnalytics.dailyTotalMillis,
        sessionSegments = dayAnalytics.sessionSegments,
        appTotals = dayAnalytics.appTotals,
        canNavigateNext = date.isBefore(analytics.today),
    )
}

fun BlockableApp.displayName(context: Context): String = context.getString(
    when (this) {
        BlockableApp.FACEBOOK -> R.string.app_facebook
        BlockableApp.FACEBOOK_LITE -> R.string.app_facebook_lite
        BlockableApp.REELS -> R.string.app_reels
        BlockableApp.SNAPCHAT -> R.string.app_snapchat
        BlockableApp.SHORTS -> R.string.app_shorts
        BlockableApp.TIKTOK -> R.string.app_tiktok
        BlockableApp.TIKTOK_LITE -> R.string.app_tiktok_lite
    },
)

@Composable
fun BlockableApp.analyticsDisplayName(): String = displayName(LocalContext.current)

/** The cyan used for TikTok is too bright to read on light surfaces, so light themes get a deeper teal. */
fun BlockableApp.analyticsColor(lightTheme: Boolean = false): Color = when (this) {
    BlockableApp.FACEBOOK -> facebookColor
    BlockableApp.FACEBOOK_LITE -> facebookLiteColor
    BlockableApp.REELS -> instagramReelsColor
    BlockableApp.SNAPCHAT -> snapchatColor
    BlockableApp.SHORTS -> youtubeShortsColor
    BlockableApp.TIKTOK -> if (lightTheme) tiktokLightThemeColor else tiktokColor
    BlockableApp.TIKTOK_LITE -> if (lightTheme) tiktokLiteLightThemeColor else tiktokLiteColor
}

// --- Analytics previews ---

@Preview(name = "Usage Analytics Previous Day")
@Composable
private fun PreviewUsageAnalyticsPreviousDay() {
    ScrollessTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InlineUsageAnalyticsPanel(
                    analytics = previewUsageAnalytics(populated = true),
                    sessionChunksExpanded = false,
                    onToggleSessionChunks = {},
                )
            }
        }
    }
}

@Preview(name = "Usage Analytics Expanded")
@Composable
private fun PreviewUsageAnalyticsExpanded() {
    ScrollessTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InlineUsageAnalyticsPanel(
                    analytics = previewUsageAnalytics(populated = true),
                    sessionChunksExpanded = true,
                    onToggleSessionChunks = {},
                )
            }
        }
    }
}

@Preview(name = "Usage Analytics Empty")
@Composable
private fun PreviewUsageAnalyticsEmpty() {
    ScrollessTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InlineUsageAnalyticsPanel(
                    analytics = previewUsageAnalytics(populated = false),
                    sessionChunksExpanded = false,
                    onToggleSessionChunks = {},
                )
            }
        }
    }
}

private fun previewUsageAnalytics(populated: Boolean): UsageAnalyticsUiState {
    val date = LocalDate.of(2026, 5, 26)
    val segments = if (!populated) {
        emptyList()
    } else {
        listOf(
            SessionSegment(BlockableApp.REELS, TimeUnit.MINUTES.toMillis(18), date.atTime(8, 10)),
            SessionSegment(BlockableApp.SHORTS, TimeUnit.MINUTES.toMillis(12), date.atTime(12, 35)),
            SessionSegment(BlockableApp.TIKTOK, TimeUnit.MINUTES.toMillis(22), date.atTime(19, 20)),
            SessionSegment(BlockableApp.REELS, TimeUnit.MINUTES.toMillis(9), date.atTime(22, 5)),
        )
    }
    val appTotals = segments.groupBy { it.app }.map { (app, appSegments) ->
        AppUsageTotal(app = app, totalMillis = appSegments.sumOf { it.durationMillis })
    }.sortedByDescending { it.totalMillis }
    return UsageAnalyticsUiState(
        selectedDate = date,
        today = date,
        dailyTotalMillis = segments.sumOf { it.durationMillis },
        sessionSegments = segments,
        appTotals = appTotals,
        weekdayAverages = emptyList(),
    )
}
