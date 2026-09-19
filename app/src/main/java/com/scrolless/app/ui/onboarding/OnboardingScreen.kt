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
package com.scrolless.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrolless.app.R
import com.scrolless.app.core.data.repository.OnboardingPreferences
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.designsystem.util.formatMinutes
import com.scrolless.app.feature.home.R as HomeR
import com.scrolless.app.feature.home.dialogs.IntervalTimerDialog
import com.scrolless.app.feature.settings.R as SettingsR
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(onFinished: () -> Unit, replay: Boolean = false, viewModel: OnboardingViewModel = hiltViewModel()) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val draft = preferences
    if (draft == null) {
        Surface(Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                if (failed) {
                    TextButton(onClick = viewModel::load) { Text(stringResource(R.string.onboarding_retry)) }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
        return
    }
    OnboardingContent(
        preferences = draft,
        saving = saving,
        failed = failed,
        onChange = viewModel::update,
        onFinish = { viewModel.finish(onFinished = onFinished) },
        onSkip = { if (replay) onFinished() else viewModel.finish(skip = true, onFinished = onFinished) },
    )
}

@Composable
private fun OnboardingContent(
    preferences: OnboardingPreferences,
    saving: Boolean,
    failed: Boolean,
    onChange: (OnboardingPreferences) -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var feedEntry by rememberSaveable { mutableStateOf(false) }
    var intervalDialog by rememberSaveable { mutableStateOf(false) }
    val blocking = preferences.option != BlockOption.NothingSelected
    val colors = MaterialTheme.colorScheme
    BackHandler(enabled = step > 0 || saving) { if (!saving) step-- }

    if (intervalDialog) {
        IntervalTimerDialog(
            initialBreakMillis = preferences.intervalLength.takeIf { it > 0 } ?: 3_600_000L,
            initialAllowanceMillis = preferences.allowance.takeIf { it > 0 } ?: 300_000L,
            onConfirm = { interval, allowance ->
                onChange(preferences.copy(option = BlockOption.IntervalTimer, intervalLength = interval, allowance = allowance))
                intervalDialog = false
            },
            onDismiss = { intervalDialog = false },
        )
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.safeDrawingPadding()) {
            val phoneHeight = (maxHeight * 0.43f).coerceIn(200.dp, 360.dp)
            Column(Modifier.widthIn(max = 560.dp).fillMaxSize().align(Alignment.TopCenter)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (step > 0) {
                        IconButton(onClick = { step-- }, enabled = !saving) {
                            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(SettingsR.string.back))
                        }
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_launcher_monochrome), null,
                            tint = colors.primary, modifier = Modifier.padding(12.dp).size(24.dp),
                        )
                    }
                    Text("Scrolless", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onSkip, enabled = !saving) { Text(stringResource(R.string.onboarding_skip)) }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(5) { index ->
                        Box(
                            Modifier.weight(1f).height(3.dp).clip(CircleShape)
                                .background(if (index <= step) colors.primary else colors.surfaceContainerHighest),
                        )
                    }
                }
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f),
                    label = "onboardingStep",
                ) { page ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.onboarding_step, page + 1, 5),
                            style = MaterialTheme.typography.labelMedium, color = colors.primary,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (page < 4) {
                            val asset = when (page) {
                                0 -> if (feedEntry) "instagram_feed_reel_block" else "instagram_reels_block"
                                1 -> "instagram_feed_reel_cover"
                                2 -> if (preferences.allowDm || !blocking) "instagram_dm_reel_allowed" else "instagram_dm_reel_block"
                                else -> "instagram_story_block"
                            }
                            val demonstratesBlocking = blocking && (page != 3 || preferences.includeStories)
                            AnimationStage(asset, demonstratesBlocking, phoneHeight)
                            if (page == 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = !feedEntry, onClick = { feedEntry = false },
                                        label = { Text(stringResource(R.string.onboarding_reels_tab)) },
                                    )
                                    FilterChip(
                                        selected = feedEntry, onClick = { feedEntry = true },
                                        label = { Text(stringResource(R.string.onboarding_feed_tab)) },
                                    )
                                }
                            } else {
                                Spacer(Modifier.height(16.dp))
                            }
                        } else {
                            Box(
                                Modifier.padding(vertical = 16.dp).size(80.dp).background(colors.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_launcher_monochrome),
                                    null,
                                    Modifier.size(48.dp),
                                    tint = colors.onPrimaryContainer,
                                )
                            }
                        }
                        val title = when (page) {
                            0 -> R.string.onboarding_reels_title
                            1 -> R.string.onboarding_feed_title
                            2 -> R.string.onboarding_dm_title
                            3 -> R.string.onboarding_stories_title
                            else -> R.string.onboarding_finish_title
                        }
                        val body = when (page) {
                            0 -> R.string.onboarding_reels_body
                            1 -> R.string.onboarding_feed_body
                            2 -> R.string.onboarding_dm_body
                            3 -> R.string.onboarding_stories_body
                            else -> R.string.onboarding_finish_body
                        }
                        Text(
                            stringResource(title), style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(body), style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        when (page) {
                            0 -> ChoiceToggle(
                                stringResource(R.string.onboarding_block_reels), blocking,
                                { onChange(preferences.copy(option = if (it) BlockOption.BlockAll else BlockOption.NothingSelected)) },
                            )

                            1 -> Surface(shape = RoundedCornerShape(20.dp), color = colors.surfaceContainerLow) {
                                Text(
                                    stringResource(if (blocking) R.string.onboarding_feed_included else R.string.onboarding_mode_off),
                                    Modifier.fillMaxWidth().padding(18.dp),
                                    style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                                )
                            }

                            2 -> {
                                ChoiceToggle(
                                    stringResource(R.string.onboarding_dm_toggle),
                                    preferences.allowDm,
                                    { onChange(preferences.copy(allowDm = it)) },
                                )
                                Note(stringResource(SettingsR.string.settings_allow_videos_sent_by_dms_note))
                            }

                            3 -> {
                                ChoiceToggle(
                                    stringResource(SettingsR.string.settings_include_stories_title),
                                    preferences.includeStories,
                                    { onChange(preferences.copy(includeStories = it)) },
                                )
                                Note(stringResource(SettingsR.string.settings_include_stories_note))
                            }

                            4 -> {
                                ModeChoices(preferences, onChange, onInterval = { intervalDialog = true })
                                Spacer(Modifier.height(16.dp))
                                ChoiceToggle(
                                    stringResource(R.string.onboarding_dm_toggle),
                                    preferences.allowDm,
                                    { onChange(preferences.copy(allowDm = it)) },
                                )
                                Spacer(Modifier.height(8.dp))
                                ChoiceToggle(
                                    stringResource(SettingsR.string.settings_include_stories_title),
                                    preferences.includeStories,
                                    { onChange(preferences.copy(includeStories = it)) },
                                )
                            }
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (failed) {
                        Text(stringResource(R.string.onboarding_error), color = colors.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = { if (step == 4) onFinish() else step++ },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text(
                                stringResource(if (step == 4) R.string.onboarding_finish else R.string.onboarding_next),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.onboarding_change_later),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimationStage(asset: String, blocking: Boolean, phoneHeight: Dp) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().height(phoneHeight), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent), center, size.width * 0.6f),
                size.width * 0.6f,
                center,
            )
            drawCircle(accent.copy(alpha = 0.08f), size.width * 0.43f, center, style = Stroke(1.dp.toPx()))
            drawCircle(accent.copy(alpha = 0.06f), size.width * 0.58f, center, style = Stroke(1.dp.toPx()))
        }
        OnboardingAnimation(
            asset, blocking, stringResource(R.string.onboarding_demo),
            Modifier.height(phoneHeight).aspectRatio(360f / 740f)
                .shadow(6.dp, RoundedCornerShape(phoneHeight * (48f / 740f)))
                .clip(RoundedCornerShape(phoneHeight * (48f / 740f))),
        )
    }
}

