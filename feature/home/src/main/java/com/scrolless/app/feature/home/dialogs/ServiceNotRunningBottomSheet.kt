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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.feature.home.BackgroundRestrictionOem
import com.scrolless.app.feature.home.R
import com.scrolless.app.feature.home.openActivityAccessibilitySettings
import timber.log.Timber

/**
 * Shown when the accessibility service is enabled in settings but the system is no longer bound to
 * it, which typically means an OEM battery manager killed the app process.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceNotRunningBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = {
            Timber.d("ServiceNotRunning: dismiss")
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        ServiceNotRunningContent(onDismiss = onDismiss)
    }
}

@Composable
private fun ServiceNotRunningContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val oem = remember { BackgroundRestrictionOem.current() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.service_not_running_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.service_not_running_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))
        NumberedLine(number = stringResource(R.string.step_one), text = stringResource(R.string.service_not_running_step_restart))
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                Timber.i("ServiceNotRunning: open accessibility settings")
                try {
                    context.openActivityAccessibilitySettings()
                } catch (e: Exception) {
                    Timber.e(e, "ServiceNotRunning: failed to open accessibility settings")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(text = stringResource(R.string.go_to_accessibility_settings), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
        NumberedLine(number = stringResource(R.string.step_two), text = stringResource(R.string.service_not_running_step_background))
        Spacer(modifier = Modifier.height(12.dp))
        BackgroundGuidanceCard(oem = oem, isIgnoringBatteryOptimizations = rememberIsIgnoringBatteryOptimizations())

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.close))
        }
    }
}

@Composable
private fun NumberedLine(number: String, text: String) {
    Row {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@DevicePreviews
@Composable
private fun ServiceNotRunningContentPreview() {
    ScrollessTheme(darkTheme = true) {
        ServiceNotRunningContent(onDismiss = {})
    }
}
