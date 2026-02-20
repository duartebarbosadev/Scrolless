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
import kotlin.math.max
import kotlin.math.min

/**
 * Immutable UI model for a single usage segment in the circular indicator.
 *
 * Each entry maps to one arc fragment rendered in [SegmentedCircularProgressIndicator].
 *
 * @param segmentName Segment label shown in the legend (for example "Reels")
 * @param usageMillis Segment duration in milliseconds
 * @param color Segment color in the ring and legend
 */
@Immutable
data class ProgressBarSegment(val segmentName: String, val usageMillis: Long, val color: Color)

/**
 * Internal representation for animated segment drawing.
 */
@Immutable
private data class AnimatedSegment(val startAngle: Float, val sweepAngle: Float, val color: Color)

/**
 * Data for legend display items.
 */
@Immutable
data class LegendItem(val legendName: String, val formattedTime: String, val color: Color)

private const val VISIBLE_GAP_DEGREES = 3f
private const val MIN_VISIBLE_SWEEP = 1.2f
private const val MIN_TOTAL_SWEEP_DEGREES = 1f
private const val GAP_SHRINK_START_SEGMENT_COUNT = 36
private const val GAP_SHRINK_FULL_SEGMENT_COUNT = 72
private const val GAP_SHRINK_START_AVERAGE_SWEEP_DEGREES = 5f
private const val GAP_SHRINK_FULL_AVERAGE_SWEEP_DEGREES = 2.2f
private const val DENSE_GAP_DEGREES = 1.2f
private const val START_ANGLE = -90f

/**
 * Circular progress indicator that renders one colored arc per usage segment.
 *
 * The total painted sweep is derived from [progressFraction], so the ring only appears full at 1f.
 * Segment spacing uses adaptive gap compression: it starts with the base visual gap and shrinks
 * progressively as segment density increases.
 *
 * @param modifier Modifier for the canvas
 * @param segments Segment list to render
 * @param progressFraction Total progress in [0f, 1f]
 * @param strokeWidth Width of segment and track strokes
 * @param trackColor Color used for the background track ring
 */
@Composable
fun SegmentedCircularProgressIndicator(
    modifier: Modifier = Modifier,
    segments: List<ProgressBarSegment>,
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
        val topLeft = Offset(
            x = (size.width - diameter) / 2,
            y = (size.height - diameter) / 2,
        )
        val arcSize = Size(diameter, diameter)
        val totalSweepDegrees = 360f * animatedProgressFraction
        val validSegmentCount = segments.count { it.usageMillis > 0L }
        val averageSweepDegrees = if (validSegmentCount > 0) {
            totalSweepDegrees / validSegmentCount
        } else {
            totalSweepDegrees
        }
        val initialGapDegrees = (VISIBLE_GAP_DEGREES + (2f * capAngleDegrees)).coerceAtMost(24f)
        val countCrowdingFactor = normalizedRange(
            value = validSegmentCount.toFloat(),
            start = GAP_SHRINK_START_SEGMENT_COUNT.toFloat(),
            end = GAP_SHRINK_FULL_SEGMENT_COUNT.toFloat(),
        )
        val sweepCrowdingFactor = normalizedRange(
            value = averageSweepDegrees,
            start = GAP_SHRINK_START_AVERAGE_SWEEP_DEGREES,
            end = GAP_SHRINK_FULL_AVERAGE_SWEEP_DEGREES,
            descending = true,
        )
        val crowdingFactor = max(countCrowdingFactor, sweepCrowdingFactor)
        val preferredGapDegrees = lerp(initialGapDegrees, DENSE_GAP_DEGREES, crowdingFactor)
        val animatedSegments = calculateSegments(
            appUsageData = segments,
            totalSweepDegrees = totalSweepDegrees,
            gapDegrees = preferredGapDegrees,
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
 * Normalizes [value] to the [0f, 1f] range between [start] and [end].
 *
 * If [descending] is true, normalization is inverted so lower values map to higher factors.
 */
private fun normalizedRange(value: Float, start: Float, end: Float, descending: Boolean = false): Float {
    if (start == end) return 0f
    val normalized = if (!descending) {
        (value - start) / (end - start)
    } else {
        (start - value) / (start - end)
    }
    return normalized.coerceIn(0f, 1f)
}

/** Linear interpolation helper for scalar float values. */
private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

/**
 * Converts app usage data to animated segments with proper angles.
 * Starts from -90 degrees (top of circle) and proceeds clockwise.
 */
private fun calculateSegments(appUsageData: List<ProgressBarSegment>, totalSweepDegrees: Float, gapDegrees: Float): List<AnimatedSegment> {
    val validSegments = appUsageData.filter { it.usageMillis > 0L }
    if (validSegments.isEmpty() || totalSweepDegrees <= 0f) return emptyList()

    val totalUsage = validSegments.sumOf { it.usageMillis }.toDouble()

    // Smoothly scale the used degrees so that at 100% progress, it leaves exactly one gap
    // empty between the end of the last segment and the start of the first.
    val progress = (totalSweepDegrees / 360f).coerceIn(0f, 1f)
    val maxVisualSweep = if (validSegments.size > 1) 360f - gapDegrees else 360f
    val usedDegrees = maxVisualSweep * progress

    val gapCount = (validSegments.size - 1).coerceAtLeast(0)

    val effectiveGapDegrees = if (gapCount > 0) {
        min(gapDegrees, ((usedDegrees - MIN_TOTAL_SWEEP_DEGREES).coerceAtLeast(0f)) / gapCount)
    } else {
        0f
    }
    
    val totalGapDegrees = (gapCount * effectiveGapDegrees).coerceAtMost(usedDegrees)
    val availableDegrees = (usedDegrees - totalGapDegrees).coerceAtLeast(0f)
    if (availableDegrees <= 0f) return emptyList()

    val minVisibleSweep = min(MIN_VISIBLE_SWEEP.toDouble(), availableDegrees.toDouble() / validSegments.size)
    val sweeps = FloatArray(validSegments.size)
    var remainingDegrees = availableDegrees.toDouble()
    var remainingUsage = totalUsage

    val unassigned = validSegments.indices.toMutableList()
    var changed = true

    while (changed && unassigned.isNotEmpty()) {
        changed = false
        val iterator = unassigned.iterator()
        while (iterator.hasNext()) {
            val i = iterator.next()
            val usage = validSegments[i].usageMillis.toDouble()
            val proposed = if (remainingUsage > 0.0) remainingDegrees * (usage / remainingUsage) else 0.0
            if (proposed < minVisibleSweep) {
                sweeps[i] = minVisibleSweep.toFloat()
                remainingDegrees -= minVisibleSweep
                remainingUsage -= usage
                iterator.remove()
                changed = true
            }
        }
    }

    for (i in unassigned) {
        val usage = validSegments[i].usageMillis.toDouble()
        sweeps[i] = (if (remainingUsage > 0.0) remainingDegrees * (usage / remainingUsage) else 0.0).toFloat()
    }

    var currentAngle = START_ANGLE
    return validSegments.mapIndexed { index, data ->
        val sweepAngle = sweeps[index].coerceAtLeast(0f)
        val segment = AnimatedSegment(
            startAngle = currentAngle,
            sweepAngle = sweepAngle,
            color = data.color,
        )
        
        val shouldAddGapAfter = index < validSegments.lastIndex
        currentAngle += sweepAngle + (if (shouldAddGapAfter) effectiveGapDegrees else 0f)
        
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
            text = "${item.legendName} (${item.formattedTime})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}
