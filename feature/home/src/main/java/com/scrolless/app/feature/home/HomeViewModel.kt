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

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockingConfig
import com.scrolless.app.core.model.BlockingSettings
import com.scrolless.app.core.model.IntervalUsage
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.model.usage.DailyUsageTotal
import com.scrolless.app.core.model.usage.calculateWeekdayAverages
import com.scrolless.app.core.repository.BlockingConfigRepository
import com.scrolless.app.core.repository.SessionSegmentStore
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.core.util.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

private const val FIRST_LAUNCH_LOADING = -1L

/**
 * ViewModel that handles the business logic and screen state of the HomeScreen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
    private val blockingConfigRepository: BlockingConfigRepository,
    private val sessionSegmentStore: SessionSegmentStore,
) : ViewModel() {

    private val _showComingSoonSnackBar = MutableStateFlow(false)
    private val _selectedAveragePeriod = MutableStateFlow(UsageAveragePeriod.LAST_WEEK)
    private val selectedAnalyticsDate = MutableStateFlow(ZonedDateTime.now().toLocalDate())
    private val currentDate = currentDayFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ZonedDateTime.now().toLocalDate(),
    )
    private val reviewPromptDismissed = MutableStateFlow(false)

    private val requestReview = combine(
        userSettingsStore.getFirstLaunchAt(),
        userSettingsStore.getHasSeenReviewPrompt(),
        userSettingsStore.getReviewPromptAttemptCount(),
        userSettingsStore.getReviewPromptLastAttemptAt(),
        reviewPromptDismissed,
    ) { firstLaunchAt, hasSeenReviewPrompt, attemptCount, lastAttemptAt, dismissed ->
        if (dismissed) return@combine false
        if (firstLaunchAt == FIRST_LAUNCH_LOADING) return@combine false
        val now = System.currentTimeMillis()

        // Avoid spamming
        // Require an initial delay, a retry cooldown, and a max attempt cap.
        !hasSeenReviewPrompt &&
            attemptCount < REVIEW_PROMPT_MAX_ATTEMPTS &&
            (lastAttemptAt == 0L || now - lastAttemptAt >= REVIEW_PROMPT_RETRY_DELAY_MILLIS) &&
            now - firstLaunchAt >= REVIEW_PROMPT_DELAY_MILLIS
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    companion object {
        private const val PROGRESS_MAX = 100
        private val REVIEW_PROMPT_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(5) // Show review popup after 5 minutes
        private const val REVIEW_PROMPT_MAX_ATTEMPTS = 3
        private val REVIEW_PROMPT_RETRY_DELAY_MILLIS = TimeUnit.DAYS.toMillis(1)
    }

    init {
        viewModelScope.launch {
            currentDate.collect { today ->
                if (selectedAnalyticsDate.value.isAfter(today)) {
                    selectedAnalyticsDate.value = today
                }
            }
        }
    }

    private val sessionSegmentsForCurrentDay = currentDate.flatMapLatest { currentDate ->
        sessionSegmentStore.getListSessionSegments(currentDate)
    }

    private val dailyUsageTotals = combine(currentDate, userSettingsStore.getFirstLaunchDate()) { today, firstLaunchDate ->
        val actualFirstLaunchDate = firstLaunchDate ?: today
        val pagerStart = today.minusDays(ANALYTICS_PAGER_DAY_COUNT.toLong())
        val windowStart = maxOf(pagerStart, actualFirstLaunchDate)
        today to windowStart
    }.flatMapLatest { (today, windowStart) ->
        sessionSegmentStore.getDailyUsageTotals(
            startDate = windowStart,
            endDateInclusive = today,
        )
    }

    private val detailedWindowSegments = selectedAnalyticsDate.flatMapLatest { selectedDate ->
        sessionSegmentStore.getListSessionSegments(
            startDate = selectedDate.minusDays(1),
            endDateInclusive = selectedDate.plusDays(1),
        )
    }

    private val analyticsSnapshot = combine(
        selectedAnalyticsDate,
        currentDate,
        dailyUsageTotals,
        detailedWindowSegments,
        _selectedAveragePeriod,
        userSettingsStore.getFirstLaunchDate(),
    ) { selectedDate, today, dailyTotals, detailedSegments, period, firstLaunchDate ->
        buildUsageAnalyticsUiState(
            selectedDate = selectedDate.coerceAtMost(today),
            today = today,
            dailyTotals = dailyTotals,
            detailedSegments = detailedSegments,
            period = period,
            firstLaunchDate = firstLaunchDate ?: LocalDate.EPOCH,
        )
    }

    /** The blocking config, with its interval window rolled forward to the one running now. */
    private val currentBlockingConfig: Flow<BlockingConfig> = blockingConfigRepository.observeConfig()
        .flatMapLatest { config ->
            config.intervalUsage.emitOnEveryRestart(config.settings.intervalLengthMillis)
                .map { usage -> config.copy(intervalUsage = usage) }
        }
        .distinctUntilChanged()

    private val usageSnapshot = combine(
        currentBlockingConfig,
        currentDate.flatMapLatest { date -> sessionSegmentStore.observeTotalDuration(date) },
        sessionSegmentsForCurrentDay,
    ) { blockingConfig, currentUsage, usageSegment ->
        UsageSnapshot(
            blockingConfig = blockingConfig,
            currentUsage = currentUsage,
            sessionSegment = usageSegment,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        usageSnapshot,
        userSettingsStore.getPauseUntil(),
        _showComingSoonSnackBar,
        requestReview,
        userSettingsStore.getHasSeenAccessibilityExplainer(),
        userSettingsStore.getPauseDuration(),
        analyticsSnapshot,
        _selectedAveragePeriod,
    ) {
            usage,
            pauseUntil,
            showComingSoonSnackBar,
            requestReview,
            hasSeenAccessibilityExplainer,
            pauseDuration,
            analytics,
            averagePeriod,
        ->

        val progress = calculateProgress(
            activeOption = usage.blockingConfig.activeOption,
            settings = usage.blockingConfig.settings,
            currentUsage = usage.currentUsage,
            intervalUsageMillis = usage.blockingConfig.intervalUsage.usageMillis,
        )

        HomeUiState(
            blockOption = usage.blockingConfig.activeOption,
            settings = usage.blockingConfig.settings,
            intervalUsage = usage.blockingConfig.intervalUsage,
            currentUsage = usage.currentUsage,
            progress = progress,
            pauseUntilMillis = pauseUntil,
            pauseDurationMillis = pauseDuration,
            showComingSoonSnackBar = showComingSoonSnackBar,
            requestReview = requestReview,
            hasSeenAccessibilityExplainer = hasSeenAccessibilityExplainer,
            hasLoadedSettings = true,
            listSessionSegments = usage.sessionSegment,
            usageAnalytics = analytics,
            averagePeriod = averagePeriod,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onBlockOptionSelected(blockOption: BlockOption) {
        Timber.i("Block option selected: %s", blockOption)
        viewModelScope.launch {
            blockingConfigRepository.setActiveOption(blockOption)
            if (blockOption == BlockOption.NothingSelected) {
                onPauseToggle(false)
            }
        }
    }

    fun onTimeLimitChange(durationMillis: Long) {
        Timber.d("Time limit changed: %d ms", durationMillis)
        viewModelScope.launch {
            blockingConfigRepository.configureDailyLimit(durationMillis)
        }
    }

    fun onPauseToggle(shouldPause: Boolean) {
        val pauseDuration = uiState.value.pauseDurationMillis.takeIf { it > 0 } ?: (5 * 60 * 1000L)
        val targetTimestamp = if (shouldPause) {
            System.currentTimeMillis() + pauseDuration
        } else {
            0L
        }
        if (shouldPause) {
            Timber.i("Pause requested until %d", targetTimestamp)
        } else {
            Timber.i("Pause cancelled early, resuming automatic blocking")
        }
        viewModelScope.launch {
            userSettingsStore.setPauseUntil(targetTimestamp)
        }
    }

    fun onIntervalTimerConfigChange(intervalBreakMillis: Long, allowanceMillis: Long) {
        Timber.d(
            "Interval timer config change: break=%d ms, allowance=%d ms",
            intervalBreakMillis,
            allowanceMillis,
        )
        viewModelScope.launch {
            blockingConfigRepository.configureIntervalTimer(
                allowanceMillis = allowanceMillis,
                intervalLengthMillis = intervalBreakMillis,
            )
        }
    }

    /**
     * Daily progress uses today's total usage. Interval progress uses the current window only.
     */
    private fun calculateProgress(
        activeOption: BlockOption,
        settings: BlockingSettings,
        currentUsage: Long,
        intervalUsageMillis: Long,
    ): Int = when (activeOption) {
        BlockOption.DailyLimit -> usageToProgress(usage = currentUsage, limit = settings.dailyLimitMillis)

        BlockOption.IntervalTimer -> usageToProgress(usage = intervalUsageMillis, limit = settings.intervalAllowanceMillis)

        BlockOption.BlockAll,
        BlockOption.NothingSelected,
        -> 0
    }

    /**
     * Keeps non-zero progress visible even when it is less than one percent.
     */
    private fun usageToProgress(usage: Long, limit: Long): Int {
        if (limit <= 0L) return 0
        if (usage <= 0L) return 0
        if (usage >= limit) return PROGRESS_MAX

        val rawProgress = ((usage.toDouble() / limit.toDouble()) * PROGRESS_MAX).toInt()
        return min(PROGRESS_MAX - 1, rawProgress.coerceAtLeast(1))
    }

    fun onSnackbarShown() {
        Timber.v("Snackbar dismissed")
        _showComingSoonSnackBar.value = false
    }

    fun onReviewRequestHandled() {
        Timber.v("Review request handled")
        reviewPromptDismissed.value = true
    }

    fun onReviewRequestStarted() {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val currentCount = userSettingsStore.getReviewPromptAttemptCount().first()
            val nextAttemptCount = currentCount + 1
            userSettingsStore.setReviewPromptAttemptCount(nextAttemptCount)
            userSettingsStore.setReviewPromptLastAttemptAt(now)
            reviewPromptDismissed.value = false
        }
    }

    fun onUsageAnalyticsDateSelected(date: LocalDate) {
        selectedAnalyticsDate.value = date.coerceAtMost(currentDate.value)
    }

    fun onUsageAnalyticsTodaySelected() {
        selectedAnalyticsDate.value = currentDate.value
    }

    fun onReviewPromptResult(result: ReviewPromptResult) {
        viewModelScope.launch {
            val attemptCount = userSettingsStore.getReviewPromptAttemptCount().first()
            val shouldMarkSeen = when (result) {
                ReviewPromptResult.Shown -> true
                ReviewPromptResult.SkippedPermanent -> true
                ReviewPromptResult.SkippedTemporary -> false
            } || attemptCount >= REVIEW_PROMPT_MAX_ATTEMPTS

            if (!shouldMarkSeen) {
                Timber.d("Review prompt was not shown; leaving eligible for future prompts.")
                return@launch
            }

            Timber.d("Review prompt resolved; marking as seen.")
            userSettingsStore.setHasSeenReviewPrompt(true)
        }
    }

    fun setWaitingForAccessibility(waiting: Boolean) {
        Timber.d("Setting waiting for accessibility: %s", waiting)
        viewModelScope.launch {
            userSettingsStore.setWaitingForAccessibility(waiting)
        }
    }

    fun onAccessibilityExplainerShown() {
        Timber.d("Accessibility explainer shown")
        viewModelScope.launch {
            userSettingsStore.setHasSeenAccessibilityExplainer(true)
        }
    }

    fun onDebugUsageSegmentsChanged(date: LocalDate, sessionSegments: List<SessionSegment>) {
        viewModelScope.launch {
            val normalizedSegments = sessionSegments.map { segment ->
                segment.copy(
                    durationMillis = segment.durationMillis.coerceAtLeast(0L),
                    startDateTime = segment.startDateTime.withSecond(0).withNano(0),
                )
            }
            sessionSegmentStore.replaceSessionSegmentsForDate(
                date = date,
                sessionSegments = normalizedSegments,
            )
        }
    }

    fun onDebugResetUsage(date: LocalDate) {
        viewModelScope.launch {
            sessionSegmentStore.replaceSessionSegmentsForDate(
                date = date,
                sessionSegments = emptyList(),
            )
        }
    }

    fun onAveragePeriodSelected(period: UsageAveragePeriod) {
        _selectedAveragePeriod.value = period
    }

    private fun currentDayFlow() = flow {
        while (true) {
            val now = ZonedDateTime.now()
            emit(now.toLocalDate())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            val delayMillis = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
            delay(delayMillis.milliseconds)
        }
    }.distinctUntilChanged()
}

@Immutable
data class HomeUiState(
    val blockOption: BlockOption = BlockOption.NothingSelected,
    val settings: BlockingSettings = BlockingSettings(),
    val intervalUsage: IntervalUsage = IntervalUsage.NOT_STARTED,
    val currentUsage: Long = 0L,
    val progress: Int = 0,
    val showComingSoonSnackBar: Boolean = false,
    val requestReview: Boolean = false,
    val isDevMode: Boolean = false,
    val playStoreUrl: String? = null,
    val pauseUntilMillis: Long = 0L,
    val pauseDurationMillis: Long = 5 * 60 * 1000L,
    val hasSeenAccessibilityExplainer: Boolean = false,

    /**
     * `true` after the app has loaded the user's settings for the first time.
     *
     * The Home screen waits for this before opening dialogs automatically. Without this check, it
     * could mistake temporary default values for the user's saved settings.
     */
    val hasLoadedSettings: Boolean = false,

    /**
     * Per-app usage breakdown for the segmented progress indicator.
     */
    val listSessionSegments: List<SessionSegment> = emptyList(),
    val usageAnalytics: UsageAnalyticsUiState = UsageAnalyticsUiState(),
    val averagePeriod: UsageAveragePeriod = UsageAveragePeriod.LAST_WEEK,
)

private fun buildUsageAnalyticsUiState(
    selectedDate: LocalDate,
    today: LocalDate,
    dailyTotals: List<DailyUsageTotal>,
    detailedSegments: List<SessionSegment>,
    period: UsageAveragePeriod = UsageAveragePeriod.LAST_WEEK,
    firstLaunchDate: LocalDate = LocalDate.EPOCH,
): UsageAnalyticsUiState {
    val windowStart = today.minusDays(ANALYTICS_PAGER_DAY_COUNT.toLong())
    val detailedSegmentsByDate = detailedSegments.groupBy { it.startDateTime.toLocalDate() }
    val dailyTotalsMap = dailyTotals.associate { it.date to it.totalMillis }

    val daySummaries = buildMap {
        val dayCount = java.time.temporal.ChronoUnit.DAYS.between(windowStart, today).toInt()
        (0..dayCount).forEach { offset ->
            val date = windowStart.plusDays(offset.toLong())
            val segments = detailedSegmentsByDate[date].orEmpty()
            if (segments.isNotEmpty() || date == selectedDate || date == selectedDate.minusDays(1) || date == selectedDate.plusDays(1)) {
                put(date, buildUsageAnalyticsDayUiState(date = date, segments = segments))
            } else {
                put(
                    date,
                    UsageAnalyticsDayUiState(
                        date = date,
                        dailyTotalMillis = dailyTotalsMap[date] ?: 0L,
                        sessionSegments = emptyList(),
                        appTotals = emptyList(),
                    ),
                )
            }
        }
    }

    val selectedDay = daySummaries[selectedDate]
        ?: buildUsageAnalyticsDayUiState(date = selectedDate, segments = detailedSegmentsByDate[selectedDate].orEmpty())

    val averageStartDate = when (period) {
        UsageAveragePeriod.LAST_WEEK -> maxOf(today.minusDays(7), firstLaunchDate)
        UsageAveragePeriod.LAST_MONTH -> maxOf(today.minusDays(30), firstLaunchDate)
        UsageAveragePeriod.LAST_YEAR -> maxOf(today.minusDays(365), firstLaunchDate)
    }
    val dataStartDate = maxOf(today.minusDays(ANALYTICS_PAGER_DAY_COUNT.toLong()), firstLaunchDate)

    return UsageAnalyticsUiState(
        selectedDate = selectedDate,
        today = today,
        dailyTotalMillis = selectedDay.dailyTotalMillis,
        sessionSegments = selectedDay.sessionSegments,
        appTotals = selectedDay.appTotals,
        daySummaries = daySummaries,
        weekdayAverages = dailyTotals.calculateWeekdayAverages(
            startDate = averageStartDate,
            endDateInclusive = today,
        ),
        canNavigateNext = selectedDate.isBefore(today),
        dataStartDate = dataStartDate,
    )
}

private fun buildUsageAnalyticsDayUiState(date: LocalDate, segments: List<SessionSegment>): UsageAnalyticsDayUiState {
    val sortedSegments = segments.sortedBy { it.startDateTime }
    val appTotals = sortedSegments
        .groupBy { it.app }
        .map { (app, appSegments) ->
            AppUsageTotal(
                app = app,
                totalMillis = appSegments.sumOf { it.durationMillis.coerceAtLeast(0L) },
            )
        }
        .filter { it.totalMillis > 0L }
        .sortedByDescending { it.totalMillis }

    return UsageAnalyticsDayUiState(
        date = date,
        dailyTotalMillis = sortedSegments.sumOf { it.durationMillis.coerceAtLeast(0L) },
        sessionSegments = sortedSegments,
        appTotals = appTotals,
    )
}

/**
 * Emits the window containing the current time, then again every time it restarts.
 *
 * The stored window is only written when a viewing session ends, but it expires on its own as time
 * passes. Without this, the repository would emit nothing at a restart and the screen would keep
 * showing the spent window: usage stuck at the allowance, the progress bar full, and the countdown
 * measured from a start that already passed.
 */
private fun IntervalUsage.emitOnEveryRestart(lengthMillis: Long): Flow<IntervalUsage> = flow {
    var usage = this@emitOnEveryRestart

    while (true) {
        // The stored window may have expired while the screen was closed, so start from the one
        // running now rather than the one that was last saved.
        usage = usage.activeIntervalAt(System.currentTimeMillis(), lengthMillis)
        emit(usage)

        // A timer that never started, or one without a length, has no restart to wait for. Looping
        // would emit the same value forever without ever suspending.
        if (!usage.isStarted || lengthMillis <= 0L) return@flow

        // Wake up exactly when this window restarts instead of polling on a fixed tick.
        delay(usage.remainingMillisAt(System.currentTimeMillis(), lengthMillis).milliseconds)
    }
}

private data class UsageSnapshot(val blockingConfig: BlockingConfig, val currentUsage: Long, val sessionSegment: List<SessionSegment>)
