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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
@Composable
fun ServiceNotRunningBottomSheet(onRestartClick: () -> Unit, onDismiss: () -> Unit) {
    GuidanceBottomSheet(
        onDismissRequest = {
            Timber.d("ServiceNotRunning: dismiss")
            onDismiss()
        },
    ) {
        ServiceNotRunningContent(
            oem = remember { BackgroundRestrictionOem.current() },
            onRestartClick = onRestartClick,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ServiceNotRunningContent(oem: BackgroundRestrictionOem?, onRestartClick: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isIgnoringBatteryOptimizations = rememberIsIgnoringBatteryOptimizations()
    // Skip the background steps when all they would show is a checkmark.
    val showGuidance = !isIgnoringBatteryOptimizations || oem?.hasAutostartManager == true

    GuidanceSheetContent(
        title = stringResource(R.string.service_not_running_title),
        description = stringResource(R.string.service_not_running_description),
    ) {
        BackgroundGuidanceCard {
            GuidanceAction(
                label = stringResource(R.string.service_not_running_step_restart),
                actionLabel = stringResource(R.string.background_open_button),
                isDone = false,
                primary = true,
                onClick = {
                    Timber.i("ServiceNotRunning: open accessibility settings")
                    onRestartClick()
                    context.openActivityAccessibilitySettings()
                },
            )
            if (showGuidance) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BackgroundGuidanceSteps(oem = oem, isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(R.string.close))
        }
    }
}

@DevicePreviews
@Composable
private fun ServiceNotRunningContentPreview() {
    ScrollessTheme(darkTheme = true) {
        ServiceNotRunningContent(oem = BackgroundRestrictionOem.Xiaomi, onRestartClick = {}, onDismiss = {})
    }
}
