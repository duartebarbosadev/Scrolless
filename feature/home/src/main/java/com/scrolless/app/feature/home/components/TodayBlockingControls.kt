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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.designsystem.util.toCountdownLabel
import com.scrolless.app.designsystem.util.toIntervalLabel
import com.scrolless.app.feature.home.HomeUiState
import com.scrolless.app.feature.home.R
import kotlinx.coroutines.delay
import timber.log.Timber

enum class BlockingButtonType { BLOCK_ALL, DAILY_LIMIT, INTERVAL }

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
    val weightBase = 1.0f
    val weightExpanded = 1.15f
    val weightShrunk = 0.65f
    val releaseDelay = 160L
    val pressAnimationSpec = spring<Float>(stiffness = 550f)

    var lastClicked by remember { mutableStateOf<BlockingButtonType?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    // Auto-releases the button expansion animation after releaseDelay
    LaunchedEffect(lastClicked) {
        if (lastClicked != null) {
            delay(releaseDelay)
            lastClicked = null
        }
    }

    // 1. Define interaction sources for ALL buttons
    val blockAllInteractionSource = remember { MutableInteractionSource() }
    val dailyLimitInteractionSource = remember { MutableInteractionSource() }
    val intervalInteractionSource = remember { MutableInteractionSource() }

    val isBlockAllPressed by blockAllInteractionSource.collectIsPressedAsState()
    val isDailyLimitPressed by dailyLimitInteractionSource.collectIsPressedAsState()
    val isIntervalPressed by intervalInteractionSource.collectIsPressedAsState()

    // Helper to evaluate target weight based on click/press states
    fun isPressedOrClicked(button: BlockingButtonType): Boolean = when (button) {
        BlockingButtonType.BLOCK_ALL -> isBlockAllPressed || lastClicked == BlockingButtonType.BLOCK_ALL
        BlockingButtonType.DAILY_LIMIT -> isDailyLimitPressed || lastClicked == BlockingButtonType.DAILY_LIMIT
        BlockingButtonType.INTERVAL -> isIntervalPressed || lastClicked == BlockingButtonType.INTERVAL
    }

    fun weightFor(button: BlockingButtonType): Float = when {
        isPressedOrClicked(button) -> weightExpanded
        BlockingButtonType.entries.any { isPressedOrClicked(it) } -> weightShrunk
        else -> weightBase
    }

    // 2. Calculate Animated Weights (Float) based on interaction and click states
    val blockAllWeight by animateFloatAsState(
        targetValue = weightFor(BlockingButtonType.BLOCK_ALL),
        animationSpec = pressAnimationSpec, label = "blockAllWeight",
    )

    val dailyLimitWeight by animateFloatAsState(
        targetValue = weightFor(BlockingButtonType.DAILY_LIMIT),
        animationSpec = pressAnimationSpec, label = "dailyLimitWeight",
    )

    val intervalWeight by animateFloatAsState(
        targetValue = weightFor(BlockingButtonType.INTERVAL),
        animationSpec = pressAnimationSpec, label = "intervalWeight",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 320)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FeatureButtonsRow(
            selectedOption = uiState.blockOption,
            onBlockAllClick = {
                lastClicked = BlockingButtonType.BLOCK_ALL
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val newOption = if (uiState.blockOption == BlockOption.BlockAll) {
                    BlockOption.NothingSelected
                } else {
                    BlockOption.BlockAll
                }
                Timber.i("BlockAll clicked -> newOption=%s (prev=%s)", newOption, uiState.blockOption)
                onBlockOptionSelected(newOption)
            },
            onDailyLimitClick = {
                lastClicked = BlockingButtonType.DAILY_LIMIT
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (uiState.timeLimit == 0L && uiState.blockOption != BlockOption.DailyLimit) {
                    Timber.d("DailyLimit clicked -> open TimeLimitDialog (no limit set)")
                    onConfigureDailyLimit()
                } else {
                    val newOption = if (uiState.blockOption == BlockOption.DailyLimit) {
                        BlockOption.NothingSelected
                    } else {
                        BlockOption.DailyLimit
                    }
                    Timber.i("DailyLimit clicked -> newOption=%s (prev=%s)", newOption, uiState.blockOption)
                    onBlockOptionSelected(newOption)
                }
            },
            onIntervalTimerClick = {
                lastClicked = BlockingButtonType.INTERVAL
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                Timber.i("IntervalTimer clicked from feature row")
                onIntervalTimerClick()
            },
            blockAllInteractionSource = blockAllInteractionSource,
            dailyLimitInteractionSource = dailyLimitInteractionSource,
            intervalInteractionSource = intervalInteractionSource,
            blockAllAnimatedWeight = blockAllWeight,
            dailyLimitAnimatedWeight = dailyLimitWeight,
            intervalAnimatedWeight = intervalWeight,
        )

        AnimatedVisibility(
            visible = uiState.blockOption == BlockOption.DailyLimit,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(300),
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(300),
            ) + fadeOut(animationSpec = tween(200)),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                ConfigButton(
                    onClick = {
                        Timber.d("Open DailyLimit config button clicked")
                        onConfigureDailyLimit()
                    },
                    dailyLimitInteractionSource = dailyLimitInteractionSource,
                    blockAllInteractionSource = blockAllInteractionSource,
                    modifier = Modifier.fillMaxWidth(0.2f),
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
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(300),
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(300),
            ) + fadeOut(animationSpec = tween(200)),
        ) {
            IntervalTimerSettingsCard(
                intervalLengthMillis = uiState.intervalLength,
                allowanceMillis = uiState.timeLimit,
                onEditClick = onIntervalTimerEdit,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (uiState.blockOption == BlockOption.IntervalTimer) {
            Spacer(modifier = Modifier.height(24.dp))
        }

        AnimatedVisibility(
            visible = isBlockingActive || isPauseActive,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(300),
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(300),
            ) + fadeOut(animationSpec = tween(200)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))

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
fun ConfigButton(
    onClick: () -> Unit,
    dailyLimitInteractionSource: MutableInteractionSource,
    blockAllInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    // Collect pressed state from both sources
    val isDailyLimitPressed by dailyLimitInteractionSource.collectIsPressedAsState()
    val isBlockAllPressed by blockAllInteractionSource.collectIsPressedAsState()

    // Wiggle if EITHER linked button is actively pressed
    val isPressed = isDailyLimitPressed || isBlockAllPressed

    // Fast tweens for visual feedback
    val animationSpec = tween<Float>(durationMillis = 100)
    val colorAnimationSpec = tween<Color>(durationMillis = 100)

    val bottomCorner by animateFloatAsState(
        targetValue = if (isPressed) 24f else 16f,
        animationSpec = animationSpec,
        label = "configButtonCorner",
    )

    val baseColor = MaterialTheme.colorScheme.surface
    val pressedColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)

    val containerColor by animateColorAsState(
        targetValue = if (isPressed) pressedColor else baseColor,
        animationSpec = colorAnimationSpec,
        label = "configButtonColor",
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        // Use internal source to prevent default press overlay, since we handle styling externally
        interactionSource = remember { MutableInteractionSource() },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
        ),
        shape = RoundedCornerShape(0.dp, 0.dp, bottomCorner.dp, bottomCorner.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.icons8_control_48),
            contentDescription = stringResource(id = R.string.daily_limit_configure_time_content_description),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        )
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
        shape = RoundedCornerShape(24.dp),
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
        shape = RoundedCornerShape(20.dp),
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

