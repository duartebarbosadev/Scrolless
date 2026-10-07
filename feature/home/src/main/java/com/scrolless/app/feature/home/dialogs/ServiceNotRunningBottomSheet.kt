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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scrolless.app.designsystem.component.PopupCircleIcon
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.feature.home.BackgroundRestrictionOem
import com.scrolless.app.feature.home.R
import com.scrolless.app.feature.home.openActivityAccessibilitySettings
import com.scrolless.app.feature.home.openBackgroundSettings
import com.scrolless.app.feature.home.requestIgnoreBatteryOptimizations
import timber.log.Timber

/**
 * Shown when the accessibility service is enabled in settings but the system is no longer bound to
 * it, which typically means an OEM battery manager killed the app process.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceNotRunningBottomSheet(onRestartClick: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    ModalBottomSheet(
        onDismissRequest = {
            Timber.d("ServiceNotRunning: dismiss")
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.Transparent,
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
    // Once everything we can check is done, the card would only show a lone checkmark.
    val showGuidance = !isIgnoringBatteryOptimizations || oem?.hasAutostartManager == true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Room for the floating icon
            Spacer(modifier = Modifier.height(116.dp))

            Text(
                text = stringResource(R.string.service_not_running_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.service_not_running_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    GuidanceAction(
                        label = stringResource(R.string.service_not_running_step_restart),
                        actionLabel = stringResource(R.string.background_open_button),
                        isDone = false,
                        primary = true,
                        onClick = {
                            Timber.i("ServiceNotRunning: open accessibility settings")
                            onRestartClick()
                            try {
                                context.openActivityAccessibilitySettings()
                            } catch (e: Exception) {
                                Timber.e(e, "ServiceNotRunning: failed to open accessibility settings")
                            }
                        },
                    )
                    if (showGuidance) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GuidanceAction(
                            label = stringResource(R.string.background_battery_step),
                            actionLabel = stringResource(R.string.background_allow_button),
                            isDone = isIgnoringBatteryOptimizations,
                            onClick = { context.requestIgnoreBatteryOptimizations() },
                        )
                    }
                    if (oem?.hasAutostartManager == true) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GuidanceAction(
                            label = stringResource(R.string.background_autostart_step),
                            actionLabel = stringResource(R.string.background_open_button),
                            isDone = false,
                            onClick = { context.openBackgroundSettings(oem) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }

        PopupCircleIcon(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            iconRes = R.drawable.ic_circle_battery,
            contentDescription = stringResource(R.string.service_not_running_title),
        )
    }
}

@DevicePreviews
@Composable
private fun ServiceNotRunningContentPreview() {
    ScrollessTheme(darkTheme = true) {
        ServiceNotRunningContent(oem = BackgroundRestrictionOem.Xiaomi, onRestartClick = {}, onDismiss = {})
    }
}
