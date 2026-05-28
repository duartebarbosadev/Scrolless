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
package com.scrolless.app.feature.home

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.designsystem.component.AppUsageLegend
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.designsystem.component.LegendItem
import com.scrolless.app.designsystem.component.ProgressBarSegment
import com.scrolless.app.designsystem.component.SegmentedCircularProgressIndicator
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.theme.facebookColor
import com.scrolless.app.designsystem.theme.facebookLiteColor
import com.scrolless.app.designsystem.theme.instagramReelsColor
import com.scrolless.app.designsystem.theme.progressbar_green_use
import com.scrolless.app.designsystem.theme.progressbar_orange_use
import com.scrolless.app.designsystem.theme.progressbar_red_use
import com.scrolless.app.designsystem.theme.snapchatColor
import com.scrolless.app.designsystem.theme.tiktokColor
import com.scrolless.app.designsystem.theme.youtubeShortsColor
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.designsystem.util.formatTime
import com.scrolless.app.designsystem.util.radialGradientScrim
import com.scrolless.app.designsystem.util.toCountdownLabel
import com.scrolless.app.designsystem.util.toIntervalLabel
import com.scrolless.app.feature.home.components.ANALYTICS_DATE_FORMATTER
import com.scrolless.app.feature.home.components.ANALYTICS_PAGER_DAY_COUNT
import com.scrolless.app.feature.home.components.AccessibilityExplainerBottomSheet
import com.scrolless.app.feature.home.components.AccessibilitySuccessBottomSheet
import com.scrolless.app.feature.home.components.AccessibilitySuccessBottomSheetPreview
import com.scrolless.app.feature.home.components.FloatingDebugUsagePanel
import com.scrolless.app.feature.home.components.HelpDialog
import com.scrolless.app.feature.home.components.InlineUsageAnalyticsPanel
import com.scrolless.app.feature.home.components.IntervalTimerDialog
import com.scrolless.app.feature.home.components.TimeLimitDialog
import com.scrolless.app.feature.home.components.WeekdayAverageSection
import com.scrolless.app.feature.home.components.analyticsForDate
import com.scrolless.app.feature.home.components.pageDateForPage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

