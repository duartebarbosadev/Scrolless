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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scrolless.app.designsystem.component.AnimatedButton
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.feature.home.BackgroundRestrictionOem
import com.scrolless.app.feature.home.R
import com.scrolless.app.feature.home.hasPendingBackgroundSteps
import timber.log.Timber

/**
 * Setup step shown once, right after accessibility is enabled, on phones known to kill background apps.
 * [onContinue] is called when the user finishes or skips it.
 */
@Composable
fun BackgroundSetupBottomSheet(onContinue: () -> Unit) {
    ExpandedModalBottomSheet(
        onDismissRequest = {
            Timber.d("BackgroundSetup: dismissed")
            onContinue()
        },
    ) {
        BackgroundSetupContent(oem = remember { BackgroundRestrictionOem.current() }, onContinue = onContinue)
    }
}

@Composable
private fun BackgroundSetupContent(oem: BackgroundRestrictionOem?, onContinue: () -> Unit) {
    val isIgnoringBatteryOptimizations = rememberIsIgnoringBatteryOptimizations()

    // Nothing else can be checked on phones without an autostart manager, so move on by ourselves.
    LaunchedEffect(isIgnoringBatteryOptimizations) {
        if (!oem.hasPendingBackgroundSteps(isIgnoringBatteryOptimizations)) {
            Timber.i("BackgroundSetup: battery optimization disabled - continuing")
            onContinue()
        }
    }

    GuidanceSheetContent(
        title = stringResource(R.string.background_step_title),
        description = stringResource(R.string.background_step_description),
    ) {
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
}

@DevicePreviews
@Composable
private fun BackgroundSetupContentPreview() {
    ScrollessTheme(darkTheme = true) {
        BackgroundSetupContent(oem = BackgroundRestrictionOem.Xiaomi, onContinue = {})
    }
}
