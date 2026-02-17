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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.designsystem.component.AppUsageLegend
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.designsystem.component.LegendItem
import com.scrolless.app.designsystem.component.ProgressBarSegment
import com.scrolless.app.designsystem.component.SegmentedCircularProgressIndicator
import com.scrolless.app.designsystem.theme.instagramReelsColor
import com.scrolless.app.designsystem.theme.progressbar_green_use
import com.scrolless.app.designsystem.theme.progressbar_orange_use
import com.scrolless.app.designsystem.theme.progressbar_red_use
import com.scrolless.app.designsystem.theme.tiktokColor
import com.scrolless.app.designsystem.theme.youtubeShortsColor
import com.scrolless.app.ui.home.components.AccessibilityExplainerBottomSheet
import com.scrolless.app.ui.home.components.AccessibilitySuccessBottomSheet
import com.scrolless.app.ui.home.components.AccessibilitySuccessBottomSheetPreview
import com.scrolless.app.ui.home.components.FloatingDebugUsagePanel
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
import java.time.LocalDateTime
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
    var pendingIntervalBreak by remember { mutableLongStateOf(DEFAULT_INTERVAL_BREAK_MILLIS) }
    var pendingIntervalAllowance by remember { mutableLongStateOf(DEFAULT_INTERVAL_ALLOWANCE_MILLIS) }
    val pauseRemainingMillis = rememberPauseRemainingTime(uiState.pauseUntilMillis)
    val isPauseActive = pauseRemainingMillis > 0L

    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestUiState by rememberUpdatedState(uiState)

    fun showAccessibilityExplainerPrompt() {
        if (showAccessibilityExplainer) return
        Timber.d("Set waiting for accessibility for app auto open")
        viewModel.setWaitingForAccessibility(true)
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
                    showAccessibilityExplainerPrompt()
                }

                uiState.blockOption != BlockOption.NothingSelected -> {
                    Timber.i("Block option selected while accessibility disabled - showing explainer")
                    showAccessibilityExplainerPrompt()
                }

                uiState.timerOverlayEnabled -> {
                    Timber.i("Timer overlay ON while accessibility disabled - showing explainer")
                    showAccessibilityExplainerPrompt()
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

    val hasLimitTimer =
        (uiState.blockOption == BlockOption.DailyLimit && uiState.timeLimit > 0L) ||
            (uiState.blockOption == BlockOption.IntervalTimer && uiState.timeLimit > 0L)
    val limitProgressFraction = if (hasLimitTimer) {
        uiState.progress.coerceIn(0, 100) / 100f
    } else {
        0f
    }
    val limitAccentColor by animateColorAsState(
        targetValue = when {
            uiState.progress < 50 -> progressbar_green_use
            uiState.progress < 100 -> progressbar_orange_use
            else -> progressbar_red_use
        },
        animationSpec = tween(durationMillis = 900),
        label = "limitAccentColor",
    )
    val shouldShowHealthyBackground = uiState.blockOption == BlockOption.BlockAll && !isPauseActive
    val backgroundAccentTargetColor = when {
        isPauseActive -> progressbar_orange_use
        shouldShowHealthyBackground -> progressbar_green_use
        hasLimitTimer -> limitAccentColor
        else -> Color.Transparent
    }
    val backgroundAccentColor by animateColorAsState(
        targetValue = backgroundAccentTargetColor,
        animationSpec = tween(durationMillis = 900),
        label = "backgroundAccentColor",
    )
    val backgroundAccentStrength by animateFloatAsState(
        targetValue = when {
            isPauseActive -> 1f
            shouldShowHealthyBackground -> 1f
            hasLimitTimer -> limitProgressFraction
            else -> 0f
        },
        animationSpec = tween(durationMillis = 900),
        label = "backgroundAccentStrength",
    )

    HomeBackground(
        modifier = modifier.fillMaxSize(),
        accentColor = backgroundAccentColor,
        accentStrength = backgroundAccentStrength,
    ) {
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
            onScreenTimerToggled = { enabled ->
                Timber.d("On-screen timer toggle from UI: %s", enabled)
                if (context.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
                    viewModel.onScreenTimerToggled(enabled)
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer (on-screen timer).")
                    showAccessibilityExplainerPrompt()
                }
            },
            onHelpClicked = {
                Timber.d("Help clicked -> show HelpDialog")
                showHelpDialog = true
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

                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
                    if (shouldPause) {
                        Timber.i("Pause clicked -> pausing blocking for 5 minutes")
                    } else {
                        Timber.i("Pause clicked -> resuming blocking immediately")
                    }
                    viewModel.onPauseToggle(shouldPause)
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer (pause).")
                    showAccessibilityExplainerPrompt()
                }
            },
            onDebugUsageChanged = { usageSegments ->
                viewModel.onDebugUsageSegmentsChanged(usageSegments)
            },
            onDebugUsageReset = {
                viewModel.onDebugResetUsage()
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
            viewModel.onReviewRequestStarted()
            requestAppReview(activity) { result ->
                viewModel.onReviewPromptResult(result)
                viewModel.onReviewRequestHandled()
            }
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
    onScreenTimerToggled: (Boolean) -> Unit,
    onHelpClicked: () -> Unit,
    onIntervalTimerClick: () -> Unit,
    onIntervalTimerEdit: () -> Unit,
    onPauseToggle: (Boolean) -> Unit,
    onDebugUsageChanged: (List<SessionSegment>) -> Unit = {},
    onDebugUsageReset: () -> Unit = {},
    onProgressCardClicked: () -> Unit = {},
) {
    // Define weights outside of composition flow
    val WEIGHT_BASE = 1f
    val WEIGHT_EXPANDED = 1.2f
    val WEIGHT_SHRUNK = 0.9f

    val pauseRemainingMillis = rememberPauseRemainingTime(uiState.pauseUntilMillis)
    val isPauseActive = pauseRemainingMillis > 0L
    val hasActiveBlockOption = uiState.blockOption != BlockOption.NothingSelected
    val showDebugPanel = BuildConfig.DEBUG || LocalInspectionMode.current
    var isDebugExpanded by remember { mutableStateOf(false) }

    // Determine if blocking is currently active (user would be blocked if they tried to view content)
    val isBlockingActive = when (uiState.blockOption) {
        BlockOption.BlockAll -> true // Always blocking
        BlockOption.DailyLimit -> uiState.timeLimit > 0 && uiState.currentUsage >= uiState.timeLimit
        BlockOption.IntervalTimer -> uiState.timeLimit > 0 && uiState.intervalUsage >= uiState.timeLimit
        BlockOption.NothingSelected -> false
    }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ProgressCard(
                    modifier = Modifier.padding(top = 18.dp),
                    blockOption = uiState.blockOption,
                    progress = uiState.progress,
                    currentUsage = uiState.currentUsage,
                    intervalUsage = uiState.intervalUsage,
                    timeLimit = uiState.timeLimit,
                    intervalLength = uiState.intervalLength,
                    intervalWindowStart = uiState.intervalWindowStart,
                    listSessionSegments = uiState.listSessionSegments,
                    onProgressCardClicked = onProgressCardClicked,
                )

                HelpButton(
                    onClick = onHelpClicked,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }

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

            if (uiState.blockOption == BlockOption.IntervalTimer) {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Show pause button only when blocking is active OR user is already paused (to allow resuming)
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
                Spacer(modifier = Modifier.height(24.dp))

                PauseButton(
                    onTogglePause = onPauseToggle,
                    isPaused = isPauseActive,
                    remainingMillis = pauseRemainingMillis,
                )
            }

            if (hasActiveBlockOption) {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Show timer overlay toggle when not BlockAll, or when paused (since user can watch content while paused)
            AnimatedVisibility(
                visible = uiState.blockOption != BlockOption.BlockAll || isPauseActive,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(300),
                ) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(300),
                ) + fadeOut(animationSpec = tween(200)),
            ) {
                OnScreenTimerToggle(
                    checked = uiState.timerOverlayEnabled,
                    onCheckedChange = onScreenTimerToggled,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }

        if (showDebugPanel) {
            FloatingDebugUsagePanel(
                sessionSegments = uiState.listSessionSegments,
                isExpanded = isDebugExpanded,
                onToggleExpanded = { isDebugExpanded = !isDebugExpanded },
                onUsageChanged = onDebugUsageChanged,
                onReset = {
                    onDebugUsageReset()
                },
                modifier = Modifier.fillMaxSize(),
            )
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

// Todo refactor this out
private fun Long.toIntervalLabel(): String {
    if (this <= 0L) return "--"
    val totalMinutes = (this / 60_000L).toInt()
    return totalMinutes.formatMinutes()
}

// Todo refactor this out
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
@OptIn(ExperimentalFoundationApi::class)
private fun ProgressCard(
    modifier: Modifier = Modifier,
    blockOption: BlockOption,
    progress: Int,
    currentUsage: Long,
    intervalUsage: Long,
    timeLimit: Long,
    intervalLength: Long,
    intervalWindowStart: Long,
    listSessionSegments: List<SessionSegment> = emptyList(),
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

    val primaryText = when {
        isIntervalMode -> displayIntervalUsage.formatTime()
        else -> currentUsage.formatTime()
    }
    val limitChipText = when {
        isIntervalMode && intervalAllowanceConfigured -> timeLimit.formatTime()
        blockOption == BlockOption.DailyLimit && timeLimit > 0L -> timeLimit.formatTime()
        else -> null
    }

    val resetText = if (isIntervalMode) {
        when {
            !intervalAllowanceConfigured -> null
            intervalLength <= 0L || intervalWindowStart <= 0L -> null
            intervalRemainingMillis <= 1_000L -> stringResource(R.string.interval_timer_next_reset_ready)
            else -> stringResource(
                R.string.interval_timer_next_reset_in,
                intervalRemainingMillis.formatTime(),
            )
        }
    } else {
        null
    }

    val reelsLabel = stringResource(R.string.app_reels)
    val tiktokLabel = stringResource(R.string.app_tiktok)
    val shortsLabel = stringResource(R.string.app_shorts)

    // Per-app usage data for the segmented progress indicator
    val progressBarSegments = remember(listSessionSegments, currentUsage, reelsLabel, tiktokLabel, shortsLabel) {
        buildProgressBarSegments(
            sessionSegments = listSessionSegments,
            currentUsage = currentUsage,
            reelsLabel = reelsLabel,
            tiktokLabel = tiktokLabel,
            shortsLabel = shortsLabel,
        )
    }
    val legendItems = remember(progressBarSegments) { buildLegendItems(progressBarSegments) }

    val segmentProgressFraction = when {
        blockOption == BlockOption.DailyLimit && timeLimit > 0L -> displayProgress / 100f
        blockOption == BlockOption.IntervalTimer && intervalAllowanceConfigured -> displayProgress / 100f
        progressBarSegments.isNotEmpty() -> 1f
        else -> 0f
    }

    val isLimitReached = when (blockOption) {
        BlockOption.DailyLimit -> timeLimit in 1..currentUsage
        BlockOption.IntervalTimer -> timeLimit in 1..intervalUsage && !intervalResetReady
        else -> false
    }
    val limitChipBackground by animateColorAsState(
        targetValue = if (isLimitReached) {
            progressbar_red_use.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
        },
        animationSpec = tween(durationMillis = 600),
        label = "limitChipBackground",
    )
    val limitChipBorderColor by animateColorAsState(
        targetValue = if (isLimitReached) {
            progressbar_red_use.copy(alpha = 0.7f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 600),
        label = "limitChipBorderColor",
    )
    val limitChipTextColor by animateColorAsState(
        targetValue = if (isLimitReached) {
            progressbar_red_use
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
        },
        animationSpec = tween(durationMillis = 600),
        label = "limitChipTextColor",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .size(220.dp)
                .padding(16.dp)
                .clickable(
                    onClick = onProgressCardClicked,
                ),
            shape = RoundedCornerShape(96.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                SegmentedCircularProgressIndicator(
                    modifier = Modifier.size(180.dp),
                    segments = progressBarSegments,
                    progressFraction = segmentProgressFraction,
                    strokeWidth = 8.dp,
                    trackColor = Color.Transparent,
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        minFontSize = 16.sp,
                    )
                    if (limitChipText != null) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(limitChipBackground)
                                .border(1.dp, limitChipBorderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.limit_chip, limitChipText),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = limitChipTextColor,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (resetText != null) {
                        Spacer(Modifier.height(6.dp))
                        AutoResizingText(
                            text = resetText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            minFontSize = 10.sp,
                        )
                    }
                }
            }
        }

        // Legend showing per-app usage
        AppUsageLegend(
            items = legendItems,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun buildProgressBarSegments(
    sessionSegments: List<SessionSegment>,
    currentUsage: Long,
    reelsLabel: String,
    tiktokLabel: String,
    shortsLabel: String,
): List<ProgressBarSegment> {
    val totalSegmentMillis = sessionSegments.sumOf { it.durationMillis.coerceAtLeast(0L) }
    val cappedTotalUsage = currentUsage.coerceAtLeast(0L)
    val scale = if (totalSegmentMillis > 0L && cappedTotalUsage in 1..<totalSegmentMillis) {
        cappedTotalUsage.toDouble() / totalSegmentMillis.toDouble()
    } else {
        1.0
    }

    return sessionSegments.mapNotNull { segment ->
        val rawUsageMillis = segment.durationMillis.coerceAtLeast(0L)
        if (rawUsageMillis <= 0L) {
            return@mapNotNull null
        }
        val usageMillis = (rawUsageMillis * scale).toLong().coerceAtLeast(1L)

        val color = when (segment.app) {
            BlockableApp.REELS -> instagramReelsColor
            BlockableApp.SHORTS -> youtubeShortsColor
            BlockableApp.TIKTOK -> tiktokColor
        }
        val appName = when (segment.app) {
            BlockableApp.REELS -> reelsLabel
            BlockableApp.SHORTS -> shortsLabel
            BlockableApp.TIKTOK -> tiktokLabel
        }

        ProgressBarSegment(segmentName = appName, usageMillis = usageMillis, color = color)
    }
}

private fun buildLegendItems(progressBarSegments: List<ProgressBarSegment>): List<LegendItem> =
    progressBarSegments.groupBy { it.segmentName }.mapNotNull { (segmentName, segments) ->
        val totalMillis = segments.sumOf { it.usageMillis.coerceAtLeast(0L) }
        if (totalMillis <= 0L) {
            return@mapNotNull null
        }

        LegendItem(
            legendName = segmentName,
            formattedTime = totalMillis.formatTime(),
            color = segments.first().color,
        )
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
fun OnScreenTimerToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    Timber.d("On-screen timer row click -> toggle to %s", !checked)
                    onCheckedChange(!checked)
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {

            AutoResizingText(
                text = stringResource(id = R.string.show_onscreen_timer),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                minFontSize = 12.sp,
            )

            Text(
                text = stringResource(id = R.string.timer_overlay_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = {
                Timber.d("On-screen timer switch toggled -> %s", it)
                onCheckedChange(it)
            },
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
private fun HomeBackground(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    accentStrength: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .radialGradientScrim(
                    baseColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    accentColor = accentColor?.copy(alpha = 0.22f),
                    accentStrength = accentStrength,
                ),
        )
        content()
    }
}

@DevicePreviews
@Composable
fun HomeScreenPreview() {
    val mockState = HomeUiState(
        blockOption = BlockOption.DailyLimit,
        timeLimit = TimeUnit.MINUTES.toMillis(60),
        currentUsage = TimeUnit.MINUTES.toMillis(42),
        progress = 70,
        timerOverlayEnabled = true,
        listSessionSegments = listOf(
            SessionSegment(BlockableApp.REELS, TimeUnit.MINUTES.toMillis(10), LocalDateTime.of(20, 10, 2, 1, 2)),
            SessionSegment(BlockableApp.REELS, TimeUnit.MINUTES.toMillis(3), LocalDateTime.of(20, 10, 2, 1, 2)),
            SessionSegment(BlockableApp.SHORTS, TimeUnit.MINUTES.toMillis(3), LocalDateTime.of(20, 10, 2, 1, 2)),
        ),
    )

    ScrollessTheme {
        HomeBackground(modifier = Modifier.fillMaxSize()) {
            HomeContent(
                uiState = mockState,
                onBlockOptionSelected = {},
                onConfigureDailyLimit = {},
                onScreenTimerToggled = {},
                onHelpClicked = {},
                onIntervalTimerClick = {},
                onIntervalTimerEdit = {},
                onPauseToggle = { _ -> },
            )
        }
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
            onScreenTimerToggled = {},
            onHelpClicked = {},
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
            onScreenTimerToggled = {},
            onHelpClicked = {},
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
            onScreenTimerToggled = {},
            onHelpClicked = {},
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
            onScreenTimerToggled = {},
            onHelpClicked = {},
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
            onScreenTimerToggled = {},
            onHelpClicked = {},
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
            onScreenTimerToggled = {},
            onHelpClicked = {},
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
            onScreenTimerToggled = {},
            onHelpClicked = {},
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
        )
        AccessibilitySuccessBottomSheetPreview()
    }
}
