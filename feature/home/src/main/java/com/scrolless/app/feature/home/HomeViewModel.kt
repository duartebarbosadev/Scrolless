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
import com.scrolless.app.core.model.SessionSegment
import com.scrolless.app.core.model.usage.DailyUsageTotal
import com.scrolless.app.core.model.usage.calculateWeekdayAverages
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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

    private val requestReview = kotlinx.coroutines.flow.combine(
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

    private val usageSnapshot = combine(
        userSettingsStore.getActiveBlockOption(),
        userSettingsStore.getTimeLimit(),
        userSettingsStore.getIntervalLength(),
        userSettingsStore.getIntervalUsage(),
        userSettingsStore.getIntervalWindowStart(),
        sessionSegmentStore.getTotalDurationForToday(),
        sessionSegmentsForCurrentDay,
    ) { blockOption, timeLimit, intervalLength, intervalUsage, intervalWindowStart, currentUsage, usageSegment ->
        UsageSnapshot(
            blockOption = blockOption,
            timeLimit = timeLimit,
            intervalLength = intervalLength,
            intervalUsage = intervalUsage,
            intervalWindowStart = intervalWindowStart,
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
            blockOption = usage.blockOption,
            currentUsage = usage.currentUsage,
            timeLimit = usage.timeLimit,
            intervalUsage = usage.intervalUsage,
        )

        HomeUiState(
            blockOption = usage.blockOption,
            timeLimit = usage.timeLimit,
            intervalLength = usage.intervalLength,
            intervalUsage = usage.intervalUsage,
            intervalWindowStart = usage.intervalWindowStart,
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
            userSettingsStore.setActiveBlockOption(blockOption)
            if (blockOption == BlockOption.NothingSelected) {
                onPauseToggle(false)
            }
        }
    }

    fun onTimeLimitChange(durationMillis: Long) {
        Timber.d("Time limit changed: %d ms", durationMillis)
        viewModelScope.launch {
            userSettingsStore.setActiveBlockOption(BlockOption.DailyLimit)
            userSettingsStore.setTimeLimit(durationMillis)
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
            userSettingsStore.setIntervalLength(intervalBreakMillis)
            userSettingsStore.setTimeLimit(allowanceMillis)
            userSettingsStore.updateIntervalState(windowStart = 0L, usage = 0L)
            userSettingsStore.setActiveBlockOption(BlockOption.IntervalTimer)
        }
    }

    /**
     * Computes progress percentage for the active blocking mode.
     *
     * Daily mode uses [currentUsage] against [timeLimit].
     * Interval mode uses [intervalUsage] against [timeLimit].
     *
     * @param blockOption Active blocking strategy.
     * @param currentUsage Total usage accumulated for the current day.
     * @param timeLimit Configured limit in milliseconds.
     * @param intervalUsage Usage accumulated in the current interval window.
     * @return Progress in integer percent, clamped to the [0, 100] range.
     */
    private fun calculateProgress(blockOption: BlockOption, currentUsage: Long, timeLimit: Long, intervalUsage: Long): Int =
        when (blockOption) {
            BlockOption.DailyLimit -> usageToProgress(usage = currentUsage, limit = timeLimit)
            BlockOption.IntervalTimer -> usageToProgress(usage = intervalUsage, limit = timeLimit)
            else -> 0
        }

    /**
     * Converts a usage/limit pair to an integer percentage.
     *
     * For non-zero usage below the limit, returns at least `1` so tiny progress
     * remains visible in the UI.
     *
     * @param usage Elapsed usage in milliseconds.
     * @param limit Allowed usage in milliseconds.
     * @return Progress in integer percent, clamped to the [0, 100] range.
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
            delay(delayMillis)
        }
    }.distinctUntilChanged()
}

@Immutable
data class HomeUiState(
    val blockOption: BlockOption = BlockOption.NothingSelected,
    val timeLimit: Long = 0L,
    val intervalLength: Long = 0L,
    val intervalUsage: Long = 0L,
    val intervalWindowStart: Long = 0L,
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
     * True once the initial values from [UserSettingsStore] have been emitted at least once.
     *
     * Home screen side effects gate on this flag to avoid running before persisted settings load
     * (e.g., auto-showing the accessibility explainer on the very first launch).
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

private data class UsageSnapshot(
    val blockOption: BlockOption,
    val timeLimit: Long,
    val intervalLength: Long,
    val intervalUsage: Long,
    val intervalWindowStart: Long,
    val currentUsage: Long,
    val sessionSegment: List<SessionSegment>,
)
