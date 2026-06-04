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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.designsystem.theme.LocalSharedTransitionScope
import com.scrolless.app.designsystem.theme.SETTINGS_TRANSITION_KEY
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.designsystem.theme.progressbar_green_use
import com.scrolless.app.designsystem.theme.progressbar_orange_use
import com.scrolless.app.designsystem.theme.progressbar_red_use
import com.scrolless.app.designsystem.tooling.DevicePreviews
import com.scrolless.app.designsystem.util.radialGradientScrim
import com.scrolless.app.feature.home.components.InlineUsageAnalyticsPanel
import com.scrolless.app.feature.home.components.ProgressCard
import com.scrolless.app.feature.home.components.TodayBlockingControls
import com.scrolless.app.feature.home.components.WeekdayAverageSection
import com.scrolless.app.feature.home.components.analyticsForDate
import com.scrolless.app.feature.home.components.shortLabel
import com.scrolless.app.feature.home.debug.FloatingDebugUsagePanel
import com.scrolless.app.feature.home.dialogs.AccessibilityExplainerBottomSheet
import com.scrolless.app.feature.home.dialogs.AccessibilitySuccessBottomSheet
import com.scrolless.app.feature.home.dialogs.AccessibilitySuccessBottomSheetPreview
import com.scrolless.app.feature.home.dialogs.HelpDialog
import com.scrolless.app.feature.home.dialogs.IntervalTimerDialog
import com.scrolless.app.feature.home.dialogs.TimeLimitDialog
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
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
            modifier = modifier,
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
                viewModel.onDebugUsageSegmentsChanged(uiState.usageAnalytics.selectedDate, usageSegments)
            },
            onDebugUsageReset = {
                viewModel.onDebugResetUsage(uiState.usageAnalytics.selectedDate)
            },
            onUsageAnalyticsDateSelected = viewModel::onUsageAnalyticsDateSelected,
            onUsageAnalyticsTodaySelected = viewModel::onUsageAnalyticsTodaySelected,
            onAveragePeriodSelected = viewModel::onAveragePeriodSelected,
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
    onAveragePeriodSelected: (UsageAveragePeriod) -> Unit = {},
) {
    val pauseRemainingMillis = rememberPauseRemainingTime(uiState.pauseUntilMillis)
    val isPauseActive = pauseRemainingMillis > 0L
    val showDebugPanel = BuildConfig.DEBUG || LocalInspectionMode.current
    var isDebugExpanded by remember { mutableStateOf(false) }
    var sessionChunksExpanded by remember(uiState.usageAnalytics.selectedDate) { mutableStateOf(false) }

    // Analytics pager state: page index 0 is the oldest day, today is the last page.
    val analytics = uiState.usageAnalytics
    val todayPage = remember(analytics.dataStartDate, analytics.today) {
        ChronoUnit.DAYS.between(analytics.dataStartDate, analytics.today).toInt()
    }
    val selectedPage = remember(analytics.dataStartDate, analytics.selectedDate, todayPage) {
        ChronoUnit.DAYS.between(analytics.dataStartDate, analytics.selectedDate).toInt()
            .coerceIn(0, todayPage.coerceAtLeast(0))
    }
    val pagerState = rememberPagerState(
        initialPage = selectedPage,
        pageCount = { (todayPage + 1).coerceAtLeast(1) },
    )

    // Screen-wide horizontal drags manually move only the progress-card pager.
    val dateSwipeThresholdPx = with(LocalDensity.current) { 32.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    // Determine if blocking is currently active (user would be blocked if they tried to view content)
    val isBlockingActive = when (uiState.blockOption) {
        // Always blocking
        BlockOption.BlockAll -> true

        BlockOption.DailyLimit -> uiState.timeLimit > 0 && uiState.currentUsage >= uiState.timeLimit

        BlockOption.IntervalTimer -> uiState.timeLimit > 0 && uiState.intervalUsage >= uiState.timeLimit

        BlockOption.NothingSelected -> false
    }

    var isInitialPageLoad by remember { mutableStateOf(true) }

    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) {
            if (isInitialPageLoad) {
                isInitialPageLoad = false
                pagerState.scrollToPage(selectedPage)
            } else {
                pagerState.animateScrollToPage(selectedPage)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .dateSwipeGesture(
                pagerState = pagerState,
                todayPage = todayPage,
                dateSwipeThresholdPx = dateSwipeThresholdPx,
                analytics = analytics,
                selectedPage = selectedPage,
                coroutineScope = coroutineScope,
                onUsageAnalyticsDateSelected = onUsageAnalyticsDateSelected,
            )
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            UsageOverviewHeader(
                uiState = uiState,
                analytics = analytics,
                pagerState = pagerState,
                todayPage = todayPage,
                selectedPage = selectedPage,
                onUsageAnalyticsDateSelected = onUsageAnalyticsDateSelected,
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
                    WeekdayAverageSection(
                        weekdayAverages = analytics.weekdayAverages,
                        selectedPeriod = uiState.averagePeriod,
                        onPeriodSelected = { onAveragePeriodSelected(it) },
                    )
                }
            }

            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }

        if (showDebugPanel) {
            FloatingDebugUsagePanel(
                sessionSegments = analytics.sessionSegments,
                selectedDate = analytics.selectedDate,
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
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    analytics: UsageAnalyticsUiState,
    pagerState: PagerState,
    todayPage: Int,
    selectedPage: Int,
    onUsageAnalyticsDateSelected: (LocalDate) -> Unit,
    onUsageAnalyticsTodaySelected: () -> Unit,
    onHelpClicked: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (todayPage > 0) {
                DateNavigator(
                    selectedDate = analytics.selectedDate,
                    today = analytics.today,
                    canGoBack = selectedPage > 0,
                    canGoForward = selectedPage < todayPage,
                    onDateClick = onUsageAnalyticsTodaySelected,
                    onPrevious = {
                        val targetPage = (selectedPage - 1).coerceAtLeast(0)
                        onUsageAnalyticsDateSelected(analytics.dataStartDate.plusDays(targetPage.toLong()))
                    },
                    onNext = {
                        val targetPage = (selectedPage + 1).coerceAtMost(todayPage)
                        onUsageAnalyticsDateSelected(analytics.dataStartDate.plusDays(targetPage.toLong()))
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp),
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
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

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            userScrollEnabled = false,
        ) { page ->
            val pageDate = remember(page, analytics.dataStartDate) {
                analytics.dataStartDate.plusDays(page.toLong())
            }
            val pageAnalytics = remember(pageDate, analytics.daySummaries) {
                analyticsForDate(analytics = analytics, date = pageDate)
            }
            val isTodayPage = pageDate == analytics.today

            ProgressCard(
                blockOption = if (isTodayPage) uiState.blockOption else BlockOption.NothingSelected,
                progress = if (isTodayPage) uiState.progress else 0,
                currentUsage = if (isTodayPage) uiState.currentUsage else pageAnalytics.dailyTotalMillis,
                intervalUsage = if (isTodayPage) uiState.intervalUsage else 0L,
                timeLimit = if (isTodayPage) uiState.timeLimit else 0L,
                intervalLength = if (isTodayPage) uiState.intervalLength else 0L,
                intervalWindowStart = if (isTodayPage) uiState.intervalWindowStart else 0L,
                listSessionSegments = if (isTodayPage) uiState.listSessionSegments else pageAnalytics.sessionSegments,
                onClick = onUsageAnalyticsTodaySelected,
            )
        }
    }
}

@Composable
private fun DateNavigator(
    selectedDate: LocalDate,
    today: LocalDate,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onDateClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateLabel = if (selectedDate == today) {
        stringResource(R.string.usage_analytics_today)
    } else {
        selectedDate.formatHeaderDate()
    }

    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canGoBack) {
                DateNavButton(
                    onClick = onPrevious,
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.usage_analytics_previous_day),
                )
            }
            AnimatedContent(
                targetState = dateLabel,
                label = "selectedDateLabel",
            ) { label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .widthIn(max = 128.dp)
                        .clickable(
                            enabled = selectedDate != today,
                            onClick = onDateClick,
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (canGoForward) {
                DateNavButton(
                    onClick = onNext,
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.usage_analytics_next_day),
                )
            }
        }
    }
}

