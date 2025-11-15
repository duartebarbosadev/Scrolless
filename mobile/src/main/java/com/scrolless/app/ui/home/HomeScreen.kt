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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrolless.app.BuildConfig
import com.scrolless.app.R
import com.scrolless.app.accessibility.ScrollessBlockAccessibilityService
import com.scrolless.app.core.data.database.model.BlockOption
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.designsystem.theme.progressbar_green_use
import com.scrolless.app.designsystem.theme.progressbar_orange_use
import com.scrolless.app.designsystem.theme.progressbar_red_use
import com.scrolless.app.ui.home.components.AccessibilityExplainerBottomSheet
import com.scrolless.app.ui.home.components.AccessibilitySuccessBottomSheet
import com.scrolless.app.ui.home.components.AccessibilitySuccessBottomSheetPreview
import com.scrolless.app.ui.home.components.HelpDialog
import com.scrolless.app.ui.home.components.IntervalTimerDialog
import com.scrolless.app.ui.home.components.TimeLimitDialog
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.ui.tooling.DevicePreviews
import com.scrolless.app.ui.utils.formatMinutes
import com.scrolless.app.util.formatTime
import com.scrolless.app.util.isAccessibilityServiceEnabled
import com.scrolless.app.util.radialGradientScrim
import com.scrolless.app.util.requestAppReview
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber

