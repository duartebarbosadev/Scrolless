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
package com.scrolless.app.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min

/**
 * Represents usage data for a single app segment.
 *
 * @param appName Display name for the app (e.g., "Reels", "TikTok", "Shorts")
 * @param usageMillis Usage time in milliseconds
 * @param color Primary color for this segment
 */
@Immutable
data class AppUsageSegment(val appName: String, val usageMillis: Long, val color: Color)

/**
 * Internal representation for animated segment drawing.
 */
@Immutable
private data class AnimatedSegment(val startAngle: Float, val sweepAngle: Float, val color: Color)

/**
 * Data for legend display items.
 */
@Immutable
data class LegendItem(val appName: String, val formattedTime: String, val color: Color)

private const val VISIBLE_GAP_DEGREES = 3f
private const val MIN_VISIBLE_SWEEP = 5f
private const val START_ANGLE = -90f

/**
 * A circular progress indicator with multiple colored segments representing per-app usage.
 *
 * @param modifier Modifier for the canvas
 * @param segments List of app usage segments to display
 * @param progressFraction Total progress to render, from 0f..1f
 * @param strokeWidth Width of the progress arc stroke
 * @param trackColor Color of the background track
 */
@Composable
fun SegmentedCircularProgressIndicator(
    modifier: Modifier = Modifier,
    segments: List<AppUsageSegment>,
    progressFraction: Float = 1f,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.primary,
) {
    val isPreview = LocalInspectionMode.current

    val clampedProgress = progressFraction.coerceIn(0f, 1f)
    val animatedProgressFraction by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = if (isPreview) 0 else 800),
        label = "totalProgress",
    )

    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val diameter = (min(size.width, size.height) - strokeWidthPx).coerceAtLeast(0f)
        val radius = (diameter / 2f).coerceAtLeast(0.001f)
        val capAngleDegrees = ((strokeWidthPx / 2f) / radius) * (180f / PI.toFloat())
        val effectiveGapDegrees = (VISIBLE_GAP_DEGREES + (2f * capAngleDegrees)).coerceAtMost(24f)
        val topLeft = Offset(
            x = (size.width - diameter) / 2,
            y = (size.height - diameter) / 2,
        )
        val arcSize = Size(diameter, diameter)
        val validSegmentCount = segments.count { it.usageMillis > 0L }
        val gapCount = (validSegmentCount - 1).coerceAtLeast(0)
        val totalGapDegrees = (gapCount * effectiveGapDegrees).coerceAtMost(360f)
        val maxArcDegrees = (360f - totalGapDegrees).coerceAtLeast(0f)
        val animatedSegments = calculateSegments(
            appUsageData = segments,
            totalSweepDegrees = totalGapDegrees + (maxArcDegrees * animatedProgressFraction),
            gapDegrees = effectiveGapDegrees,
        )

        // Draw track (background circle)
        drawArc(
            color = trackColor,
            startAngle = START_ANGLE,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
        )

        // Draw each segment
        animatedSegments.forEach { segment ->
            if (segment.sweepAngle > 0f) {
                drawArc(
                    color = segment.color,
                    startAngle = segment.startAngle,
                    sweepAngle = segment.sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/**
 * Converts app usage data to animated segments with proper angles.
 * Starts from -90 degrees (top of circle) and proceeds clockwise.
 */
private fun calculateSegments(appUsageData: List<AppUsageSegment>, totalSweepDegrees: Float, gapDegrees: Float): List<AnimatedSegment> {
    val totalUsage = appUsageData.sumOf { it.usageMillis }
    if (totalUsage == 0L) return emptyList()

    val validSegments = appUsageData.filter { it.usageMillis > 0L }
    val usedDegrees = totalSweepDegrees.coerceIn(0f, 360f)
    if (usedDegrees <= 0f) return emptyList()

    val gapCount = (validSegments.size - 1).coerceAtLeast(0)
    val totalGapDegrees = (gapCount * gapDegrees).coerceAtMost(usedDegrees)
    val availableDegrees = (usedDegrees - totalGapDegrees).coerceAtLeast(0f)
    if (availableDegrees <= 0f) return emptyList()

    var currentAngle = START_ANGLE
    val minVisibleSweep = min(MIN_VISIBLE_SWEEP, availableDegrees / validSegments.size)

    val weights = validSegments.map { it.usageMillis.toDouble() / totalUsage.toDouble() }
    val sweeps = DoubleArray(validSegments.size)
    var remainingDegrees = availableDegrees.toDouble()
    var remainingWeightSum = weights.sum()
    val remainingIndices = validSegments.indices.toMutableSet()

    while (remainingIndices.isNotEmpty()) {
        var anyPinnedToMinimum = false
        val indicesSnapshot = remainingIndices.toList()
        indicesSnapshot.forEach { index ->
            val weight = weights[index]
            val proposed = if (remainingWeightSum > 0.0) remainingDegrees * (weight / remainingWeightSum) else 0.0
            if (proposed < minVisibleSweep.toDouble()) {
                sweeps[index] = minVisibleSweep.toDouble()
                remainingDegrees -= minVisibleSweep.toDouble()
                remainingWeightSum -= weight
                remainingIndices.remove(index)
                anyPinnedToMinimum = true
            }
        }

        if (!anyPinnedToMinimum) break
        if (remainingDegrees <= 0.0) break
        if (remainingWeightSum <= 0.0) break
    }

    remainingDegrees = remainingDegrees.coerceAtLeast(0.0)
    remainingIndices.forEach { index ->
        val weight = weights[index]
        sweeps[index] = if (remainingWeightSum > 0.0) remainingDegrees * (weight / remainingWeightSum) else 0.0
    }

    return validSegments.mapIndexed { index, data ->
        val sweepAngle = sweeps[index].toFloat().coerceAtLeast(0f)

        val segment = AnimatedSegment(
            startAngle = currentAngle,
            sweepAngle = sweepAngle,
            color = data.color,
        )

        val shouldAddGapAfter = index < validSegments.lastIndex
        currentAngle += sweepAngle + (if (shouldAddGapAfter) gapDegrees else 0f)

        segment
    }
}

/**
 * A horizontal legend showing app usage items with colored dots.
 *
 * @param items List of legend items to display
 * @param modifier Modifier for the row
 */
@Composable
fun AppUsageLegend(items: List<LegendItem>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            LegendEntry(item = item)
        }
    }
}

@Composable
private fun LegendEntry(item: LegendItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Color indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = item.color, shape = CircleShape),
        )

        // App name and time
        Text(
            text = "${item.appName} (${item.formattedTime})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}
