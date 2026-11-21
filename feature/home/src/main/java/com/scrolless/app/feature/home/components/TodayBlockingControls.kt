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
package com.scrolless.app.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockingSettings
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.designsystem.util.rememberHapticHelper
import com.scrolless.app.designsystem.util.toCountdownLabel
import com.scrolless.app.designsystem.util.toIntervalLabel
import com.scrolless.app.feature.home.HomeUiState
import com.scrolless.app.feature.home.R
import kotlin.math.min
import kotlin.math.roundToInt
import timber.log.Timber

// The Daily Limit button's selected corner radius. The tab, flares included, spans only the flat part
// of the button's bottom edge so its flares never land on the curved corners
private val CONFIG_TAB_BUTTON_CORNER = 24.dp
private val CONFIG_TAB_HEIGHT = 40.dp
private val CONFIG_TAB_FLARE_RADIUS = 8.dp
private val CONFIG_TAB_CORNER_RADIUS = 16.dp
private val CONFIG_TAB_OVERLAP = 2.dp

@Composable
fun TodayBlockingControls(
    uiState: HomeUiState,
    isBlockingActive: Boolean,
    isPauseActive: Boolean,
    pauseRemainingMillis: Long,
    onBlockOptionSelected: (BlockOption) -> Unit,
    onConfigureDailyLimit: () -> Unit,
    onIntervalTimerClick: () -> Unit,
    onIntervalTimerEdit: () -> Unit,
    onPauseToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = rememberHapticHelper()

    // Each feature button grows while pressed, squeezing its neighbours
    val blockAllInteractionSource = remember { MutableInteractionSource() }
    val dailyLimitInteractionSource = remember { MutableInteractionSource() }
    val intervalInteractionSource = remember { MutableInteractionSource() }

    // Where the Daily Limit button currently sits, so its config tab can follow the button group's
    // press-to-expand animation. Only read during layout, so updates don't recompose.
    var dailyLimitBounds by remember { mutableStateOf(Rect.Zero) }
    var configTabRowLeft by remember { mutableFloatStateOf(0f) }

    val motionScheme = MaterialTheme.motionScheme
    val expandIn = expandVertically(expandFrom = Alignment.Top, animationSpec = motionScheme.defaultSpatialSpec()) +
        fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
    val shrinkOut = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = motionScheme.defaultSpatialSpec()) +
        fadeOut(animationSpec = motionScheme.defaultEffectsSpec())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = motionScheme.defaultSpatialSpec()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FeatureButtonsRow(
            selectedOption = uiState.blockOption,
            onBlockAllClick = {
                val isSelected = uiState.blockOption == BlockOption.BlockAll
                hapticFeedback.playToggle(!isSelected)
                val newOption = if (isSelected) {
                    BlockOption.NothingSelected
                } else {
                    BlockOption.BlockAll
                }
                Timber.i("BlockAll clicked -> newOption=%s (prev=%s)", newOption, uiState.blockOption)
                onBlockOptionSelected(newOption)
            },
            onDailyLimitClick = {
                val isSelected = uiState.blockOption == BlockOption.DailyLimit
                hapticFeedback.playToggle(!isSelected)
                if (uiState.settings.dailyLimitMillis == 0L && !isSelected) {
                    Timber.d("DailyLimit clicked -> open TimeLimitDialog (no limit set)")
                    onConfigureDailyLimit()
                } else {
                    val newOption = if (isSelected) {
                        BlockOption.NothingSelected
                    } else {
                        BlockOption.DailyLimit
                    }
                    Timber.i("DailyLimit clicked -> newOption=%s (prev=%s)", newOption, uiState.blockOption)
                    onBlockOptionSelected(newOption)
                }
            },
            onIntervalTimerClick = {
                val isSelected = uiState.blockOption == BlockOption.IntervalTimer
                hapticFeedback.playToggle(!isSelected)
                Timber.i("IntervalTimer clicked from feature row")
                onIntervalTimerClick()
            },
            blockAllInteractionSource = blockAllInteractionSource,
            dailyLimitInteractionSource = dailyLimitInteractionSource,
            intervalInteractionSource = intervalInteractionSource,
            onDailyLimitPlaced = { dailyLimitBounds = it },
        )

        AnimatedVisibility(
            visible = uiState.blockOption == BlockOption.DailyLimit,
            enter = expandIn,
            exit = shrinkOut,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onPlaced { configTabRowLeft = it.positionInRoot().x },
            ) {
                ConfigButton(
                    onClick = {
                        Timber.d("Open DailyLimit config button clicked")
                        onConfigureDailyLimit()
                    },
                    modifier = Modifier.layout { measurable, constraints ->
                        // Stay centered under the Daily Limit button and scale with its width
                        val bounds = dailyLimitBounds
                        val flares = (CONFIG_TAB_FLARE_RADIUS * 2).roundToPx()
                        val width = if (bounds.width > 0f) {
                            (bounds.width - CONFIG_TAB_BUTTON_CORNER.toPx() * 2).roundToInt().coerceAtLeast(flares * 2)
                        } else {
                            (constraints.maxWidth * 0.2f).roundToInt() + flares
                        }
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        val x = if (bounds.width > 0f) {
                            (bounds.center.x - configTabRowLeft - width / 2f).roundToInt()
                        } else {
                            (constraints.maxWidth - width) / 2
                        }
                        // Tuck the top under the button; the layout keeps the tab's visible height
                        val overlap = CONFIG_TAB_OVERLAP.roundToPx()
                        layout(constraints.maxWidth, placeable.height - overlap) { placeable.place(x, -overlap) }
                    },
                )
            }
        }

        if (uiState.blockOption == BlockOption.IntervalTimer) {
            Spacer(
                modifier = Modifier.height(8.dp),
            )
        }

        AnimatedVisibility(
            visible = uiState.blockOption == BlockOption.IntervalTimer,
            enter = expandIn,
            exit = shrinkOut,
        ) {
            IntervalTimerSettingsCard(
                intervalLengthMillis = uiState.settings.intervalLengthMillis,
                allowanceMillis = uiState.settings.intervalAllowanceMillis,
                onEditClick = onIntervalTimerEdit,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (uiState.blockOption == BlockOption.IntervalTimer) {
            Spacer(modifier = Modifier.height(24.dp))
        }

        AnimatedVisibility(
            visible = isBlockingActive || isPauseActive,
            enter = expandIn,
            exit = shrinkOut,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))

                PauseButton(
                    onTogglePause = onPauseToggle,
                    isPaused = isPauseActive,
                    remainingMillis = pauseRemainingMillis,
                    pauseDurationMinutes = (uiState.pauseDurationMillis / 60_000L).toInt().coerceAtLeast(1),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ConfigButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val shape = remember(density) {
        with(density) {
            ConfigTabShape(
                flareRadius = CONFIG_TAB_FLARE_RADIUS.toPx(),
                bottomCornerRadius = CONFIG_TAB_CORNER_RADIUS.toPx(),
                overlap = CONFIG_TAB_OVERLAP.toPx(),
            )
        }
    }

    // Same color as the selected feature button, so the tab reads as part of it
    Button(
        onClick = onClick,
        modifier = modifier.height(CONFIG_TAB_HEIGHT + CONFIG_TAB_OVERLAP),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shape = shape,
        contentPadding = PaddingValues(top = CONFIG_TAB_OVERLAP),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_tune),
            contentDescription = stringResource(id = R.string.daily_limit_configure_time_content_description),
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * A tab hanging from the button above it: the top corners curve outwards into the button and the
 * bottom corners are rounded. The top [overlap] tucks under the button's rounded bottom edge so no
 * gap shows where that edge starts to curve.
 */
private class ConfigTabShape(private val flareRadius: Float, private val bottomCornerRadius: Float, private val overlap: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val width = size.width
        val height = size.height
        val flare = flareRadius.coerceAtMost(width / 4f)
        val corner = bottomCornerRadius.coerceAtMost(min(width / 2f - flare, height - overlap - flare))
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            lineTo(width, overlap)
            arcTo(Rect(Offset(width, overlap + flare), flare), -90f, -90f, false)
            lineTo(width - flare, height - corner)
            arcTo(Rect(Offset(width - flare - corner, height - corner), corner), 0f, 90f, false)
            lineTo(flare + corner, height)
            arcTo(Rect(Offset(flare + corner, height - corner), corner), 90f, 90f, false)
            lineTo(flare, overlap + flare)
            arcTo(Rect(Offset(0f, overlap + flare), flare), 0f, -90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun IntervalTimerSettingsCard(intervalLengthMillis: Long, allowanceMillis: Long, onEditClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasSchedule = intervalLengthMillis > 0 && allowanceMillis > 0
    val allowanceLabel = if (hasSchedule) allowanceMillis.toIntervalLabel() else "--"
    val breakLabel = if (hasSchedule) intervalLengthMillis.toIntervalLabel() else "--"
    val actionLabel = if (hasSchedule) {
        stringResource(R.string.interval_timer_card_edit)
    } else {
        stringResource(R.string.interval_timer_card_set_schedule)
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (hasSchedule) {
                    stringResource(
                        R.string.interval_timer_card_summary,
                        allowanceLabel,
                        breakLabel,
                    )
                } else {
                    stringResource(R.string.interval_timer_card_summary_empty)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IntervalValueChip(
                    label = stringResource(R.string.interval_timer_card_allowance_chip),
                    value = allowanceLabel,
                    modifier = Modifier.weight(1f),
                )
                IntervalValueChip(
                    label = stringResource(R.string.interval_timer_card_break_chip),
                    value = breakLabel,
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun IntervalValueChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PauseButton(
    modifier: Modifier = Modifier,
    onTogglePause: (Boolean) -> Unit,
    isPaused: Boolean,
    remainingMillis: Long,
    pauseDurationMinutes: Int = 5,
) {

    val hapticHelper = rememberHapticHelper()

    val iconRes = if (isPaused) R.drawable.ic_play else R.drawable.ic_pause
    val buttonLabel = if (isPaused) {
        stringResource(id = R.string.resume)
    } else {
        stringResource(id = R.string.pause)
    }

    Column(
        modifier = modifier.wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Touch and accessibility clicks share this, so both play the haptic
        val togglePause = {
            hapticHelper.playToggle(isPaused)
            onTogglePause(!isPaused)
        }
        // Checked means paused, which morphs the button's shape and colours
        ToggleButton(
            checked = isPaused,
            onCheckedChange = { togglePause() },
            // The label already names the action (Pause or Resume), so announce a plain button, not "Resume, on"
            modifier = Modifier.clearAndSetSemantics {
                role = Role.Button
                contentDescription = buttonLabel
                onClick(label = buttonLabel) {
                    togglePause()
                    true
                }
            },
            colors = ToggleButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                checkedContainerColor = MaterialTheme.colorScheme.errorContainer,
                checkedContentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        AnimatedContent(
            targetState = isPaused,
            modifier = Modifier.padding(top = 8.dp),
            label = "pauseButtonSupportingText",
        ) { paused ->
            val text = if (paused) {
                stringResource(id = R.string.pause_resumes_in, remainingMillis.toCountdownLabel())
            } else {
                stringResource(id = R.string.pause_duration_hint, pauseDurationMinutes)
            }
            Text(
                text = text,
                color = if (paused) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                },
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeatureButtonsRow(
    selectedOption: BlockOption,
    onBlockAllClick: () -> Unit,
    onDailyLimitClick: () -> Unit,
    onIntervalTimerClick: () -> Unit,
    blockAllInteractionSource: MutableInteractionSource,
    dailyLimitInteractionSource: MutableInteractionSource,
    intervalInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    onDailyLimitPlaced: (Rect) -> Unit = {},
) {
    ButtonGroup(
        overflowIndicator = {},
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp),
    ) {
        customItem(
            buttonGroupContent = {
                FeatureButton(
                    onClick = onBlockAllClick,
                    icon = R.drawable.icons8_block_120,
                    text = stringResource(id = R.string.block_all),
                    isSelected = selectedOption == BlockOption.BlockAll,
                    interactionSource = blockAllInteractionSource,
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(blockAllInteractionSource),
                )
            },
            menuContent = {},
        )

        customItem(
            buttonGroupContent = {
                FeatureButton(
                    onClick = onDailyLimitClick,
                    icon = R.drawable.icons8_timer_no_shadow_64,
                    text = stringResource(id = R.string.daily_limit),
                    isSelected = selectedOption == BlockOption.DailyLimit,
                    interactionSource = dailyLimitInteractionSource,
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(dailyLimitInteractionSource)
                        .onPlaced { onDailyLimitPlaced(it.boundsInRoot()) },
                )
            },
            menuContent = {},
        )

        customItem(
            buttonGroupContent = {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(intervalInteractionSource)
                        .fillMaxSize(),
                ) {
                    FeatureButton(
                        onClick = onIntervalTimerClick,
                        icon = R.drawable.icons8_stopwatch_no_shadow_64,
                        text = stringResource(id = R.string.time_interval),
                        isSelected = selectedOption == BlockOption.IntervalTimer,
                        interactionSource = intervalInteractionSource,
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (selectedOption == BlockOption.IntervalTimer) {
                        IntervalTimerPointer(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(y = 10.dp, x = (-10).dp),
                        )
                    }
                }
            },
            menuContent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeatureButton(
    onClick: () -> Unit,
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val finalModifier = if (!isEnabled) modifier.alpha(0.7f) else modifier

    val iconRotation = remember { Animatable(0f) }
    val iconScale = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            when (icon) {
                R.drawable.icons8_timer_no_shadow_64 -> {
                    iconRotation.snapTo(0f)
                    iconRotation.animateTo(
                        targetValue = 45f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                    )
                }

                R.drawable.icons8_stopwatch_no_shadow_64 -> {
                    iconRotation.snapTo(0f)
                    iconRotation.animateTo(15f, spring(stiffness = Spring.StiffnessHigh))
                    iconRotation.animateTo(-15f, spring(stiffness = Spring.StiffnessHigh))
                    iconRotation.animateTo(8f, spring(stiffness = Spring.StiffnessMedium))
                    iconRotation.animateTo(-8f, spring(stiffness = Spring.StiffnessMedium))
                    iconRotation.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                }

                R.drawable.icons8_block_120 -> {
                    iconScale.snapTo(1f)
                    iconScale.animateTo(1.25f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy))
                    iconScale.animateTo(1.0f, spring(stiffness = Spring.StiffnessLow))
                }
            }
        } else {
            iconRotation.snapTo(0f)
            iconScale.snapTo(1f)
        }
    }

    ToggleButton(
        checked = isSelected,
        onCheckedChange = { onClick() },
        modifier = finalModifier.fillMaxSize(),
        enabled = isEnabled,
        colors = ToggleButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shapes = ToggleButtonShapes(
            shape = MaterialTheme.shapes.large,
            pressedShape = MaterialTheme.shapes.extraLarge,
            checkedShape = MaterialTheme.shapes.extraLarge,
        ),
        interactionSource = interactionSource,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = icon),
                // The text below already labels the button
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer(
                        scaleX = iconScale.value,
                        scaleY = iconScale.value,
                        rotationZ = iconRotation.value,
                    ),
            )
            AutoResizingText(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun IntervalTimerPointer(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 42.dp, height = 16.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@DevicePreviews
@Composable
fun TodayBlockingControlsPreview() {
    ScrollessTheme(darkTheme = true) {
        Surface {
            TodayBlockingControls(
                uiState = HomeUiState(
                    blockOption = BlockOption.DailyLimit,
                    settings = BlockingSettings(dailyLimitMillis = 60 * 60 * 1000L),
                ),
                isBlockingActive = false,
                isPauseActive = false,
                pauseRemainingMillis = 0L,
                onBlockOptionSelected = {},
                onConfigureDailyLimit = {},
                onIntervalTimerClick = {},
                onIntervalTimerEdit = {},
                onPauseToggle = { _ -> },
            )
        }
    }
}

@DevicePreviews
@Composable
fun TodayBlockingIntervalTimerControlsPreview() {
    ScrollessTheme(darkTheme = true) {
        Surface {
            TodayBlockingControls(
                uiState = HomeUiState(
                    blockOption = BlockOption.IntervalTimer,
                    settings = BlockingSettings(
                        intervalAllowanceMillis = 5 * 60 * 1000L,
                        intervalLengthMillis = 60 * 60 * 1000L,
                    ),
                ),
                isBlockingActive = false,
                isPauseActive = true,
                pauseRemainingMillis = 0L,
                onBlockOptionSelected = {},
                onConfigureDailyLimit = {},
                onIntervalTimerClick = {},
                onIntervalTimerEdit = {},
                onPauseToggle = { _ -> },
            )
        }
    }
}