private val DEFAULT_INTERVAL_BREAK_MILLIS = TimeUnit.MINUTES.toMillis(60)
private val DEFAULT_INTERVAL_ALLOWANCE_MILLIS = TimeUnit.MINUTES.toMillis(5)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    accessibilityServiceClass: Class<out AccessibilityService>? = null,
    onRequestAppReview: (Activity, (ReviewPromptResult) -> Unit) -> Unit = { _, onResult ->
        onResult(ReviewPromptResult.SkippedPermanent)
    },
    viewModel: HomeViewModel = hiltViewModel(),
) {
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

    // Observe lifecycle resume events so we can react when the user returns from settings:
    // - If accessibility is now enabled, flip the success sheet on once.
    // - If it is still disabled while a block option is active (or first launch), re-open the explainer.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Timber.d("HomeScreen resumed")
                val isAccessibilityEnabled = context.isAccessibilityServiceEnabled(accessibilityServiceClass)
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
            !context.isAccessibilityServiceEnabled(accessibilityServiceClass)
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
            onNavigateToSettings = onNavigateToSettings,
            onBlockOptionSelected = { blockOption ->
                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(accessibilityServiceClass)) {
                    Timber.i("Block option click -> %s (debug bypass: %s)", blockOption, shouldBypass)
                    viewModel.onBlockOptionSelected(blockOption)
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer.")
                    showAccessibilityExplainerPrompt()
                }
            },
            onConfigureDailyLimit = {
                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(accessibilityServiceClass)) {
                    Timber.d("Open TimeLimitDialog (debug bypass: %s)", shouldBypass)
                    showTimeLimitDialog = true
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer (daily limit).")
                    showAccessibilityExplainerPrompt()
                }
            },
            onHelpClicked = {
                Timber.d("Help clicked -> show HelpDialog")
                showHelpDialog = true
            },
            onIntervalTimerClick = {
                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(accessibilityServiceClass)) {
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
                if (shouldBypass || context.isAccessibilityServiceEnabled(accessibilityServiceClass)) {
                    Timber.d("Interval timer edit requested")
                    openIntervalConfig()
                } else {
                    Timber.w("Accessibility service not enabled. Showing explainer (interval timer config).")
                    showAccessibilityExplainerPrompt()
                }
            },
            onPauseToggle = { shouldPause ->

                val shouldBypass = BuildConfig.DEBUG && debugBypassAccessibilityCheck
                if (shouldBypass || context.isAccessibilityServiceEnabled(accessibilityServiceClass)) {
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
            onUsageAnalyticsDateSelected = viewModel::onUsageAnalyticsDateSelected,
            onUsageAnalyticsTodaySelected = viewModel::onUsageAnalyticsTodaySelected,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
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
            onRequestAppReview(activity) { result ->
                viewModel.onReviewPromptResult(result)
                viewModel.onReviewRequestHandled()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onBlockOptionSelected: (BlockOption) -> Unit,
    onConfigureDailyLimit: () -> Unit,
    onHelpClicked: () -> Unit,
    onIntervalTimerClick: () -> Unit,
    onIntervalTimerEdit: () -> Unit,
    onPauseToggle: (Boolean) -> Unit,
    onDebugUsageChanged: (List<SessionSegment>) -> Unit = {},
    onDebugUsageReset: () -> Unit = {},
    onUsageAnalyticsDateSelected: (LocalDate) -> Unit = {},
    onUsageAnalyticsTodaySelected: () -> Unit = {},
) {
    // Live pause/debug/expansion state used by the fixed home content below.
    val pauseRemainingMillis = rememberPauseRemainingTime(uiState.pauseUntilMillis)
    val isPauseActive = pauseRemainingMillis > 0L
    val showDebugPanel = BuildConfig.DEBUG || LocalInspectionMode.current
    var isDebugExpanded by remember { mutableStateOf(false) }
    var sessionChunksExpanded by remember(uiState.usageAnalytics.selectedDate) { mutableStateOf(false) }

    // Analytics pager state: page index 0 is the oldest day, today is the last page.
    val analytics = uiState.usageAnalytics
    val todayPage = ANALYTICS_PAGER_DAY_COUNT - 1
    val selectedPage = (
        todayPage - ChronoUnit.DAYS.between(analytics.selectedDate, analytics.today).toInt()
        ).coerceIn(0, todayPage)
    val pagerState = rememberPagerState(initialPage = selectedPage, pageCount = { ANALYTICS_PAGER_DAY_COUNT })

    // Screen-wide horizontal drags manually move only the progress-card pager.
    var hasDismissedSwipeHint by rememberSaveable { mutableStateOf(false) }
    val dateSwipeThresholdPx = with(LocalDensity.current) { 32.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    // Pointer input is long-lived, so it reads latest values through rememberUpdatedState.
    val latestAnalytics by rememberUpdatedState(analytics)
    val latestSelectedPage by rememberUpdatedState(selectedPage)
    val latestOnUsageAnalyticsDateSelected by rememberUpdatedState(onUsageAnalyticsDateSelected)

    // Determine if blocking is currently active (user would be blocked if they tried to view content)
    val isBlockingActive = when (uiState.blockOption) {
        // Always blocking
        BlockOption.BlockAll -> true

        BlockOption.DailyLimit -> uiState.timeLimit > 0 && uiState.currentUsage >= uiState.timeLimit

        BlockOption.IntervalTimer -> uiState.timeLimit > 0 && uiState.intervalUsage >= uiState.timeLimit

        BlockOption.NothingSelected -> false
    }

    LaunchedEffect(selectedPage) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(pagerState, dateSwipeThresholdPx) {
                var totalDragX = 0f
                var dragStartPage = latestSelectedPage
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        dragStartPage = latestSelectedPage
                        hasDismissedSwipeHint = true
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount
                        pagerState.dispatchRawDelta(-dragAmount)
                    },
                    onDragEnd = {
                        val targetPage = when {
                            totalDragX <= -dateSwipeThresholdPx -> dragStartPage + 1
                            totalDragX >= dateSwipeThresholdPx -> dragStartPage - 1
                            else -> dragStartPage
                        }.coerceIn(0, todayPage)
                        val targetDate = pageDateForPage(targetPage, latestAnalytics.today, todayPage)

                        if (targetDate != latestAnalytics.selectedDate) {
                            latestOnUsageAnalyticsDateSelected(targetDate)
                        }

                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(dragStartPage)
                        }
                    },
                )
            }
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            UsageOverviewHeader(
                uiState = uiState,
                analytics = analytics,
                isViewingToday = analytics.selectedDate == analytics.today,
                showDateSwipeHint = !hasDismissedSwipeHint,
                pagerState = pagerState,
                todayPage = todayPage,
                onUsageAnalyticsTodaySelected = onUsageAnalyticsTodaySelected,
                onHelpClicked = onHelpClicked,
                onNavigateToSettings = onNavigateToSettings,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(
                    visible = analytics.selectedDate == analytics.today,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(220),
                    ) + fadeIn(animationSpec = tween(140)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(180),
                    ) + fadeOut(animationSpec = tween(100)),
                ) {
                    TodayBlockingControls(
                        uiState = uiState,
                        isBlockingActive = isBlockingActive,
                        isPauseActive = isPauseActive,
                        pauseRemainingMillis = pauseRemainingMillis,
                        onBlockOptionSelected = onBlockOptionSelected,
                        onConfigureDailyLimit = onConfigureDailyLimit,
                        onIntervalTimerClick = onIntervalTimerClick,
                        onIntervalTimerEdit = onIntervalTimerEdit,
                        onPauseToggle = onPauseToggle,
                    )
                }

                InlineUsageAnalyticsPanel(
                    analytics = analytics,
                    sessionChunksExpanded = sessionChunksExpanded,
                    onToggleSessionChunks = { sessionChunksExpanded = !sessionChunksExpanded },
                )

                if (analytics.selectedDate == analytics.today) {
                    Spacer(modifier = Modifier.height(24.dp))
                    WeekdayAverageSection(weekdayAverages = analytics.weekdayAverages)
                }
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
private fun UsageOverviewHeader(
    uiState: HomeUiState,
    analytics: UsageAnalyticsUiState,
    isViewingToday: Boolean,
    showDateSwipeHint: Boolean,
    pagerState: PagerState,
    todayPage: Int,
    onUsageAnalyticsTodaySelected: () -> Unit,
    onHelpClicked: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(top = 54.dp),
            beyondViewportPageCount = 1,
            userScrollEnabled = false,
        ) { page ->
            val pageDate = remember(page, analytics.today) {
                pageDateForPage(page, analytics.today, todayPage)
            }
            val pageAnalytics = remember(pageDate, analytics.daySummaries) {
                analyticsForDate(analytics = analytics, date = pageDate)
            }
            val isTodayPage = pageDate == analytics.today
            val dateLabel = if (isTodayPage) {
                stringResource(R.string.usage_analytics_today)
            } else {
                pageDate.format(ANALYTICS_DATE_FORMATTER)
            }

            ProgressCard(
                blockOption = if (isTodayPage) uiState.blockOption else BlockOption.NothingSelected,
                progress = if (isTodayPage) uiState.progress else 0,
                currentUsage = if (isTodayPage) uiState.currentUsage else pageAnalytics.dailyTotalMillis,
                intervalUsage = if (isTodayPage) uiState.intervalUsage else 0L,
                timeLimit = if (isTodayPage) uiState.timeLimit else 0L,
                intervalLength = if (isTodayPage) uiState.intervalLength else 0L,
                intervalWindowStart = if (isTodayPage) uiState.intervalWindowStart else 0L,
                listSessionSegments = if (isTodayPage) uiState.listSessionSegments else pageAnalytics.sessionSegments,
                dateLabel = dateLabel,
                showDateSwipeHint = showDateSwipeHint,
                onClick = onUsageAnalyticsTodaySelected,
            )
        }

        AnimatedVisibility(
            visible = !isViewingToday,
            enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(240)),
            exit = fadeOut(animationSpec = tween(140)) + shrinkVertically(animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp, 8.dp, 0.dp, 8.dp),
        ) {
            TodayShortcutButton(onClick = onUsageAnalyticsTodaySelected)
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp, 8.dp, 0.dp, 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HelpButton(
                onClick = onHelpClicked,
            )

            SettingsButton(
                onClick = onNavigateToSettings,
            )
        }
    }
}