@Composable
fun PauseButton(
    modifier: Modifier = Modifier,
    onTogglePause: (Boolean) -> Unit,
    isPaused: Boolean,
    remainingMillis: Long,
    pauseDurationMinutes: Int = 5,
) {
    val buttonShape = RoundedCornerShape(20.dp)
    val containerColor by animateColorAsState(
        targetValue = if (isPaused) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        label = "pauseButtonContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isPaused) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        label = "pauseButtonContent",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPaused) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        },
        label = "pauseButtonBorder",
    )

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
        Button(
            onClick = { onTogglePause(!isPaused) },
            shape = buttonShape,
            border = BorderStroke(1.dp, borderColor),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
            modifier = Modifier.height(64.dp),
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = buttonLabel,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = buttonLabel,
                style = MaterialTheme.typography.titleMedium,
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
    blockAllAnimatedWeight: Float,
    dailyLimitAnimatedWeight: Float,
    intervalAnimatedWeight: Float,
    modifier: Modifier = Modifier,
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
                    contentDescription = stringResource(id = R.string.block_all),
                    isSelected = selectedOption == BlockOption.BlockAll,
                    interactionSource = blockAllInteractionSource,
                    modifier = Modifier.weight(blockAllAnimatedWeight),
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
                    contentDescription = stringResource(id = R.string.daily_limit),
                    isSelected = selectedOption == BlockOption.DailyLimit,
                    interactionSource = dailyLimitInteractionSource,
                    modifier = Modifier.weight(dailyLimitAnimatedWeight),
                )
            },
            menuContent = {},
        )

        customItem(
            buttonGroupContent = {
                Box(
                    modifier = Modifier
                        .weight(intervalAnimatedWeight)
                        .fillMaxSize(),
                ) {
                    FeatureButton(
                        onClick = onIntervalTimerClick,
                        icon = R.drawable.icons8_stopwatch_no_shadow_64,
                        text = stringResource(id = R.string.time_interval),
                        contentDescription = stringResource(id = R.string.time_interval),
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
    contentDescription: String,
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
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shapes = ToggleButtonShapes(
            shape = RoundedCornerShape(16.dp),
            pressedShape = RoundedCornerShape(24.dp),
            checkedShape = RoundedCornerShape(24.dp),
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
                contentDescription = contentDescription,
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
                uiState = HomeUiState(blockOption = BlockOption.DailyLimit),
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
                uiState = HomeUiState(blockOption = BlockOption.IntervalTimer),
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
