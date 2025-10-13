/*
 * Copyright (C) 2025 Scrolless
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
package com.scrolless.app.ui.home

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrolless.app.BuildConfig
import com.scrolless.app.R
import com.scrolless.app.accessibility.ScrollessBlockAccessibilityService
import com.scrolless.app.core.data.database.model.BlockOption
import com.scrolless.app.designsystem.theme.progressbar_green_use
import com.scrolless.app.designsystem.theme.progressbar_orange_use
import com.scrolless.app.designsystem.theme.progressbar_red_use
import com.scrolless.app.ui.home.components.AccessibilityExplainerBottomSheet
import com.scrolless.app.ui.home.components.AccessibilitySuccessBottomSheetPreview
import com.scrolless.app.ui.home.components.HelpDialog
import com.scrolless.app.ui.home.components.TimeLimitDialog
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.ui.tooling.DevicePreviews
import com.scrolless.app.util.formatTime
import com.scrolless.app.util.isAccessibilityServiceEnabled
import com.scrolless.app.util.radialGradientScrim
import com.scrolless.app.util.requestAppReview
import timber.log.Timber

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val featureComingSoonMessage = stringResource(R.string.feature_coming_soon)

    var showTimeLimitDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAccessibilityExplainer by remember { mutableStateOf(false) }
    var debugBypassAccessibilityCheck by remember { mutableStateOf(false) }

    val activity = context as? Activity

    LaunchedEffect(Unit) {
        Timber.d("HomeScreen composed")
    }

    // Show snackbar when needed
    LaunchedEffect(uiState.showComingSoonSnackBar) {
        if (uiState.showComingSoonSnackBar) {
            Timber.i("Showing 'feature coming soon' snackbar")
            snackbarHostState.showSnackbar(featureComingSoonMessage)
            viewModel.onSnackbarShown()
        }
    }

    HomeBackground(modifier = modifier.fillMaxSize()) {
        HomeContent(
            modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            uiState = uiState,
            onBlockOptionSelected = { blockOption ->
                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
                    Timber.i("Block option click -> %s (debug bypass: %s)", blockOption, shouldBypass)
                    viewModel.onBlockOptionSelected(blockOption)
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer.")
                    showAccessibilityExplainer = true
                }
            },
            onConfigureDailyLimit = {
                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
                    Timber.d("Open TimeLimitDialog (debug bypass: %s)", shouldBypass)
                    showTimeLimitDialog = true
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer (daily limit).")
                    showAccessibilityExplainer = true
                }
            },
            onTimerOverlayToggled = { enabled ->
                Timber.d("Timer overlay toggle from UI: %s", enabled)
                viewModel.onTimerOverlayToggled(enabled)
            },
            onHelpClicked = {
                Timber.d("Help clicked -> show HelpDialog")
                showHelpDialog = true
            },
            onReviewClicked = {
                Timber.i("Review button clicked")
                viewModel.onReviewRequested()
            },
            onPauseClicked = {
                Timber.i("Pause clicked (coming soon)")
                viewModel.onFeatureComingSoon()
            },
            onProgressCardClicked = {
                if (BuildConfig.DEBUG) {
                    debugBypassAccessibilityCheck = !debugBypassAccessibilityCheck
                    Timber.i("Progress card clicked - Debug bypass mode: %s", debugBypassAccessibilityCheck)
                } else {
                    Timber.d("Progress card clicked (no action in release build)")
                }
            },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    // Show dialog when needed
    if (showTimeLimitDialog) {
        TimeLimitDialog(
            onDismiss = { selectedSeconds ->
                Timber.d("TimeLimitDialog dismissed: selected=%d s", selectedSeconds)
                showTimeLimitDialog = false
                if (selectedSeconds > 0) {
                    Timber.i("Setting time limit to %d seconds", selectedSeconds)
                    viewModel.onTimeLimitChange(selectedSeconds * 1000) // Convert to millis
                }
            },
        )
    }

    if (showHelpDialog) {
        HelpDialog(
            onDismiss = {
                Timber.d("HelpDialog dismissed")
                showHelpDialog = false
            },
        )
    }

    if (showAccessibilityExplainer) {
        Timber.d("Showing AccessibilityExplainerBottomSheet")
        AccessibilityExplainerBottomSheet(
            onDismiss = {
                Timber.d("AccessibilityExplainer: Dismiss from home screen")
                showAccessibilityExplainer = false
            },
        )
    }

    LaunchedEffect(uiState.requestReview) {
        if (uiState.requestReview && activity != null) {
            Timber.i("Requesting in-app review")
            requestAppReview(activity)
            viewModel.onReviewRequestHandled()
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    onBlockOptionSelected: (BlockOption) -> Unit,
    onConfigureDailyLimit: () -> Unit,
    onTimerOverlayToggled: (Boolean) -> Unit,
    onHelpClicked: () -> Unit,
    onReviewClicked: () -> Unit,
    onPauseClicked: () -> Unit,
    onProgressCardClicked: () -> Unit = {},
) {
    Box(
        modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Help Button with padding
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                HelpButton(onClick = onHelpClicked)
            }

            ProgressCard(
                progress = uiState.progress,
                currentUsage = uiState.currentUsage,
                timeLimit = uiState.timeLimit,
                showTimeLimit = uiState.blockOption == BlockOption.DailyLimit,
                onProgressCardClicked = onProgressCardClicked,
            )

            Spacer(modifier = Modifier.height(8.dp))

            FeatureButtonsRow(
                selectedOption = uiState.blockOption,
                onBlockAllClick = {
                    val newOption = if (uiState.blockOption == BlockOption.BlockAll) {
                        BlockOption.NothingSelected
                    } else {
                        BlockOption.BlockAll
                    }
                    Timber.i("BlockAll clicked -> newOption=%s (prev=%s)", newOption, uiState.blockOption)
                    onBlockOptionSelected(newOption)
                },
                onDailyLimitClick = {
                    if (uiState.timeLimit == 0L && uiState.blockOption != BlockOption.DailyLimit) {
                        Timber.d("DailyLimit clicked -> open TimeLimitDialog (no limit set)")
                        // If no time limit set, open the picker
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
                    Timber.i("IntervalTimer clicked (feature not implemented)")
                    onPauseClicked()
                }, // Feature not implemented yet
            )

            if (uiState.blockOption == BlockOption.DailyLimit) {
                ConfigButton(
                    onClick = {
                        Timber.d("Open DailyLimit config button clicked")
                        onConfigureDailyLimit()
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PauseButton(onClick = onPauseClicked)

            Spacer(modifier = Modifier.height(24.dp))
            TimerOverlayToggle(
                checked = uiState.timerOverlayEnabled,
                onCheckedChange = onTimerOverlayToggled,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            RateButton(onClick = onReviewClicked)
        }
    }
}

@Composable
fun ConfigButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .wrapContentWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.icons8_control_48),
            contentDescription = stringResource(id = R.string.go_to_accessibility_settings), // TODO Fix this string
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun PauseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(60.dp)
            .wrapContentWidth()
            .alpha(0.7f),

        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = (0.7f)),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_pause),
            contentDescription = stringResource(id = R.string.pause),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.pause),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun TimerOverlayToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    Timber.d("Timer overlay row click -> toggle to %s", !checked)
                    onCheckedChange(!checked)
                },
            )
            .wrapContentWidth()
            .padding(8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.show_timer_overlay),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = {
                Timber.d("Timer overlay switch toggled -> %s", it)
                onCheckedChange(it)
            },
        )
    }
}

@Composable
private fun RateButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
        ),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_rate_review),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.rate_scrolless),
            color = MaterialTheme.colorScheme.onSecondary,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeatureButtonsRow(
    selectedOption: BlockOption,
    onBlockAllClick: () -> Unit,
    onDailyLimitClick: () -> Unit,
    onIntervalTimerClick: () -> Unit,
) {
    val interactionSource1 = remember { MutableInteractionSource() }
    val interactionSource2 = remember { MutableInteractionSource() }
    val interactionSource3 = remember { MutableInteractionSource() }

    ButtonGroup(
        overflowIndicator = {},
        modifier = Modifier
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
                    interactionSource = interactionSource1,
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(interactionSource = interactionSource1),
                )
            },
            menuContent = {},
        )

        customItem(
            buttonGroupContent = {
                FeatureButton(
                    onClick = onDailyLimitClick,
                    icon = R.drawable.icons8_timer_64,
                    text = stringResource(id = R.string.daily_limit),
                    contentDescription = stringResource(id = R.string.daily_limit),
                    isSelected = selectedOption == BlockOption.DailyLimit,
                    interactionSource = interactionSource2,
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(interactionSource = interactionSource2),
                )
            },
            menuContent = {},
        )

        customItem(
            buttonGroupContent = {
                FeatureButton(
                    onClick = onIntervalTimerClick,
                    icon = R.drawable.icons8_stopwatch_64,
                    text = stringResource(id = R.string.interval_timer),
                    contentDescription = stringResource(id = R.string.interval_timer),
                    isSelected = selectedOption == BlockOption.IntervalTimer,
                    isEnabled = false,
                    interactionSource = interactionSource3,
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(interactionSource = interactionSource3),
                )
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
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = text,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun ProgressCard(
    progress: Int,
    currentUsage: Long,
    timeLimit: Long,
    showTimeLimit: Boolean,
    onProgressCardClicked: () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = if (LocalInspectionMode.current) snap() else tween(durationMillis = 1000),
        label = "progress",
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            progress < 75 -> progressbar_green_use // Green
            progress < 100 -> progressbar_orange_use // Orange
            else -> progressbar_red_use // Red
        },
        animationSpec = tween(durationMillis = 500),
        label = "color",
    )

    Card(
        modifier = Modifier
            .size(220.dp)
            .padding(16.dp)
            .clickable(onClick = onProgressCardClicked),
        shape = RoundedCornerShape(96.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(180.dp),
                color = progressColor,
                strokeWidth = 8.dp,
                trackColor = MaterialTheme.colorScheme.primary,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = if (showTimeLimit) {
                        "${currentUsage.formatTime()} / ${timeLimit.formatTime()}"
                    } else {
                        currentUsage.formatTime()
                    },
                    modifier = Modifier.padding(top = 16.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.total_time_wasted),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_help_outline_24),
            contentDescription = stringResource(R.string.cd_add),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.help),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

@Composable
private fun HomeBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .radialGradientScrim(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        )
        content()
    }
}

@DevicePreviews
@Composable
fun HomeScreenPreview() {
    val mockState = HomeUiState(
        blockOption = BlockOption.DailyLimit,
        timeLimit = 7200000L, // 2 hours
        currentUsage = 5400000L, // 1.5 hours
        progress = 75,
        timerOverlayEnabled = true,
    )

    ScrollessTheme {
        HomeContent(
            uiState = mockState,
            onBlockOptionSelected = {},
            onConfigureDailyLimit = {},
            onTimerOverlayToggled = {},
            onHelpClicked = {},
            onReviewClicked = {},
            onPauseClicked = {},
        )
    }
}

@Preview(name = "Block All Active")
@Composable
fun PreviewBlockAll() {
    ScrollessTheme {
        HomeContent(
            uiState = HomeUiState(blockOption = BlockOption.BlockAll),
            onBlockOptionSelected = {},
            onConfigureDailyLimit = {},
            onTimerOverlayToggled = {},
            onHelpClicked = {},
            onReviewClicked = {},
            onPauseClicked = {},
        )
    }
}

@Preview(name = "Nothing Selected")
@Composable
fun PreviewNothingSelected() {
    ScrollessTheme {
        HomeContent(
            uiState = HomeUiState(blockOption = BlockOption.NothingSelected, currentUsage = 3590000L),
            onBlockOptionSelected = {},
            onConfigureDailyLimit = {},
            onTimerOverlayToggled = {},
            onHelpClicked = {},
            onReviewClicked = {},
            onPauseClicked = {},
        )
    }
}

@Preview(name = "Interval Timer Active")
@Composable
fun PreviewIntervalTimer() {
    ScrollessTheme {
        HomeContent(
            uiState = HomeUiState(blockOption = BlockOption.DailyLimit),
            onBlockOptionSelected = {},
            onConfigureDailyLimit = {},
            onTimerOverlayToggled = {},
            onHelpClicked = {},
            onReviewClicked = {},
            onPauseClicked = {},
        )
        TimeLimitDialog { onDismissedSeconds -> {} }
    }
}

@Preview(name = "Interval Timer Active")
@Composable
fun PreviewHelpDialog() {
    ScrollessTheme {
        HomeContent(
            uiState = HomeUiState(blockOption = BlockOption.BlockAll),
            onBlockOptionSelected = {},
            onConfigureDailyLimit = {},
            onTimerOverlayToggled = {},
            onHelpClicked = {},
            onReviewClicked = {},
            onPauseClicked = {},
        )
        HelpDialog { }
    }
}

@Preview(name = "Accessibility Explainer")
@Composable
fun PreviewAccessibilityExplainer() {
    ScrollessTheme {
        HomeContent(
            uiState = HomeUiState(blockOption = BlockOption.NothingSelected),
            onBlockOptionSelected = {},
            onConfigureDailyLimit = {},
            onTimerOverlayToggled = {},
            onHelpClicked = {},
            onReviewClicked = {},
            onPauseClicked = {},
        )
        AccessibilityExplainerBottomSheet { }
    }
}

@Preview(name = "Accessibility success dialog")
@Composable
fun PreviewAccessibilitySuccessDialog() {
    ScrollessTheme {
        HomeContent(
            uiState = HomeUiState(blockOption = BlockOption.NothingSelected),
            onBlockOptionSelected = {},
            onConfigureDailyLimit = {},
            onTimerOverlayToggled = {},
            onHelpClicked = {},
            onReviewClicked = {},
            onPauseClicked = {},
        )
        AccessibilitySuccessBottomSheetPreview()
    }
}