@Composable
private fun ChoiceToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (checked) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChecked)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        Modifier.padding(top = 10.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ModeChoices(preferences: OnboardingPreferences, onChange: (OnboardingPreferences) -> Unit, onInterval: () -> Unit) {
    val dailyLimitLabel = stringResource(HomeR.string.daily_limit_configure_time_content_description)
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(BlockOption.BlockAll, BlockOption.DailyLimit, BlockOption.IntervalTimer, BlockOption.NothingSelected).forEach { option ->
            val title = when (option) {
                BlockOption.BlockAll -> stringResource(HomeR.string.block_all).replace('\n', ' ')
                BlockOption.DailyLimit -> stringResource(HomeR.string.daily_limit).replace('\n', ' ')
                BlockOption.IntervalTimer -> stringResource(HomeR.string.time_interval).replace('\n', ' ')
                BlockOption.NothingSelected -> stringResource(R.string.onboarding_mode_off)
            }
            val selected = preferences.option == option
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton) {
                            when (option) {
                                BlockOption.IntervalTimer -> onInterval()

                                BlockOption.DailyLimit -> onChange(
                                    preferences.copy(option = option, dailyLimit = preferences.dailyLimit.takeIf { it > 0 } ?: 900_000L),
                                )

                                else -> onChange(preferences.copy(option = option))
                            }
                        }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected, onClick = null)
                        Text(title, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleSmall)
                    }
                    if (selected && option == BlockOption.DailyLimit) {
                        Text(
                            (preferences.dailyLimit / 60_000).toInt().formatMinutes(),
                            Modifier.padding(horizontal = 24.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Slider(
                            value = (preferences.dailyLimit / 60_000f).coerceIn(1f, 120f),
                            onValueChange = { onChange(preferences.copy(dailyLimit = it.roundToInt() * 60_000L)) },
                            valueRange = 1f..120f, steps = 118,
                            modifier = Modifier.padding(horizontal = 20.dp).semantics { contentDescription = dailyLimitLabel },
                        )
                    }
                    if (selected && option == BlockOption.IntervalTimer) {
                        TextButton(onClick = onInterval, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(
                                stringResource(
                                    HomeR.string.interval_timer_card_summary,
                                    (preferences.allowance / 60_000).toInt().formatMinutes(),
                                    (preferences.intervalLength / 60_000).toInt().formatMinutes(),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