@Composable
private fun DateNavButton(onClick: () -> Unit, imageVector: ImageVector, contentDescription: String) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
        )
    }
}

@Composable
private fun LocalDate.formatHeaderDate(): String {
    val locale = Locale.current.platformLocale
    val weekday = dayOfWeek.shortLabel()
    val monthStr = month.getDisplayName(TextStyle.SHORT, locale)
    val formattedWeekday = if (weekday.endsWith('.')) weekday else "$weekday."
    val formattedMonth = if (monthStr.endsWith('.')) monthStr else "$monthStr."
    return "$formattedWeekday, $formattedMonth $dayOfMonth"
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

@Composable
fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_help_outline_24),
            contentDescription = stringResource(R.string.help),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val sharedTransitionScope = LocalSharedTransitionScope.current

    val sharedBoundsModifier = if (sharedTransitionScope != null) {
        val animatedVisibilityScope = LocalNavAnimatedContentScope.current
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = SETTINGS_TRANSITION_KEY),
                animatedVisibilityScope = animatedVisibilityScope,
                clipInOverlayDuringTransition = OverlayClip(clipShape = RoundedCornerShape(50)),
            )
        }
    } else {
        Modifier
    }

    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.then(sharedBoundsModifier),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
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

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.dateSwipeGesture(
    pagerState: PagerState,
    todayPage: Int,
    dateSwipeThresholdPx: Float,
    analytics: UsageAnalyticsUiState,
    selectedPage: Int,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onUsageAnalyticsDateSelected: (LocalDate) -> Unit,
): Modifier = this.pointerInput(
    pagerState,
    dateSwipeThresholdPx,
    selectedPage,
    todayPage,
    analytics.dataStartDate,
    analytics.selectedDate,
) {
    var totalDragX = 0f
    var dragStartPage = selectedPage
    detectHorizontalDragGestures(
        onDragStart = {
            totalDragX = 0f
            dragStartPage = selectedPage
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
            val targetDate = analytics.dataStartDate.plusDays(targetPage.toLong())

            if (targetDate != analytics.selectedDate) {
                onUsageAnalyticsDateSelected(targetDate)
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

@DevicePreviews
@Composable
fun HomeScreenPreview() {
    val mockState = HomeUiState(
        blockOption = BlockOption.DailyLimit,
        timeLimit = TimeUnit.MINUTES.toMillis(60),
        currentUsage = TimeUnit.MINUTES.toMillis(42),
        progress = 70,
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
