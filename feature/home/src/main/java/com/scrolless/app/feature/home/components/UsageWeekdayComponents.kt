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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolless.app.core.model.usage.WeekdayUsageAverage
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.theme.progressbar_green_use
import com.scrolless.app.designsystem.theme.progressbar_orange_use
import com.scrolless.app.designsystem.theme.progressbar_red_use
import com.scrolless.app.feature.home.R
import com.scrolless.app.feature.home.UsageAveragePeriod
import java.time.DayOfWeek
import java.util.concurrent.TimeUnit

@Composable
fun WeekdayAverageSection(
    weekdayAverages: List<WeekdayUsageAverage>,
    selectedPeriod: UsageAveragePeriod = UsageAveragePeriod.LAST_WEEK,
    onPeriodSelected: (UsageAveragePeriod) -> Unit = {},
) {
    val maxValue = weekdayAverages.maxOfOrNull { it.averageMillis } ?: 0L
    var menuExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.usage_analytics_average_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { menuExpanded = true }
                        .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(selectedPeriod.labelResId),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    UsageAveragePeriod.entries.forEach { period ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(period.labelResId),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (period == selectedPeriod) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (period == selectedPeriod) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            },
                            onClick = {
                                onPeriodSelected(period)
                                menuExpanded = false
                            },
                        )
                    }
                }
            }
        }
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

fun usageIntensityColor(fraction: Float): Color = when {
    fraction >= 0.72f -> progressbar_red_use.copy(alpha = 0.82f)
    fraction >= 0.38f -> progressbar_orange_use.copy(alpha = 0.82f)
    else -> progressbar_green_use.copy(alpha = 0.82f)
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

@Preview(name = "Weekday Average Section")
@Composable
private fun PreviewWeekdayAverageSection() {
    ScrollessTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                WeekdayAverageSection(weekdayAverages = previewWeekdayAverages())
            }
        }
    }
}

@Preview(name = "Weekday Average Section — Period Dropdown Open")
@Composable
private fun PreviewWeekdayAverageSectionDropdownOpen() {
    ScrollessTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(16.dp)) {
                WeekdayAverageSection(weekdayAverages = previewWeekdayAverages())
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = {},
                ) {
                    UsageAveragePeriod.entries.forEach { period ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(period.labelResId),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (period == UsageAveragePeriod.LAST_WEEK) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (period == UsageAveragePeriod.LAST_WEEK) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            },
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

private fun previewWeekdayAverages(): List<WeekdayUsageAverage> = DayOfWeek.entries.mapIndexed { index, dayOfWeek ->
    WeekdayUsageAverage(
        dayOfWeek = dayOfWeek,
        averageMillis = TimeUnit.MINUTES.toMillis(((index + 1) * 8).toLong()),
    )
}
