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
package com.scrolless.app.feature.home

import androidx.compose.ui.graphics.Color
import com.scrolless.app.designsystem.component.ProgressBarSegment
import com.scrolless.app.feature.home.components.buildLegendItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCardTest {

    @Test
    fun `buildLegendItems ignores segments with duration under 1 second`() {
        val segments = listOf(
            ProgressBarSegment(segmentName = "TikTok", usageMillis = 50L, color = Color.Black),
            ProgressBarSegment(segmentName = "YouTube", usageMillis = 500L, color = Color.Red),
            ProgressBarSegment(segmentName = "Instagram", usageMillis = 999L, color = Color.Magenta),
        )

        val legendItems = buildLegendItems(segments)

        assertTrue("Expected no legend items for sub-second usage", legendItems.isEmpty())
    }

    @Test
    fun `buildLegendItems includes segments with duration of 1 second or more`() {
        val segments = listOf(
            ProgressBarSegment(segmentName = "TikTok", usageMillis = 50L, color = Color.Black),
            ProgressBarSegment(segmentName = "YouTube", usageMillis = 5_000L, color = Color.Red),
        )

        val legendItems = buildLegendItems(segments)

        assertEquals(1, legendItems.size)
        assertEquals("YouTube", legendItems.first().legendName)
        assertEquals("5s", legendItems.first().formattedTime)
    }

    @Test
    fun `buildLegendItems aggregates multiple segments for the same app and filters if total under 1 second`() {
        val subSecondSegments = listOf(
            ProgressBarSegment(segmentName = "TikTok", usageMillis = 200L, color = Color.Black),
            ProgressBarSegment(segmentName = "TikTok", usageMillis = 300L, color = Color.Black),
        )
        val aboveSecondSegments = listOf(
            ProgressBarSegment(segmentName = "TikTok", usageMillis = 600L, color = Color.Black),
            ProgressBarSegment(segmentName = "TikTok", usageMillis = 600L, color = Color.Black),
        )

        val subSecondLegend = buildLegendItems(subSecondSegments)
        val aboveSecondLegend = buildLegendItems(aboveSecondSegments)

        assertTrue("Expected TikTok to be omitted when total is under 1 second", subSecondLegend.isEmpty())
        assertEquals(1, aboveSecondLegend.size)
        assertEquals("TikTok", aboveSecondLegend.first().legendName)
        assertEquals("1s", aboveSecondLegend.first().formattedTime)
    }
}
