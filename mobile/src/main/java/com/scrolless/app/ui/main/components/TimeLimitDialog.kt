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
package com.scrolless.app.ui.main.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.duration.DurationDialog
import com.maxkeppeler.sheets.duration.models.DurationConfig
import com.maxkeppeler.sheets.duration.models.DurationFormat
import com.maxkeppeler.sheets.duration.models.DurationSelection
import com.scrolless.app.R
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeLimitDialog(onDismiss: (selectedTimeInSeconds: Long) -> Unit) {
    Timber.d("TimeLimitDialog: show")

    val state = rememberUseCaseState(
        visible = true,
        onCloseRequest = {
            Timber.d("TimeLimitDialog: closed without selection")
            onDismiss(-1) // -1 means cancelled,
        },
    )

    DurationDialog(
        state = state,
        selection = DurationSelection { newTimeInSeconds ->
            Timber.i("TimeLimitDialog: selected %d seconds", newTimeInSeconds)
            onDismiss(newTimeInSeconds)
        },
        config = DurationConfig(
            timeFormat = DurationFormat.HH_MM,
            currentTime = 0L,
            maxTime = 24 * 60 * 60, // 24 hours in seconds,
            displayClearButton = false,
        ),
        header = Header.Default(
            title = stringResource(R.string.time_limit_dialog_title),
        ),
    )
}

@Preview
@Composable
private fun PreviewTimeLimitDialog() {
    TimeLimitDialog(onDismiss = {})
}