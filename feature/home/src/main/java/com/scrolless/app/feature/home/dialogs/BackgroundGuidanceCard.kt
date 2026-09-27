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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.scrolless.app.feature.home.BackgroundRestrictionOem
import com.scrolless.app.feature.home.R
import com.scrolless.app.feature.home.isIgnoringBatteryOptimizations
import com.scrolless.app.feature.home.openBackgroundSettings
import com.scrolless.app.feature.home.requestIgnoreBatteryOptimizations
import timber.log.Timber

/** Tracks whether Scrolless is still subject to battery optimization, refreshed on every resume. */
@Composable
internal fun rememberIsIgnoringBatteryOptimizations(): Boolean {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var isIgnoring by remember { mutableStateOf(isPreview || context.isIgnoringBatteryOptimizations()) }
    if (!isPreview) {
        LifecycleResumeEffect(context) {
            isIgnoring = context.isIgnoringBatteryOptimizations()
            onPauseOrDispose { }
        }
    }
    return isIgnoring
}

/** Compact card with one-tap actions that let Scrolless keep running in the background. */
@Composable
internal fun BackgroundGuidanceCard(
    oem: BackgroundRestrictionOem?,
    isIgnoringBatteryOptimizations: Boolean,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (showHeader) {
                Text(
                    text = stringResource(R.string.background_step_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.background_step_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            GuidanceAction(
                label = stringResource(R.string.background_battery_step),
                actionLabel = stringResource(R.string.background_allow_button),
                isDone = isIgnoringBatteryOptimizations,
                onClick = {
                    Timber.i("BackgroundGuidance: request battery optimization exemption")
                    context.requestIgnoreBatteryOptimizations()
                },
            )
            if (oem?.hasAutostartManager == true) {
                GuidanceAction(
                    label = stringResource(R.string.background_autostart_step),
                    actionLabel = stringResource(R.string.background_open_button),
                    isDone = false,
                    onClick = {
                        Timber.i("BackgroundGuidance: open autostart settings (oem=%s)", oem)
                        context.openBackgroundSettings(oem)
                    },
                )
            }
        }
    }
}

@Composable
private fun GuidanceAction(label: String, actionLabel: String, isDone: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        if (isDone) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp),
            )
        } else {
            FilledTonalButton(onClick = onClick) {
                Text(text = actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
