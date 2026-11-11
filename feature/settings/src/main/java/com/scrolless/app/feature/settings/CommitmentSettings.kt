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
package com.scrolless.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.scrolless.app.core.model.COMMITMENT_DELAY_OPTIONS_MILLIS
import com.scrolless.app.core.model.CommitmentMode
import com.scrolless.app.core.model.CommitmentState
import com.scrolless.app.designsystem.component.PinDialog
import com.scrolless.app.designsystem.util.formatTimeWithDay
import com.scrolless.app.designsystem.util.rememberHapticHelper

/** Lets the user pick how loosening changes are held back, and shows a change that is waiting. */
@Composable
internal fun CommitmentModeItem(
    commitment: CommitmentState,
    onModeSelected: (CommitmentMode) -> Unit,
    onCancelPendingChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticHelper = rememberHapticHelper()
    val modes = CommitmentMode.entries
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_commitment_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_commitment_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = commitment.mode == mode,
                    onClick = {
                        hapticHelper.playTick()
                        onModeSelected(mode)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                ) {
                    Text(stringResource(mode.labelRes()))
                }
            }
        }
        when (commitment.mode) {
            CommitmentMode.Off -> Unit

            CommitmentMode.Delay -> CommitmentNote(
                stringResource(R.string.settings_commitment_delay_note, stringResource(delayLabelRes(commitment.delayMillis))),
            )

            CommitmentMode.Pin -> CommitmentNote(stringResource(R.string.settings_commitment_pin_note))
        }
        commitment.pendingChange?.let { pending ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_commitment_pending,
                        formatTimeWithDay(LocalContext.current, pending.applyAtMillis),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancelPendingChange) {
                    Text(stringResource(R.string.settings_commitment_cancel_change))
                }
            }
        }
    }
}

@Composable
private fun CommitmentNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Shows the dialog for [prompt]. */
@Composable
internal fun SettingsPromptDialog(
    prompt: SettingsPrompt,
    onDelayChosen: (Long) -> Unit,
    onNewPinEntered: (String) -> Unit,
    onPinSubmitted: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (prompt) {
        SettingsPrompt.ChooseDelay -> ChooseDelayDialog(onDelayChosen = onDelayChosen, onDismiss = onDismiss)

        is SettingsPrompt.ChoosePin -> PinDialog(
            title = stringResource(
                if (prompt.firstEntry == null) R.string.settings_commitment_choose_pin else R.string.settings_commitment_confirm_pin,
            ),
            message = if (prompt.firstEntry == null) stringResource(R.string.settings_commitment_choose_pin_message) else null,
            error = if (prompt.mismatch) stringResource(R.string.settings_commitment_pin_mismatch) else null,
            confirmLabel = stringResource(R.string.settings_commitment_confirm),
            dismissLabel = stringResource(R.string.settings_commitment_cancel),
            onConfirm = onNewPinEntered,
            onDismiss = onDismiss,
            key = prompt,
        )

        is SettingsPrompt.EnterPin -> PinDialog(
            title = stringResource(R.string.settings_commitment_enter_pin),
            message = stringResource(R.string.settings_commitment_enter_pin_message),
            error = if (prompt.wrongPin) stringResource(R.string.settings_commitment_pin_wrong) else null,
            confirmLabel = stringResource(R.string.settings_commitment_confirm),
            dismissLabel = stringResource(R.string.settings_commitment_cancel),
            onConfirm = onPinSubmitted,
            onDismiss = onDismiss,
        )

        is SettingsPrompt.Scheduled -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_commitment_scheduled)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_commitment_scheduled_message,
                        formatTimeWithDay(LocalContext.current, prompt.applyAtMillis),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_commitment_ok)) }
            },
        )
    }
}

@Composable
private fun ChooseDelayDialog(onDelayChosen: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_commitment_choose_delay)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                COMMITMENT_DELAY_OPTIONS_MILLIS.forEach { delayMillis ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = false, role = Role.RadioButton, onClick = { onDelayChosen(delayMillis) })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = false, onClick = null)
                        Text(
                            text = stringResource(delayLabelRes(delayMillis)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_commitment_cancel)) }
        },
    )
}

private fun CommitmentMode.labelRes(): Int = when (this) {
    CommitmentMode.Off -> R.string.settings_commitment_off
    CommitmentMode.Delay -> R.string.settings_commitment_delay
    CommitmentMode.Pin -> R.string.settings_commitment_pin
}

private fun delayLabelRes(delayMillis: Long): Int = when {
    delayMillis <= 15 * 60 * 1000L -> R.string.settings_commitment_delay_15_minutes
    delayMillis <= 60 * 60 * 1000L -> R.string.settings_commitment_delay_1_hour
    else -> R.string.settings_commitment_delay_24_hours
}