private val DEFAULT_INTERVAL_BREAK_MILLIS = TimeUnit.MINUTES.toMillis(60)
private val DEFAULT_INTERVAL_ALLOWANCE_MILLIS = TimeUnit.MINUTES.toMillis(5)

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val featureComingSoonMessage = stringResource(R.string.feature_coming_soon)

    var showTimeLimitDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAccessibilityExplainer by remember { mutableStateOf(false) }
    var showAccessibilitySuccess by remember { mutableStateOf(false) }
    var debugBypassAccessibilityCheck by remember { mutableStateOf(false) }
    var showIntervalTimerDialog by remember { mutableStateOf(false) }
    var pendingIntervalBreak by remember { mutableStateOf(DEFAULT_INTERVAL_BREAK_MILLIS) }
    var pendingIntervalAllowance by remember { mutableStateOf(DEFAULT_INTERVAL_ALLOWANCE_MILLIS) }

    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestUiState by rememberUpdatedState(uiState)

    fun showAccessibilityExplainerPrompt(setWaitingForAccessibility: Boolean = true) {
        if (showAccessibilityExplainer) return
        Timber.d("Queuing accessibility explainer (setWaiting=%s)", setWaitingForAccessibility)
        if (setWaitingForAccessibility) {
            viewModel.setWaitingForAccessibility(true)
        }
        showAccessibilityExplainer = true
        if (!uiState.hasSeenAccessibilityExplainer) {
            viewModel.onAccessibilityExplainerShown()
        }
    }

    /**
     * Observe lifecycle resume events so we can react when the user returns from settings:
     * - If accessibility is now enabled, flip the success sheet on once.
     * - If it is still disabled while a block option is active (or first launch), re-open the explainer.
     */
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Timber.d("HomeScreen resumed")
                val isAccessibilityEnabled = context.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)
                if (isAccessibilityEnabled) {
                    if (showAccessibilityExplainer) {
                        Timber.i("Accessibility service enabled - showing success dialog")
                        showAccessibilityExplainer = false
                        showAccessibilitySuccess = true
                        viewModel.setWaitingForAccessibility(false)
                    }
                } else if (latestUiState.hasLoadedSettings) {
                    val hasBlockSelection = latestUiState.blockOption != BlockOption.NothingSelected
                    val hasSeenExplainer = latestUiState.hasSeenAccessibilityExplainer
                    if ((!hasSeenExplainer || hasBlockSelection) && !showAccessibilityExplainer) {
                        Timber.i(
                            "Accessibility service disabled on resume - auto showing explainer (firstLaunch=%s, hasBlock=%s)",
                            !hasSeenExplainer,
                            hasBlockSelection,
                        )
                        showAccessibilityExplainerPrompt()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.hasLoadedSettings, uiState.hasSeenAccessibilityExplainer, uiState.blockOption) {
        if (
            uiState.hasLoadedSettings &&
            !showAccessibilityExplainer &&
            !context.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)
        ) {
            when {
                !uiState.hasSeenAccessibilityExplainer -> {
                    Timber.i("First launch detected - showing accessibility explainer")
                    showAccessibilityExplainerPrompt(setWaitingForAccessibility = false)
                }

                uiState.blockOption != BlockOption.NothingSelected -> {
                    Timber.i("Block option selected while accessibility disabled - showing explainer")
                    showAccessibilityExplainerPrompt(setWaitingForAccessibility = false)
                }
            }
        }
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
        fun openIntervalConfig() {
            pendingIntervalBreak = uiState.intervalLength.takeIf { it > 0L } ?: DEFAULT_INTERVAL_BREAK_MILLIS
            pendingIntervalAllowance = uiState.timeLimit.takeIf { it > 0L } ?: DEFAULT_INTERVAL_ALLOWANCE_MILLIS
            showIntervalTimerDialog = true
        }

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
                    showAccessibilityExplainerPrompt()
                }
            },
            onConfigureDailyLimit = {
                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
                    Timber.d("Open TimeLimitDialog (debug bypass: %s)", shouldBypass)
                    showTimeLimitDialog = true
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer (daily limit).")
                    showAccessibilityExplainerPrompt()
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
            onIntervalTimerClick = {
                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
                    Timber.i("Interval timer clicked -> current=%s", uiState.blockOption)
                    if (uiState.blockOption == BlockOption.IntervalTimer) {
                        viewModel.onBlockOptionSelected(BlockOption.NothingSelected)
                    } else if (uiState.intervalLength == 0L || uiState.timeLimit == 0L) {
                        openIntervalConfig()
                    } else {
                        viewModel.onBlockOptionSelected(BlockOption.IntervalTimer)
                    }
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer (interval timer).")
                    showAccessibilityExplainerPrompt()
                }
            },
            onIntervalTimerEdit = {
                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
                    Timber.d("Interval timer edit requested")
                    openIntervalConfig()
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer (interval timer config).")
                    showAccessibilityExplainerPrompt()
                }
            },
            onPauseToggle = { shouldPause ->
                if (shouldPause) {
                    Timber.i("Pause clicked -> pausing blocking for 5 minutes")
                } else {
                    Timber.i("Pause clicked -> resuming blocking immediately")
                }
                viewModel.onPauseToggle(shouldPause)
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

    if (showIntervalTimerDialog) {
        IntervalTimerDialog(
            initialBreakMillis = pendingIntervalBreak,
            initialAllowanceMillis = pendingIntervalAllowance,
            onConfirm = { breakMillis, allowanceMillis ->
                Timber.i(
                    "Interval timer schedule saved: break=%d, allowance=%d",
                    breakMillis,
                    allowanceMillis,
                )
                showIntervalTimerDialog = false
                viewModel.onIntervalTimerConfigChange(breakMillis, allowanceMillis)
            },
            onDismiss = {
                Timber.d("Interval timer dialog dismissed")
                showIntervalTimerDialog = false
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
                viewModel.setWaitingForAccessibility(false)
            },
        )
    }

    if (showAccessibilitySuccess) {
        AccessibilitySuccessBottomSheet(
            onDismiss = {
                showAccessibilitySuccess = false
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    onBlockOptionSelected: (BlockOption) -> Unit,
    onConfigureDailyLimit: () -> Unit,
    onTimerOverlayToggled: (Boolean) -> Unit,
    onHelpClicked: () -> Unit,
    onReviewClicked: () -> Unit,
    onIntervalTimerClick: () -> Unit,
    onIntervalTimerEdit: () -> Unit,
    onPauseToggle: (Boolean) -> Unit,
    onProgressCardClicked: () -> Unit = {},
) {
    // Define weights outside of composition flow
    val WEIGHT_BASE = 1f
    val WEIGHT_EXPANDED = 1.2f
    val WEIGHT_SHRUNK = 0.9f

    val pauseRemainingMillis = rememberPauseRemainingTime(uiState.pauseUntilMillis)
    val isPauseActive = pauseRemainingMillis > 0L
    val hasActiveBlockOption = uiState.blockOption != BlockOption.NothingSelected

    Box(
        modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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
                blockOption = uiState.blockOption,
                progress = uiState.progress,
                currentUsage = uiState.currentUsage,
                intervalUsage = uiState.intervalUsage,
                timeLimit = uiState.timeLimit,
                intervalLength = uiState.intervalLength,
                intervalWindowStart = uiState.intervalWindowStart,
                onProgressCardClicked = onProgressCardClicked,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Define interaction sources for ALL buttons
            val blockAllInteractionSource = remember { MutableInteractionSource() }
            val dailyLimitInteractionSource = remember { MutableInteractionSource() }
            val intervalInteractionSource = remember { MutableInteractionSource() }

            val isBlockAllPressed by blockAllInteractionSource.collectIsPressedAsState()
            val isDailyLimitPressed by dailyLimitInteractionSource.collectIsPressedAsState()
            val isIntervalPressed by intervalInteractionSource.collectIsPressedAsState()

            // 2. Calculate Animated Weights (Float) based on interaction states
            val blockAllWeight by animateFloatAsState(
                targetValue = when {
                    isBlockAllPressed -> WEIGHT_EXPANDED
                    isDailyLimitPressed || isIntervalPressed -> WEIGHT_SHRUNK
                    else -> WEIGHT_BASE
                },
                animationSpec = tween(100), label = "blockAllWeight",
            )

            val dailyLimitWeight by animateFloatAsState(
                targetValue = when {
                    isDailyLimitPressed -> WEIGHT_EXPANDED
                    isBlockAllPressed || isIntervalPressed -> WEIGHT_SHRUNK
                    else -> WEIGHT_BASE
                },
                animationSpec = tween(100), label = "dailyLimitWeight",
            )

            val intervalWeight by animateFloatAsState(
                targetValue = when {
                    isIntervalPressed -> WEIGHT_EXPANDED
                    isBlockAllPressed || isDailyLimitPressed -> WEIGHT_SHRUNK
                    else -> WEIGHT_BASE
                },
                animationSpec = tween(100), label = "intervalWeight",
            )

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
                    Timber.i("IntervalTimer clicked from feature row")
                    onIntervalTimerClick()
                },
                // Pass sources
                blockAllInteractionSource = blockAllInteractionSource,
                dailyLimitInteractionSource = dailyLimitInteractionSource,
                intervalInteractionSource = intervalInteractionSource,
                // Pass animated weights to sync top row
                blockAllAnimatedWeight = blockAllWeight,
                dailyLimitAnimatedWeight = dailyLimitWeight,
                intervalAnimatedWeight = intervalWeight,
            )

            // 3. Smooth appearance for ConfigButton
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
                // Mimic ButtonGroup layout to sync width exactly using animated weights
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Invisible spacer mirroring "Block All" width behavior
                    Spacer(modifier = Modifier.weight(blockAllWeight))
                    Box(
                        modifier = Modifier
                            .weight(dailyLimitWeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        ConfigButton(
                            onClick = {
                                Timber.d("Open DailyLimit config button clicked")
                                onConfigureDailyLimit()
                            },
                            dailyLimitInteractionSource = dailyLimitInteractionSource,
                            blockAllInteractionSource = blockAllInteractionSource,
                            // Set fixed width to approximately half of the feature button width
                            modifier = Modifier
                                .fillMaxWidth(0.6f) // Occupy 60% of the Box's (Daily Limit Slot's) width
                                .align(Alignment.Center), // Center horizontally within the Box
                        )
                    }

                    // Invisible spacer mirroring "Interval Timer" width behavior
                    Spacer(
                        modifier = Modifier
                            .weight(intervalWeight),
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(
                    if (uiState.blockOption == BlockOption.IntervalTimer) 12.dp else 24.dp,
                ),
            )

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

            if (hasActiveBlockOption) {
                Spacer(modifier = Modifier.height(24.dp))

                PauseButton(
                    onTogglePause = onPauseToggle,
                    isPaused = isPauseActive,
                    remainingMillis = pauseRemainingMillis,
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
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
fun ConfigButton(
    onClick: () -> Unit,
    dailyLimitInteractionSource: MutableInteractionSource,
    blockAllInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    // Collect pressed state from both sources
    val isDailyLimitPressed by dailyLimitInteractionSource.collectIsPressedAsState()
    val isBlockAllPressed by blockAllInteractionSource.collectIsPressedAsState()

    // Wiggle if EITHER linked button is pressed
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
            contentDescription = stringResource(id = R.string.go_to_accessibility_settings),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun IntervalTimerSettingsCard(
    intervalLengthMillis: Long,
    allowanceMillis: Long,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
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

            Text(
                text = stringResource(R.string.interval_timer_card_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
            )

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

@Composable
private fun rememberIntervalRemainingTime(isRunning: Boolean, intervalLength: Long, windowStart: Long): Long {
    val isInspectionMode = LocalInspectionMode.current

    fun calculateRemaining(): Long {
        if (intervalLength <= 0L || windowStart <= 0L) return 0L
        val now = System.currentTimeMillis()
        val elapsed = now - windowStart
        if (elapsed < 0L) return intervalLength
        val remaining = intervalLength - elapsed
        return remaining.coerceAtLeast(0L)
    }

    var remaining by remember(isRunning, intervalLength, windowStart) {
        mutableLongStateOf(calculateRemaining())
    }

    LaunchedEffect(isRunning, intervalLength, windowStart, isInspectionMode) {
        if (!isRunning || intervalLength <= 0L || windowStart <= 0L || isInspectionMode) {
            remaining = calculateRemaining()
        } else {
            while (isActive) {
                val nextRemaining = calculateRemaining()
                remaining = nextRemaining
                if (nextRemaining <= 0L) break
                delay(1_000L)
            }
        }
    }

    return remaining
}

@Composable
private fun rememberPauseRemainingTime(pauseUntilMillis: Long): Long {
    val isInspectionMode = LocalInspectionMode.current

    fun calculateRemaining(): Long {
        if (pauseUntilMillis <= 0L) return 0L
        val delta = pauseUntilMillis - System.currentTimeMillis()
        return delta.coerceAtLeast(0L)
    }

    var remaining by remember(pauseUntilMillis) {
        mutableLongStateOf(calculateRemaining())
    }

    LaunchedEffect(pauseUntilMillis, isInspectionMode) {
        if (pauseUntilMillis <= 0L || isInspectionMode) {
            remaining = calculateRemaining()
        } else {
            while (isActive) {
                remaining = calculateRemaining()
                if (remaining <= 0L) break
                delay(1_000L)
            }
        }
    }

    return remaining
}

private fun Long.toIntervalLabel(): String {
    if (this <= 0L) return "--"
    val totalMinutes = (this / 60_000L).toInt()
    return totalMinutes.formatMinutes()
}

private fun Long.toCountdownLabel(): String {
    if (this <= 0L) return "0:00"
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeatureButtonsRow(
    selectedOption: BlockOption,
    onBlockAllClick: () -> Unit,
    onDailyLimitClick: () -> Unit,
    onIntervalTimerClick: () -> Unit,
    // Accept sources from parent
    blockAllInteractionSource: MutableInteractionSource,
    dailyLimitInteractionSource: MutableInteractionSource,
    intervalInteractionSource: MutableInteractionSource,
    // Accept animated weights
    blockAllAnimatedWeight: Float,
    dailyLimitAnimatedWeight: Float,
    intervalAnimatedWeight: Float,
) {
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
                    interactionSource = blockAllInteractionSource,
                    modifier = Modifier.weight(blockAllAnimatedWeight), // Using animated weight
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
                    interactionSource = dailyLimitInteractionSource,
                    modifier = Modifier.weight(dailyLimitAnimatedWeight), // Using animated weight
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
                        icon = R.drawable.icons8_stopwatch_64,
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

    ToggleButton(
        checked = isSelected,
        onCheckedChange = { onClick() },
        modifier = finalModifier.fillMaxSize(), // fillMaxSize respects the weight set by the caller
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
                modifier = Modifier.size(32.dp),
            )
            AutoResizingText(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                minFontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ProgressCard(
    blockOption: BlockOption,
    progress: Int,
    currentUsage: Long,
    intervalUsage: Long,
    timeLimit: Long,
    intervalLength: Long,
    intervalWindowStart: Long,
    onProgressCardClicked: () -> Unit = {},
) {
    val clampedProgress = progress.coerceIn(0, 100)

    val isIntervalMode = blockOption == BlockOption.IntervalTimer
    val intervalAllowanceConfigured = isIntervalMode && timeLimit > 0L
    val intervalRemainingMillis = if (isIntervalMode) {
        rememberIntervalRemainingTime(
            isRunning = intervalAllowanceConfigured && intervalLength > 0L && intervalWindowStart > 0L,
            intervalLength = intervalLength,
            windowStart = intervalWindowStart,
        )
    } else {
        0L
    }
    val intervalResetReady =
        intervalAllowanceConfigured &&
            intervalLength > 0L &&
            intervalWindowStart > 0L &&
            intervalRemainingMillis <= 1_000L
    val displayIntervalUsage = if (intervalResetReady) 0L else intervalUsage
    val displayProgress = if (intervalResetReady) 0 else clampedProgress

    val animatedProgress by animateFloatAsState(
        targetValue = displayProgress / 100f,
        animationSpec = if (LocalInspectionMode.current) tween(durationMillis = 1000) else tween(durationMillis = 1000),
        label = "progress",
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            displayProgress < 75 -> progressbar_green_use // Green
            displayProgress < 100 -> progressbar_orange_use // Orange
            else -> progressbar_red_use // Red
        },
        animationSpec = tween(durationMillis = 500),
        label = "color",
    )

    val primaryText = when {
        isIntervalMode && intervalAllowanceConfigured ->
            "${displayIntervalUsage.coerceAtLeast(0L).coerceAtMost(timeLimit).formatTime()} / ${timeLimit.formatTime()}"
        isIntervalMode -> displayIntervalUsage.formatTime()
        blockOption == BlockOption.DailyLimit && timeLimit > 0L ->
            "${currentUsage.formatTime()} / ${timeLimit.formatTime()}"
        else -> currentUsage.formatTime()
    }

    val resetText = if (isIntervalMode) {
        when {
            !intervalAllowanceConfigured -> stringResource(R.string.interval_timer_next_reset_unknown)
            intervalLength <= 0L || intervalWindowStart <= 0L -> stringResource(R.string.interval_timer_next_reset_unknown)
            intervalRemainingMillis <= 1_000L -> stringResource(R.string.interval_timer_next_reset_ready)
            else -> stringResource(
                R.string.interval_timer_next_reset_in,
                intervalRemainingMillis.formatTime(),
            )
        }
    } else {
        null
    }

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
                AutoResizingText(
                    text = primaryText,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minFontSize = 16.sp,
                )
                Spacer(Modifier.height(8.dp))
                AutoResizingText(
                    text = stringResource(R.string.time_wasted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minFontSize = 12.sp,
                )

                if (resetText != null) {
                    Spacer(Modifier.height(6.dp))
                    AutoResizingText(
                        text = resetText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        minFontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun PauseButton(onTogglePause: (Boolean) -> Unit, isPaused: Boolean, remainingMillis: Long, modifier: Modifier = Modifier) {
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
            modifier = Modifier
                .height(64.dp),
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
                stringResource(id = R.string.pause_duration_hint)
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
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
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
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
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
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
        )
    }
}

@Preview(name = "Interval Timer Selected")
@Composable
fun PreviewIntervalTimerSelected() {
    ScrollessTheme {
        HomeContent(
            uiState = HomeUiState(
                blockOption = BlockOption.IntervalTimer,
                timeLimit = TimeUnit.MINUTES.toMillis(5),
                intervalLength = TimeUnit.MINUTES.toMillis(60),
                intervalUsage = TimeUnit.MINUTES.toMillis(3),
                intervalWindowStart = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30),
                currentUsage = TimeUnit.MINUTES.toMillis(42),
            ),
            onBlockOptionSelected = {},
            onConfigureDailyLimit = {},
            onTimerOverlayToggled = {},
            onHelpClicked = {},
            onReviewClicked = {},
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
        )
    }
}

@Preview(name = "Interval Timer Active")
@Composable
fun PreviewIntervalTimer() {
    ScrollessTheme {
        HomeContent(
            uiState = HomeUiState(
                blockOption = BlockOption.IntervalTimer,
                timeLimit = TimeUnit.MINUTES.toMillis(5),
                intervalLength = TimeUnit.MINUTES.toMillis(60),
                intervalUsage = TimeUnit.MINUTES.toMillis(4),
                intervalWindowStart = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(45),
                currentUsage = TimeUnit.MINUTES.toMillis(50),
            ),
            onBlockOptionSelected = {},
            onConfigureDailyLimit = {},
            onTimerOverlayToggled = {},
            onHelpClicked = {},
            onReviewClicked = {},
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
        )
        IntervalTimerDialog(
            initialBreakMillis = TimeUnit.MINUTES.toMillis(60),
            initialAllowanceMillis = TimeUnit.MINUTES.toMillis(5),
            onConfirm = { _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview(name = "Help Dialog")
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
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
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
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
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
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
        )
        AccessibilitySuccessBottomSheetPreview()
    }
}
