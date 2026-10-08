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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scrolless.app.designsystem.component.AnimatedButton
import com.scrolless.app.designsystem.component.PopupCircleIcon
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.feature.home.BackgroundRestrictionOem
import com.scrolless.app.feature.home.R
import timber.log.Timber

/**
 * Setup step shown right after accessibility is enabled, on phones known to kill background apps.
 * [onContinue] is called when the user finishes or skips it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSetupBottomSheet(onContinue: () -> Unit) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    ModalBottomSheet(
        onDismissRequest = {
            Timber.d("BackgroundSetup: dismissed")
            onContinue()
        },
        sheetState = sheetState,
        containerColor = Color.Transparent,
    ) {
        BackgroundSetupContent(oem = remember { BackgroundRestrictionOem.current() }, onContinue = onContinue)
    }
}

@Composable
private fun BackgroundSetupContent(oem: BackgroundRestrictionOem?, onContinue: () -> Unit) {
    val isIgnoringBatteryOptimizations = rememberIsIgnoringBatteryOptimizations()

    // Nothing else can be checked on phones without an autostart manager, so move on by ourselves.
    if (!LocalInspectionMode.current) {
        LaunchedEffect(isIgnoringBatteryOptimizations) {
            if (isIgnoringBatteryOptimizations && oem?.hasAutostartManager != true) {
                Timber.i("BackgroundSetup: battery optimization disabled - continuing")
                onContinue()
            }
        }
    }

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
                text = stringResource(R.string.background_step_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.background_step_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))
            BackgroundGuidanceCard {
                BackgroundGuidanceSteps(oem = oem, isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations)
            }

            Spacer(modifier = Modifier.height(24.dp))
            AnimatedButton(
                onClick = {
                    Timber.i("BackgroundSetup: continue clicked")
                    onContinue()
                },
                text = stringResource(R.string.background_continue_button),
                delay = 200L,
            )
        }

        PopupCircleIcon(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            iconRes = R.drawable.ic_circle_battery,
            contentDescription = stringResource(R.string.background_step_title),
        )
    }
}

@DevicePreviews
@Composable
private fun BackgroundSetupContentPreview() {
    ScrollessTheme(darkTheme = true) {
        BackgroundSetupContent(oem = BackgroundRestrictionOem.Xiaomi, onContinue = {})
    }
}