@Composable
private fun TodayBlockingControls(
    uiState: HomeUiState,
    isBlockingActive: Boolean,
    isPauseActive: Boolean,
    pauseRemainingMillis: Long,
    onBlockOptionSelected: (BlockOption) -> Unit,
    onConfigureDailyLimit: () -> Unit,
    onIntervalTimerClick: () -> Unit,
    onIntervalTimerEdit: () -> Unit,
    onPauseToggle: (Boolean) -> Unit,
) {
    val weightBase = 1f
    val weightExpanded = 1.2f
    val weightShrunk = 0.9f

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
            isBlockAllPressed -> weightExpanded
            isDailyLimitPressed || isIntervalPressed -> weightShrunk
            else -> weightBase
        },
        animationSpec = tween(100), label = "blockAllWeight",
    )

    val dailyLimitWeight by animateFloatAsState(
        targetValue = when {
            isDailyLimitPressed -> weightExpanded
            isBlockAllPressed || isIntervalPressed -> weightShrunk
            else -> weightBase
        },
        animationSpec = tween(100), label = "dailyLimitWeight",
    )

    val intervalWeight by animateFloatAsState(
        targetValue = when {
            isIntervalPressed -> weightExpanded
            isBlockAllPressed || isDailyLimitPressed -> weightShrunk
            else -> weightBase
        },
        animationSpec = tween(100), label = "intervalWeight",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 320)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            Row(modifier = Modifier.fillMaxWidth()) {
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
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .align(Alignment.Center),
                    )
                }
                Spacer(modifier = Modifier.weight(intervalWeight))
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
    val allowanceLabel = allowanceMillis.toIntervalLabel(stringResource(R.string.time_placeholder_dash))
    val breakLabel = intervalLengthMillis.toIntervalLabel(stringResource(R.string.time_placeholder_dash))
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
private fun TodayShortcutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.usage_analytics_today),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
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
    dateLabel: String? = null,
    showDateSwipeHint: Boolean = false,
    onClick: () -> Unit = {},
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

    val facebookLabel = stringResource(R.string.app_facebook)
    val facebookLiteLabel = stringResource(R.string.app_facebook_lite)
    val reelsLabel = stringResource(R.string.app_reels)
    val snapchatLabel = stringResource(R.string.app_snapchat)
    val tiktokLabel = stringResource(R.string.app_tiktok)
    val shortsLabel = stringResource(R.string.app_shorts)

    // Per-app usage data for the segmented progress indicator
    val progressBarSegments =
        remember(listSessionSegments, currentUsage, facebookLabel, facebookLiteLabel, reelsLabel, snapchatLabel, tiktokLabel, shortsLabel) {
            buildProgressBarSegments(
                sessionSegments = listSessionSegments,
                currentUsage = currentUsage,
                facebookLabel = facebookLabel,
                facebookLiteLabel = facebookLiteLabel,
                reelsLabel = reelsLabel,
                snapchatLabel = snapchatLabel,
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
                .clickable(onClick = onClick),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        if (dateLabel != null) {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                ) {
                    AnimatedContent(
                        targetState = dateLabel,
                        label = "progressDateLabel",
                    ) { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                AnimatedVisibility(
                    visible = showDateSwipeHint,
                    enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(140)) + shrinkVertically(animationSpec = tween(160)),
                ) {
                    Text(
                        text = stringResource(R.string.usage_analytics_swipe_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun buildProgressBarSegments(
    sessionSegments: List<SessionSegment>,
    currentUsage: Long,
    facebookLabel: String,
    facebookLiteLabel: String,
    reelsLabel: String,
    snapchatLabel: String,
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
            BlockableApp.FACEBOOK -> facebookColor
            BlockableApp.FACEBOOK_LITE -> facebookLiteColor
            BlockableApp.REELS -> instagramReelsColor
            BlockableApp.SNAPCHAT -> snapchatColor
            BlockableApp.SHORTS -> youtubeShortsColor
            BlockableApp.TIKTOK -> tiktokColor
        }
        val appName = when (segment.app) {
            BlockableApp.FACEBOOK -> facebookLabel
            BlockableApp.FACEBOOK_LITE -> facebookLiteLabel
            BlockableApp.REELS -> reelsLabel
            BlockableApp.SNAPCHAT -> snapchatLabel
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
                stringResource(
                    id = R.string.pause_resumes_in,
                    remainingMillis.toCountdownLabel(stringResource(R.string.time_placeholder_zero)),
                )
            } else {
                stringResource(
                    id = R.string.pause_duration_hint,
                    pauseDurationMinutes,
                )
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
fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_help_outline_24),
            contentDescription = stringResource(R.string.help),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_settings),
            contentDescription = stringResource(R.string.settings),
            tint = MaterialTheme.colorScheme.onSurface,
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
            SessionSegment(BlockableApp.FACEBOOK, TimeUnit.MINUTES.toMillis(8), LocalDateTime.of(2026, 10, 2, 0, 55)),
            SessionSegment(BlockableApp.FACEBOOK_LITE, TimeUnit.MINUTES.toMillis(5), LocalDateTime.of(2026, 10, 2, 1, 0)),
            SessionSegment(BlockableApp.REELS, TimeUnit.MINUTES.toMillis(10), LocalDateTime.of(2026, 10, 2, 1, 2)),
            SessionSegment(BlockableApp.REELS, TimeUnit.MINUTES.toMillis(3), LocalDateTime.of(2026, 10, 2, 1, 2)),
            SessionSegment(BlockableApp.SHORTS, TimeUnit.MINUTES.toMillis(3), LocalDateTime.of(2026, 10, 2, 1, 2)),
        ),
    )

    ScrollessTheme {
        HomeBackground(modifier = Modifier.fillMaxSize()) {
            HomeContent(
                uiState = mockState,
                onBlockOptionSelected = {},
                onConfigureDailyLimit = {},
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
            onHelpClicked = {},
            onIntervalTimerClick = {},
            onIntervalTimerEdit = {},
            onPauseToggle = { _ -> },
        )
        AccessibilitySuccessBottomSheetPreview()
    }
}
