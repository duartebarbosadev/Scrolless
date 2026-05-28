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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.model.usage.WeekdayUsageAverage
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.theme.facebookColor
import com.scrolless.app.designsystem.theme.facebookLiteColor
import com.scrolless.app.designsystem.theme.instagramReelsColor
import com.scrolless.app.designsystem.theme.progressbar_green_use
import com.scrolless.app.designsystem.theme.progressbar_orange_use
import com.scrolless.app.designsystem.theme.progressbar_red_use
import com.scrolless.app.designsystem.theme.snapchatColor
import com.scrolless.app.designsystem.theme.tiktokColor
import com.scrolless.app.designsystem.theme.youtubeShortsColor
import com.scrolless.app.feature.home.AppUsageTotal
import com.scrolless.app.feature.home.R
import com.scrolless.app.feature.home.UsageAnalyticsDayUiState
import com.scrolless.app.feature.home.UsageAnalyticsUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

const val ANALYTICS_PAGER_DAY_COUNT = 365 // 365 days should be enough for anyone :)
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
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    enabled = sessionSegments.isNotEmpty(),
                    onClick = onToggleSessionChunks,
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
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
                        Text(
                            text = stringResource(R.string.usage_analytics_session_count, sessionSegments.size),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = analytics.dailyTotalMillis.formatAnalyticsDuration(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (sessionSegments.isNotEmpty()) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        text = if (sessionChunksExpanded) {
                            stringResource(R.string.usage_analytics_collapse)
                        } else {
                            stringResource(R.string.usage_analytics_expand)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(durationMillis = 320)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                UsageTimelineCanvas(sessionSegments = sessionSegments)
                if (sessionSegments.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = sessionChunksExpanded,
                        enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(260)),
                        exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(200)),
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
fun UsageTimelineCanvas(sessionSegments: List<SessionSegment>) {
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val trackColor = MaterialTheme.colorScheme.surface

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

            for (hour in 0..24 step 6) {
                val x = size.width * (hour / 24f)
                drawLine(
                    color = outline,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            sessionSegments.forEach { segment ->
                val startMinutes = segment.startDateTime.toLocalTime().toSecondOfDay() / 60f
                val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(segment.durationMillis).coerceAtLeast(1).toFloat()
                val startX = size.width * (startMinutes / 1440f)
                val width = (size.width * (durationMinutes / 1440f)).coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                    color = segment.app.analyticsColor(),
                    topLeft = Offset(startX, trackTop),
                    size = Size(width.coerceAtMost(size.width - startX), trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
                )
            }
        }
        TimelineTickLabels()
    }
}

@Composable
fun TimelineTickLabels() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SessionChunkRow(segment: SessionSegment) {
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
                .background(segment.app.analyticsColor(), RoundedCornerShape(4.dp)),
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
            shape = RoundedCornerShape(12.dp),
            color = segment.app.analyticsColor().copy(alpha = 0.12f),
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

@Composable
fun WeekdayAverageSection(weekdayAverages: List<WeekdayUsageAverage>) {
    val maxValue = weekdayAverages.maxOfOrNull { it.averageMillis } ?: 0L

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitleRow(
            title = stringResource(R.string.usage_analytics_average_title),
            trailing = stringResource(R.string.usage_analytics_average_subtitle),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                weekdayAverages.forEach { average ->
                    WeekdayAverageBar(
                        average = average,
                        maxValue = maxValue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun WeekdayAverageBar(average: WeekdayUsageAverage, maxValue: Long, modifier: Modifier = Modifier) {
    val fraction = if (maxValue > 0L) {
        average.averageMillis.toFloat() / maxValue.toFloat()
    } else {
        0f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = if (average.averageMillis > 0L) fraction.coerceIn(0.08f, 1f) else 0.04f,
        animationSpec = tween(durationMillis = 720),
        label = "weekdayAverageFraction",
    )
    val barColor by animateColorAsState(
        targetValue = usageIntensityColor(fraction),
        animationSpec = tween(durationMillis = 420),
        label = "weekdayAverageColor",
    )
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        AutoResizingText(
            text = average.averageMillis.formatAnalyticsDuration(),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            minFontSize = 8.sp,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFraction)
                    .clip(RoundedCornerShape(9.dp))
                    .background(barColor),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = average.dayOfWeek.shortLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun SectionTitleRow(title: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
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

fun usageIntensityColor(fraction: Float): Color = when {
    fraction >= 0.72f -> progressbar_red_use.copy(alpha = 0.82f)
    fraction >= 0.38f -> progressbar_orange_use.copy(alpha = 0.82f)
    else -> progressbar_green_use.copy(alpha = 0.82f)
}

fun pageDateForPage(page: Int, today: LocalDate, todayPage: Int): LocalDate {
    val daysBack = todayPage - page
    return today.minusDays(daysBack.toLong())
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

@Composable
fun BlockableApp.analyticsDisplayName(): String = when (this) {
    BlockableApp.FACEBOOK -> stringResource(R.string.app_facebook)
    BlockableApp.FACEBOOK_LITE -> stringResource(R.string.app_facebook_lite)
    BlockableApp.REELS -> stringResource(R.string.app_reels)
    BlockableApp.SNAPCHAT -> stringResource(R.string.app_snapchat)
    BlockableApp.SHORTS -> stringResource(R.string.app_shorts)
    BlockableApp.TIKTOK -> stringResource(R.string.app_tiktok)
}

fun BlockableApp.analyticsColor(): Color = when (this) {
    BlockableApp.FACEBOOK -> facebookColor
    BlockableApp.FACEBOOK_LITE -> facebookLiteColor
    BlockableApp.REELS -> instagramReelsColor
    BlockableApp.SNAPCHAT -> snapchatColor
    BlockableApp.SHORTS -> youtubeShortsColor
    BlockableApp.TIKTOK -> tiktokColor
}

@Composable
fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> stringResource(R.string.usage_analytics_day_mon)
    DayOfWeek.TUESDAY -> stringResource(R.string.usage_analytics_day_tue)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.usage_analytics_day_wed)
    DayOfWeek.THURSDAY -> stringResource(R.string.usage_analytics_day_thu)
    DayOfWeek.FRIDAY -> stringResource(R.string.usage_analytics_day_fri)
    DayOfWeek.SATURDAY -> stringResource(R.string.usage_analytics_day_sat)
    DayOfWeek.SUNDAY -> stringResource(R.string.usage_analytics_day_sun)
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
        weekdayAverages = DayOfWeek.entries.mapIndexed { index, dayOfWeek ->
            WeekdayUsageAverage(
                dayOfWeek = dayOfWeek,
                averageMillis = TimeUnit.MINUTES.toMillis(((index + 1) * 5).toLong()),
            )
        },
    )
}
