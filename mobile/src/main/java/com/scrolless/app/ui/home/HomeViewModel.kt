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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.UsageSegment
import com.scrolless.app.core.repository.UsageSegmentStore
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.core.util.combine
import com.scrolless.app.util.ReviewPromptResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel that handles the business logic and screen state of the Podcast details screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
    private val usageSegmentStore: UsageSegmentStore,
) : ViewModel() {

    private val _showComingSoonSnackBar = MutableStateFlow(false)
    private val _requestReview = MutableStateFlow(false)
    // Keep latest persisted attempt metadata so we can update without re-collecting.
    private var latestReviewAttemptCount = 0
    private var latestReviewAttemptAt = 0L

    companion object {
        private const val PROGRESS_MAX = 100
        private const val PAUSE_DURATION_MILLIS = 5 * 60 * 1000L
        private val REVIEW_PROMPT_DELAY_MILLIS = TimeUnit.DAYS.toMillis(1) // Show review popup after 1 day
        private const val REVIEW_PROMPT_MAX_ATTEMPTS = 3
        private val REVIEW_PROMPT_RETRY_DELAY_MILLIS = TimeUnit.DAYS.toMillis(2)
    }

    init {
        viewModelScope.launch {
            // Gate review prompts by first launch, attempt count, and retry delay
            kotlinx.coroutines.flow.combine(
                userSettingsStore.getFirstLaunchAt(),
                userSettingsStore.getHasSeenReviewPrompt(),
                userSettingsStore.getReviewPromptAttemptCount(),
                userSettingsStore.getReviewPromptLastAttemptAt(),
            ) { firstLaunchAt, hasSeenReviewPrompt, attemptCount, lastAttemptAt ->
                ReviewPromptSnapshot(firstLaunchAt, hasSeenReviewPrompt, attemptCount, lastAttemptAt)
            }.collect { snapshot ->
                val now = System.currentTimeMillis()
                latestReviewAttemptCount = snapshot.attemptCount
                latestReviewAttemptAt = snapshot.lastAttemptAt
                val resolvedFirstLaunch = if (snapshot.firstLaunchAt == 0L) {
                    userSettingsStore.setFirstLaunchAt(now)
                    now
                } else {
                    snapshot.firstLaunchAt
                }

                // Avoid spamming
                // Require a initial delay, a retry cooldown, and a max attempt cap.
                val shouldPrompt = !snapshot.hasSeenReviewPrompt &&
                    snapshot.attemptCount < REVIEW_PROMPT_MAX_ATTEMPTS &&
                    (snapshot.lastAttemptAt == 0L || now - snapshot.lastAttemptAt >= REVIEW_PROMPT_RETRY_DELAY_MILLIS) &&
                    now - resolvedFirstLaunch >= REVIEW_PROMPT_DELAY_MILLIS

                if (shouldPrompt && !_requestReview.value) {
                    _requestReview.value = true
                } else {
                    Timber.d(
                        "Review prompt not eligible: hasSeen=%s, attempts=%d, lastAttemptAt=%d",
                        snapshot.hasSeenReviewPrompt,
                        snapshot.attemptCount,
                        snapshot.lastAttemptAt,
                    )
                }
            }
        }
    }

    private val usageSnapshot = combine(
        userSettingsStore.getActiveBlockOption(),
        userSettingsStore.getTimeLimit(),
        userSettingsStore.getIntervalLength(),
        userSettingsStore.getIntervalUsage(),
        userSettingsStore.getIntervalWindowStart(),
        userSettingsStore.getTotalDailyUsage(),
        usageSegmentStore.getUsageSegment(LocalDate.now()),
    ) { blockOption, timeLimit, intervalLength, intervalUsage, intervalWindowStart, currentUsage, usageSegment ->
        UsageSnapshot(
            blockOption = blockOption,
            timeLimit = timeLimit,
            intervalLength = intervalLength,
            intervalUsage = intervalUsage,
            intervalWindowStart = intervalWindowStart,
            currentUsage = currentUsage,
            usageSegment = usageSegment,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        usageSnapshot,
        userSettingsStore.getTimerOverlayEnabled(),
        userSettingsStore.getPauseUntil(),
        _showComingSoonSnackBar,
        _requestReview,
        userSettingsStore.getHasSeenAccessibilityExplainer(),
    ) { usage, timerEnabled, pauseUntil, showComingSoonSnackBar, requestReview, hasSeenAccessibilityExplainer ->

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
            timerOverlayEnabled = timerEnabled,
            pauseUntilMillis = pauseUntil,
            showComingSoonSnackBar = showComingSoonSnackBar,
            requestReview = requestReview,
            hasSeenAccessibilityExplainer = hasSeenAccessibilityExplainer,
            hasLoadedSettings = true,
            listUsageSegments = usage.usageSegment,
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

    fun onScreenTimerToggled(enabled: Boolean) {
        Timber.d("On-screen timer toggled: %s", enabled)
        viewModelScope.launch {
            userSettingsStore.setTimerOverlayToggle(enabled)
        }
    }

    fun onPauseToggle(shouldPause: Boolean) {
        val targetTimestamp = if (shouldPause) {
            System.currentTimeMillis() + PAUSE_DURATION_MILLIS
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

    private fun calculateProgress(blockOption: BlockOption, currentUsage: Long, timeLimit: Long, intervalUsage: Long): Int =
        when (blockOption) {
            BlockOption.DailyLimit -> {
                if (timeLimit > 0) {
                    val remainingTime = timeLimit - currentUsage
                    if (remainingTime > 0) {
                        min(PROGRESS_MAX, ((currentUsage.toDouble() / timeLimit.toDouble()) * PROGRESS_MAX).toInt())
                    } else {
                        PROGRESS_MAX
                    }
                } else {
                    0
                }
            }

            BlockOption.IntervalTimer -> {
                if (timeLimit > 0) {
                    min(PROGRESS_MAX, ((intervalUsage.toDouble() / timeLimit.toDouble()) * PROGRESS_MAX).toInt())
                } else {
                    0
                }
            }

            else -> 0
        }

    fun onSnackbarShown() {
        Timber.v("Snackbar dismissed")
        _showComingSoonSnackBar.value = false
    }

    fun onReviewRequestHandled() {
        Timber.v("Review request handled")
        _requestReview.value = false
    }

    fun onReviewRequestStarted() {
        val now = System.currentTimeMillis()
        val nextAttemptCount = latestReviewAttemptCount + 1
        latestReviewAttemptCount = nextAttemptCount
        latestReviewAttemptAt = now

        viewModelScope.launch {
            userSettingsStore.setReviewPromptAttemptCount(nextAttemptCount)
            userSettingsStore.setReviewPromptLastAttemptAt(now)
        }
    }

    fun onReviewPromptResult(result: ReviewPromptResult) {
        // Treat permanent failures and max retry exhaustion as terminal.
        val shouldMarkSeen = when (result) {
            ReviewPromptResult.Shown -> true
            ReviewPromptResult.SkippedPermanent -> true
            ReviewPromptResult.SkippedTemporary -> false
        } || latestReviewAttemptCount >= REVIEW_PROMPT_MAX_ATTEMPTS

        if (!shouldMarkSeen) {
            Timber.d("Review prompt was not shown; leaving eligible for future prompts.")
            return
        }

        viewModelScope.launch {
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

    fun onDebugUsageChanged(reelsMinutes: Int, shortsMinutes: Int, tiktokMinutes: Int) {
        viewModelScope.launch {
            val reelsUsage = TimeUnit.MINUTES.toMillis(reelsMinutes.coerceAtLeast(0).toLong())
            val shortsUsage = TimeUnit.MINUTES.toMillis(shortsMinutes.coerceAtLeast(0).toLong())
            val tiktokUsage = TimeUnit.MINUTES.toMillis(tiktokMinutes.coerceAtLeast(0).toLong())
            userSettingsStore.updateReelsDailyUsage(reelsUsage)
            userSettingsStore.updateShortsDailyUsage(shortsUsage)
            userSettingsStore.updateTiktokDailyUsage(tiktokUsage)
            userSettingsStore.updateTotalDailyUsage(reelsUsage + shortsUsage + tiktokUsage)
        }
    }

    fun onDebugResetUsage() {
        viewModelScope.launch {
            userSettingsStore.resetAllDailyUsage()
        }
    }
}

data class HomeUiState(
    val blockOption: BlockOption = BlockOption.NothingSelected,
    val timeLimit: Long = 0L,
    val intervalLength: Long = 0L,
    val intervalUsage: Long = 0L,
    val intervalWindowStart: Long = 0L,
    val currentUsage: Long = 0L,
    val progress: Int = 0,
    val timerOverlayEnabled: Boolean = false,
    val showComingSoonSnackBar: Boolean = false,
    val requestReview: Boolean = false,
    val isDevMode: Boolean = false,
    val playStoreUrl: String? = null,
    val pauseUntilMillis: Long = 0L,
    val hasSeenAccessibilityExplainer: Boolean = false,

    /**
     * True once the initial values from [UserSettingsStore] have been emitted at least once.
     *
     * Home screen side-effects gate on this flag to avoid running before persisted settings load
     * (e.g., auto-showing the accessibility explainer on the very first launch).
     */
    val hasLoadedSettings: Boolean = false,

    /**
     * Per-app usage breakdown for the segmented progress indicator.
     */
    val listUsageSegments: List<UsageSegment> = emptyList(),
)

private data class UsageSnapshot(
    val blockOption: BlockOption,
    val timeLimit: Long,
    val intervalLength: Long,
    val intervalUsage: Long,
    val intervalWindowStart: Long,
    val currentUsage: Long,
    val usageSegment: List<UsageSegment>,
)

private data class ReviewPromptSnapshot(
    val firstLaunchAt: Long,
    val hasSeenReviewPrompt: Boolean,
    val attemptCount: Int,
    val lastAttemptAt: Long,
)
