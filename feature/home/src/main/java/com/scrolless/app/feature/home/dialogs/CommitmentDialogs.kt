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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scrolless.app.designsystem.component.PinDialog
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.designsystem.util.formatTimeWithDay
import com.scrolless.app.feature.home.CommitmentPrompt
import com.scrolless.app.feature.home.R

/** Shows the dialog for [prompt]: a pause offer, a PIN request or a scheduled-change notice. */
@Composable
fun CommitmentPromptDialog(
    prompt: CommitmentPrompt,
    pauseDurationMillis: Long,
    onPauseInstead: () -> Unit,
    onChangeAnyway: () -> Unit,
    onPinSubmitted: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (prompt) {
        is CommitmentPrompt.OfferPause -> PauseInsteadDialog(
            pauseMinutes = (pauseDurationMillis / 60_000L).toInt().coerceAtLeast(1),
            onPauseInstead = onPauseInstead,
            onChangeAnyway = onChangeAnyway,
            onDismiss = onDismiss,
        )

        is CommitmentPrompt.EnterPin -> PinDialog(
            title = stringResource(R.string.commitment_pin_title),
            message = stringResource(R.string.commitment_pin_message),
            error = if (prompt.wrongPin) stringResource(R.string.commitment_pin_wrong) else null,
            confirmLabel = stringResource(R.string.commitment_confirm),
            dismissLabel = stringResource(R.string.commitment_cancel),
            onConfirm = onPinSubmitted,
            onDismiss = onDismiss,
        )

        is CommitmentPrompt.Scheduled -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            title = { Text(stringResource(R.string.commitment_scheduled_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.commitment_scheduled_message,
                        formatTimeWithDay(LocalContext.current, prompt.applyAtMillis),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.commitment_ok)) }
            },
        )
    }
}

@Composable
private fun PauseInsteadDialog(pauseMinutes: Int, onPauseInstead: () -> Unit, onChangeAnyway: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.commitment_pause_instead_title)) },
        text = { Text(stringResource(R.string.commitment_pause_instead_message, pauseMinutes)) },
        confirmButton = {
            TextButton(onClick = onPauseInstead) {
                Text(stringResource(R.string.commitment_pause_instead_confirm, pauseMinutes))
            }
        },
        dismissButton = {
            TextButton(onClick = onChangeAnyway) { Text(stringResource(R.string.commitment_change_anyway)) }
        },
    )
}

/** Tells the user a loosening change is queued and lets them take it back. */
@Composable
fun PendingChangeCard(applyAtMillis: Long, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text(
                text = stringResource(
                    R.string.commitment_pending_banner,
                    formatTimeWithDay(LocalContext.current, applyAtMillis),
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) { Text(stringResource(R.string.commitment_pending_cancel)) }
        }
    }
}

@DevicePreviews
@Composable
private fun PauseInsteadDialogPreview() {
    ScrollessTheme {
        PauseInsteadDialog(pauseMinutes = 5, onPauseInstead = {}, onChangeAnyway = {}, onDismiss = {})
    }
}

@DevicePreviews
@Composable
private fun PendingChangeCardPreview() {
    ScrollessTheme {
        PendingChangeCard(applyAtMillis = System.currentTimeMillis() + 60 * 60 * 1000L, onCancel = {})
    }
}
