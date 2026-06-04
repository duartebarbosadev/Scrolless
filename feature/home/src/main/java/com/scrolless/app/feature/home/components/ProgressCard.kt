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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.designsystem.component.AppUsageLegend
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.designsystem.component.LegendItem
import com.scrolless.app.designsystem.component.ProgressBarSegment
import com.scrolless.app.designsystem.component.SegmentedCircularProgressIndicator
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.theme.progressbar_red_use
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.designsystem.util.formatTime
import com.scrolless.app.feature.home.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ProgressCard(
    blockOption: BlockOption,
    progress: Int,
    currentUsage: Long,
    intervalUsage: Long,
    timeLimit: Long,
    intervalLength: Long,
    intervalWindowStart: Long,
    modifier: Modifier = Modifier,
    listSessionSegments: List<SessionSegment> = emptyList(),
    onClick: () -> Unit = {},
) {
    val clampedProgress = progress.coerceIn(0, 100)

    val isIntervalMode = blockOption == BlockOption.IntervalTimer
    val intervalAllowanceConfigured = isIntervalMode && timeLimit > 0L
    val intervalRemainingMillis = if (isIntervalMode) {
        rememberIntervalRemainingTime(
            isRunning = intervalAllowanceConfigured && intervalLength > 0L && intervalWindowStart > 0L,
            intervalLength = intervalLength,
            windowStart = intervalWindowStart,
        )
    } else {
        0L
    }
    val intervalResetReady =
        intervalAllowanceConfigured &&
            intervalLength > 0L &&
            intervalWindowStart > 0L &&
            intervalRemainingMillis <= 1_000L
    val displayIntervalUsage = if (intervalResetReady) 0L else intervalUsage
    val displayProgress = if (intervalResetReady) 0 else clampedProgress

    val primaryText = when {
        isIntervalMode -> displayIntervalUsage.formatTime()
        else -> currentUsage.formatTime()
    }
    val limitChipText = when {
        isIntervalMode && intervalAllowanceConfigured -> timeLimit.formatTime()
        blockOption == BlockOption.DailyLimit && timeLimit > 0L -> timeLimit.formatTime()
        else -> null
    }

    val resetText = if (isIntervalMode) {
        when {
            !intervalAllowanceConfigured -> null

            intervalLength <= 0L || intervalWindowStart <= 0L -> null

            intervalRemainingMillis <= 1_000L -> stringResource(R.string.interval_timer_next_reset_ready)

            else -> stringResource(
                R.string.interval_timer_next_reset_in,
                intervalRemainingMillis.formatTime(),
            )
        }
    } else {
        null
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // Per-app usage data for the segmented progress indicator
    val progressBarSegments =
        remember(listSessionSegments, currentUsage, context, configuration) {
            buildProgressBarSegments(
                sessionSegments = listSessionSegments,
                currentUsage = currentUsage,
                context = context,
            )
        }
    val legendItems = remember(progressBarSegments) { buildLegendItems(progressBarSegments) }

    val segmentProgressFraction = when {
        blockOption == BlockOption.DailyLimit && timeLimit > 0L -> displayProgress / 100f
        blockOption == BlockOption.IntervalTimer && intervalAllowanceConfigured -> displayProgress / 100f
        progressBarSegments.isNotEmpty() -> 1f
        else -> 0f
    }

    val isLimitReached = when (blockOption) {
        BlockOption.DailyLimit -> timeLimit in 1..currentUsage
        BlockOption.IntervalTimer -> timeLimit in 1..intervalUsage && !intervalResetReady
        else -> false
    }

    // Card Exceeded Bounce animation
    val cardScale = remember { Animatable(1f) }
    LaunchedEffect(isLimitReached) {
        if (isLimitReached) {
            // Quick overshoot bounce
            cardScale.animateTo(
                targetValue = 1.08f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                ),
            )
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                ),
            )
        } else if (cardScale.value != 1f) {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    val limitChipBackground by animateColorAsState(
        targetValue = if (isLimitReached) {
            progressbar_red_use.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
        },
        animationSpec = tween(durationMillis = 600),
        label = "limitChipBackground",
    )
    val limitChipBorderColor by animateColorAsState(
        targetValue = if (isLimitReached) {
            progressbar_red_use.copy(alpha = 0.7f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 600),
        label = "limitChipBorderColor",
    )
    val limitChipTextColor by animateColorAsState(
        targetValue = if (isLimitReached) {
            progressbar_red_use
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
        },
        animationSpec = tween(durationMillis = 600),
        label = "limitChipTextColor",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = cardScale.value,
                    scaleY = cardScale.value,
                )
                .size(220.dp)
                .padding(16.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(96.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {

                SegmentedCircularProgressIndicator(
                    modifier = Modifier.size(180.dp),
                    segments = progressBarSegments,
                    progressFraction = segmentProgressFraction,
                    strokeWidth = 8.dp,
                    trackColor = Color.Transparent,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    AutoResizingText(
                        text = primaryText,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        minFontSize = 16.sp,
                    )
                    if (limitChipText != null) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(limitChipBackground)
                                .border(1.dp, limitChipBorderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.limit_chip, limitChipText),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = limitChipTextColor,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (resetText != null) {
                        Spacer(Modifier.height(6.dp))
                        AutoResizingText(
                            text = resetText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            minFontSize = 10.sp,
                        )
                    }
                }
            }
        }

        // Legend showing per-app usage
        AppUsageLegend(
            items = legendItems,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun rememberIntervalRemainingTime(isRunning: Boolean, intervalLength: Long, windowStart: Long): Long {
    val isInspectionMode = LocalInspectionMode.current

    fun calculateRemaining(): Long {
        if (intervalLength <= 0L || windowStart <= 0L) return 0L
        val now = System.currentTimeMillis()
        val elapsed = now - windowStart
        if (elapsed < 0L) return intervalLength
        val remaining = intervalLength - elapsed
        return remaining.coerceAtLeast(0L)
    }

    var remaining by remember(isRunning, intervalLength, windowStart) {
        mutableLongStateOf(calculateRemaining())
    }

    LaunchedEffect(isRunning, intervalLength, windowStart, isInspectionMode) {
        if (!isRunning || intervalLength <= 0L || windowStart <= 0L || isInspectionMode) {
            remaining = calculateRemaining()
        } else {
            while (isActive) {
                val nextRemaining = calculateRemaining()
                remaining = nextRemaining
                if (nextRemaining <= 0L) break
                delay(1_000L)
            }
        }
    }

    return remaining
}

private fun buildProgressBarSegments(
    sessionSegments: List<SessionSegment>,
    currentUsage: Long,
    context: Context,
): List<ProgressBarSegment> {
    val totalSegmentMillis = sessionSegments.sumOf { it.durationMillis.coerceAtLeast(0L) }
    val cappedTotalUsage = currentUsage.coerceAtLeast(0L)
    val scale = if (totalSegmentMillis > 0L && cappedTotalUsage in 1..<totalSegmentMillis) {
        cappedTotalUsage.toDouble() / totalSegmentMillis.toDouble()
    } else {
        1.0
    }

    return sessionSegments.mapNotNull { segment ->
        val rawUsageMillis = segment.durationMillis.coerceAtLeast(0L)
        if (rawUsageMillis <= 0L) {
            return@mapNotNull null
        }
        val usageMillis = (rawUsageMillis * scale).toLong().coerceAtLeast(1L)

        ProgressBarSegment(
            segmentName = segment.app.displayName(context),
            usageMillis = usageMillis,
            color = segment.app.analyticsColor(),
        )
    }
}

private fun buildLegendItems(progressBarSegments: List<ProgressBarSegment>): List<LegendItem> =
    progressBarSegments.groupBy { it.segmentName }.mapNotNull { (segmentName, segments) ->
        val totalMillis = segments.sumOf { it.usageMillis.coerceAtLeast(0L) }
        if (totalMillis <= 0L) {
            return@mapNotNull null
        }
        totalMillis to LegendItem(
            legendName = segmentName,
            formattedTime = totalMillis.formatTime(),
            color = segments.first().color,
        )
    }.sortedByDescending { it.first }
        .map { it.second }

@DevicePreviews
@Composable
fun ProgressCardPreview() {
    ScrollessTheme(darkTheme = true) {
        Surface {
            ProgressCard(
                blockOption = BlockOption.NothingSelected,
                progress = 0,
                currentUsage = 3600000L,
                intervalUsage = 0L,
                timeLimit = 0L,
                intervalLength = 0L,
                intervalWindowStart = 0L,
                listSessionSegments = listOf(
                    SessionSegment(BlockableApp.TIKTOK, 1800000L, java.time.LocalDateTime.now()),
                    SessionSegment(BlockableApp.REELS, 1200000L, java.time.LocalDateTime.now()),
                    SessionSegment(BlockableApp.FACEBOOK, 600000L, java.time.LocalDateTime.now()),
                ),
            )
        }
    }
}
