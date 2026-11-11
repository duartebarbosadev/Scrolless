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
package com.scrolless.app.feature.home.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.scrolless.app.core.model.BlockedHours
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.designsystem.util.formatMinutes
import com.scrolless.app.designsystem.util.rememberHapticHelper
import com.scrolless.app.feature.home.R
import kotlin.math.roundToInt

private const val MIN_BREAK_MINUTES = 30
private const val MAX_BREAK_MINUTES = 600
private const val BREAK_STEP_MINUTES = 30

private const val ALLOWANCE_STEP_MINUTES = 1
private const val MIN_ALLOWANCE_MINUTES = 1
private const val MAX_ALLOWANCE_MINUTES = 30

private const val BLOCKED_HOURS_PER_ROW = 6

@Composable
fun IntervalTimerDialog(
    initialBreakMillis: Long,
    initialAllowanceMillis: Long,
    initialBlockedHours: BlockedHours,
    onConfirm: (breakMillis: Long, allowanceMillis: Long, blockedHours: BlockedHours) -> Unit,
    onDismiss: () -> Unit,
) {
    val hapticHelper = rememberHapticHelper()
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
    var blockedHoursMask by rememberSaveable { mutableIntStateOf(initialBlockedHours.mask) }
    val blockedHours = BlockedHours(blockedHoursMask)

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
                    .verticalScroll(rememberScrollState())
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
                            val newValue = snapToStepValue(
                                value = rawValue,
                                step = BREAK_STEP_MINUTES,
                                min = MIN_BREAK_MINUTES,
                                max = MAX_BREAK_MINUTES,
                            )
                            if (newValue != breakMinutes) {
                                hapticHelper.playTick()
                            }
                            breakMinutes = newValue
                        },
                        onValueChangeFinished = {
                            hapticHelper.playConfirm()
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
                            val newValue = snapToStepValue(
                                value = rawValue,
                                step = ALLOWANCE_STEP_MINUTES,
                                min = MIN_ALLOWANCE_MINUTES,
                                max = MAX_ALLOWANCE_MINUTES,
                            )
                            if (newValue != allowanceMinutes) {
                                hapticHelper.playTick()
                            }
                            allowanceMinutes = newValue
                        },
                        onValueChangeFinished = {
                            hapticHelper.playConfirm()
                        },
                        valueRange = MIN_ALLOWANCE_MINUTES.toFloat()..MAX_ALLOWANCE_MINUTES.toFloat(),
                        steps = (MAX_ALLOWANCE_MINUTES - MIN_ALLOWANCE_MINUTES) - 1,
                    )
                }

                Spacer(Modifier.height(20.dp))

                BlockedHoursSection(
                    blockedHours = blockedHours,
                    onToggleHour = { hour ->
                        hapticHelper.playTick()
                        blockedHoursMask = blockedHours.toggle(hour).mask
                    },
                )

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
                    TextButton(
                        onClick = {
                            onDismiss()
                        },
                    ) {
                        Text(text = stringResource(R.string.interval_timer_dialog_cancel))
                    }
                    Button(
                        onClick = {
                            hapticHelper.playConfirm()
                            onConfirm(
                                breakMinutes.minutesToMillis(),
                                allowanceMinutes.minutesToMillis(),
                                blockedHours,
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

/** A grid of the 24 hours of the day, where each selected hour blocks shorts completely. */
@Composable
private fun BlockedHoursSection(blockedHours: BlockedHours, onToggleHour: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.interval_timer_dialog_blocked_hours_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        (0 until BlockedHours.HOURS_PER_DAY).chunked(BLOCKED_HOURS_PER_ROW).forEach { rowHours ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowHours.forEach { hour ->
                    BlockedHourCell(
                        hour = hour,
                        isBlocked = blockedHours.isBlocked(hour),
                        onToggle = { onToggleHour(hour) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.interval_timer_dialog_blocked_hours_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BlockedHourCell(hour: Int, isBlocked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .toggleable(value = isBlocked, role = Role.Checkbox, onValueChange = { onToggle() }),
        shape = RoundedCornerShape(10.dp),
        color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isBlocked) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "%02d".format(hour),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.Normal,
            )
        }
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
            initialBlockedHours = BlockedHours.of(22, 23, 0, 1, 2, 3, 4, 5),
            onConfirm = { _, _, _ -> },
            onDismiss = { },
        )
    }
}
