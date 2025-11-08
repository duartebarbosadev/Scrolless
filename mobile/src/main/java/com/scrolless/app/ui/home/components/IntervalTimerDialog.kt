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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.scrolless.app.R
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.ui.tooling.DevicePreviews
import com.scrolless.app.ui.utils.formatMinutes
import kotlin.math.roundToInt

private const val MIN_BREAK_MINUTES = 30
private const val MAX_BREAK_MINUTES = 180
private const val BREAK_STEP_MINUTES = 15

private const val ALLOWANCE_STEP_MINUTES = 1
private const val MIN_ALLOWANCE_MINUTES = 1
private const val MAX_ALLOWANCE_MINUTES = 30

@Composable
fun IntervalTimerDialog(
    initialBreakMillis: Long,
    initialAllowanceMillis: Long,
    onConfirm: (breakMillis: Long, allowanceMillis: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var breakMinutes by rememberSaveable {
        mutableIntStateOf(
            initialBreakMillis.millisToRoundedMinutes(
                step = BREAK_STEP_MINUTES,
                min = MIN_BREAK_MINUTES,
                max = MAX_BREAK_MINUTES,
            ),
        )
    }
    var allowanceMinutes by rememberSaveable {
        mutableIntStateOf(
            initialAllowanceMillis.millisToRoundedMinutes(
                step = ALLOWANCE_STEP_MINUTES,
                min = MIN_ALLOWANCE_MINUTES,
                max = MAX_ALLOWANCE_MINUTES,
            ),
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.interval_timer_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.interval_timer_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                IntervalSettingSection(
                    label = stringResource(R.string.interval_timer_dialog_break_label),
                    description = stringResource(R.string.interval_timer_dialog_break_description),
                    formattedValue = breakMinutes.formatMinutes(),
                ) {
                    Slider(
                        value = breakMinutes.toFloat(),
                        onValueChange = { rawValue ->
                            breakMinutes = snapToStepValue(
                                value = rawValue,
                                step = BREAK_STEP_MINUTES,
                                min = MIN_BREAK_MINUTES,
                                max = MAX_BREAK_MINUTES,
                            )
                        },
                        valueRange = MIN_BREAK_MINUTES.toFloat()..MAX_BREAK_MINUTES.toFloat(),
                        steps = ((MAX_BREAK_MINUTES - MIN_BREAK_MINUTES) / BREAK_STEP_MINUTES) - 1,
                    )
                }

                Spacer(Modifier.height(20.dp))

                IntervalSettingSection(
                    label = stringResource(R.string.interval_timer_dialog_allowance_label),
                    description = stringResource(R.string.interval_timer_dialog_allowance_description),
                    formattedValue = allowanceMinutes.formatMinutes(),
                ) {
                    Slider(
                        value = allowanceMinutes.toFloat(),
                        onValueChange = { rawValue ->
                            allowanceMinutes = snapToStepValue(
                                value = rawValue,
                                step = ALLOWANCE_STEP_MINUTES,
                                min = MIN_ALLOWANCE_MINUTES,
                                max = MAX_ALLOWANCE_MINUTES,
                            )
                        },
                        valueRange = MIN_ALLOWANCE_MINUTES.toFloat()..MAX_ALLOWANCE_MINUTES.toFloat(),
                        steps = (MAX_ALLOWANCE_MINUTES - MIN_ALLOWANCE_MINUTES) - 1,
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(
                        R.string.interval_timer_dialog_summary,
                        allowanceMinutes.formatMinutes(),
                        breakMinutes.formatMinutes(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.interval_timer_dialog_cancel))
                    }
                    Button(
                        onClick = {
                            onConfirm(
                                breakMinutes.minutesToMillis(),
                                allowanceMinutes.minutesToMillis(),
                            )
                        },
                    ) {
                        Text(text = stringResource(R.string.interval_timer_dialog_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalSettingSection(label: String, description: String, formattedValue: String, slider: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formattedValue,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        slider()
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Long.millisToRoundedMinutes(step: Int, min: Int, max: Int): Int {
    if (this <= 0L) return min
    val minutes = (this / MINUTE_IN_MILLIS).toInt()
    return snapToStepValue(minutes.toFloat(), step, min, max)
}

private fun Int.minutesToMillis(): Long = this * MINUTE_IN_MILLIS

private fun snapToStepValue(value: Float, step: Int, min: Int, max: Int): Int {
    val coerced = value.coerceIn(min.toFloat(), max.toFloat())
    val stepsFromMin = ((coerced - min) / step).roundToInt()
    return (min + stepsFromMin * step).coerceIn(min, max)
}

private const val MINUTE_IN_MILLIS = 60_000L

@DevicePreviews
@Composable
fun IntervalTimerPreview() {
    ScrollessTheme(darkTheme = true) {
        IntervalTimerDialog(
            initialBreakMillis = 1000L,
            initialAllowanceMillis = 1000L,
            onConfirm = { _, _ -> },
            onDismiss = { },
        )
    }
}
