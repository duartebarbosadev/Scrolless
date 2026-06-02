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

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.scrolless.app.designsystem.theme.LocalSharedTransitionScope
import com.scrolless.app.designsystem.theme.SETTINGS_TRANSITION_KEY
import com.scrolless.app.designsystem.theme.ScrollessTheme
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        modifier = modifier,
        uiState = uiState,
        onPauseDurationChange = viewModel::onPauseDurationChange,
        onExceptReelsSentByDmChange = viewModel::onExceptReelsSentByDmChange,
        onTimerOverlayEnabledChange = viewModel::onTimerOverlayEnabledChange,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    uiState: SettingsUiState,
    onPauseDurationChange: (Int) -> Unit,
    onExceptReelsSentByDmChange: (Boolean) -> Unit,
    onTimerOverlayEnabledChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current

    val sharedBoundsModifier = if (sharedTransitionScope != null) {
        val animatedVisibilityScope = LocalNavAnimatedContentScope.current
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = SETTINGS_TRANSITION_KEY),
                animatedVisibilityScope = animatedVisibilityScope,
                clipInOverlayDuringTransition = OverlayClip(clipShape = RoundedCornerShape(0.dp)),
            )
        }
    } else {
        Modifier
    }

    Scaffold(
        modifier = modifier.then(sharedBoundsModifier),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 22.dp, bottom = 28.dp),
        ) {
            SettingsSectionLabel(stringResource(R.string.settings_section_blocking))

            Spacer(modifier = Modifier.height(10.dp))

            SettingsGroup {
                PauseDurationItem(
                    pauseDurationMinutes = uiState.pauseDurationMinutes,
                    onPauseDurationChange = onPauseDurationChange,
                )

                SettingsDivider()

                ExceptReelsSentByDmItem(
                    checked = uiState.exceptReelsSentByDm,
                    onCheckedChange = onExceptReelsSentByDmChange,
                )

                SettingsDivider()

                TimerOverlayItem(
                    checked = uiState.timerOverlayEnabled,
                    onCheckedChange = onTimerOverlayEnabledChange,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 2.dp),
    )
}

@Composable
private fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 20.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
}

@Composable
private fun PauseDurationItem(pauseDurationMinutes: Int, onPauseDurationChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var sliderValue by remember(pauseDurationMinutes) {
        mutableIntStateOf(pauseDurationMinutes)
    }
    val sliderTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val valueLabel = pluralStringResource(R.plurals.settings_pause_duration_value, sliderValue, sliderValue)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_pause_duration_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SettingsValuePill(
                text = valueLabel,
            )
        }
        Text(
            text = stringResource(R.string.settings_pause_duration_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = sliderValue.toFloat(),
            onValueChange = {
                sliderValue = it.roundToInt()
            },
            onValueChangeFinished = {
                if (sliderValue != pauseDurationMinutes) {
                    onPauseDurationChange(sliderValue)
                }
            },
            valueRange = 1f..15f,
            steps = 13,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = sliderTrackColor,
                activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.56f),
                inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        )
    }
}

@Composable
private fun SettingsValuePill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun ExceptReelsSentByDmItem(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SettingsSwitchItem(
        title = stringResource(R.string.settings_except_reels_sent_by_dms_title),
        description = stringResource(R.string.settings_except_reels_sent_by_dms_description),
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
    )
}

@Composable
private fun TimerOverlayItem(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SettingsSwitchItem(
        title = stringResource(R.string.settings_show_onscreen_timer_title),
        description = stringResource(R.string.settings_show_onscreen_timer_description),
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.size(width = 58.dp, height = 36.dp),
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7)
@Preview(showBackground = true, device = Devices.PIXEL_7, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    ScrollessTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(pauseDurationMinutes = 5, timerOverlayEnabled = true),
            onPauseDurationChange = {},
            onNavigateBack = {},
            onExceptReelsSentByDmChange = {},
            onTimerOverlayEnabledChange = {},
        )
    }
}
