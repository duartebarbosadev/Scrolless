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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Text that automatically shrink-wraps its font size to avoid overflowing the given bounds.
 *
 * The composable starts at [maxFontSize] (or the font size coming from [style]) and repeatedly
 * decreases the size by [step] while the layout still overflows. Once the string fits or reaches
 * [minFontSize], the final size is drawn. Because the algorithm relies on successive layout passes,
 * prefer it for compact labels rather than large paragraphs.
 *
 * @param text text content to display.
 * @param modifier optional [Modifier] for styling/placement.
 * @param style base [TextStyle]; defaults to [TextStyle.Default].
 * @param color explicit text color. Defaults to using [style]'s color.
 * @param textAlign horizontal alignment for the text.
 * @param maxLines maximum line count permitted.
 * @param overflow overflow behavior once [maxLines] is reached.
 * @param minFontSize lower bound for the auto-scaling process.
 * @param maxFontSize upper bound for the auto-scaling process.
 * @param step decrement applied to the font size whenever overflow occurs.
 */
@Composable
fun AutoResizingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    minFontSize: TextUnit = 12.sp,
    maxFontSize: TextUnit = if (style.fontSize.isSpecified) style.fontSize else 16.sp,
    step: TextUnit = 1.sp,
) {
    val resolvedColor = if (color == Color.Unspecified) style.color else color
    val resolvedMaxFontSize = when {
        maxFontSize.isSpecified -> maxFontSize
        style.fontSize.isSpecified -> style.fontSize
        else -> 16.sp
    }
    val resolvedMinFontSize = if (minFontSize.isSpecified) minFontSize else 12.sp
    val resolvedStep = if (step.isSpecified && step.value > 0f) step else 1.sp

    var readyToDraw by remember(text, resolvedMaxFontSize, resolvedMinFontSize, maxLines) { mutableStateOf(false) }
    var currentFontSize by remember(text, resolvedMaxFontSize, resolvedMinFontSize, maxLines) { mutableStateOf(resolvedMaxFontSize.value) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        style = style.copy(
            color = resolvedColor,
            fontSize = currentFontSize.sp,
        ),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { result ->
            val overflowed = result.didOverflowWidth || result.didOverflowHeight
            val minValue = resolvedMinFontSize.value
            val stepValue = resolvedStep.value.coerceAtLeast(0.5f)
            if (overflowed && currentFontSize > minValue) {
                val nextFontSize = (currentFontSize - stepValue).coerceAtLeast(minValue)
                if (nextFontSize == currentFontSize) {
                    readyToDraw = true
                } else {
                    currentFontSize = nextFontSize
                }
            } else {
                readyToDraw = true
            }
        },
    )
}
