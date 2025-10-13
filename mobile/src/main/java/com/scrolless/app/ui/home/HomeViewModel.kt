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
import com.scrolless.app.core.data.database.model.BlockOption
import com.scrolless.app.core.data.repository.UserSettingsStore
import com.scrolless.app.core.util.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min

/**
 * ViewModel that handles the business logic and screen state of the Podcast details screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
) : ViewModel() {

    private val _showComingSoonSnackBar = MutableStateFlow(false)
    private val _requestReview = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        userSettingsStore.getActiveBlockOption(),
        userSettingsStore.getTimeLimit(),
        userSettingsStore.getTotalDailyUsage(),
        userSettingsStore.getTimerOverlayEnabled(),
        _showComingSoonSnackBar,
        _requestReview,
    ) { blockOption, timeLimit, currentUsage, timerEnabled, showComingSoonSnackBar, requestReview ->

        val progress = calculateProgress(
            currentUsage = if (blockOption == BlockOption.DailyLimit) currentUsage else 0L,
            timeLimit = timeLimit,
        )

        HomeUiState(
            blockOption = blockOption,
            timeLimit = timeLimit,
            currentUsage = currentUsage,
            progress = progress,
            timerOverlayEnabled = timerEnabled,
            showComingSoonSnackBar = showComingSoonSnackBar,
            requestReview = requestReview,
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
        }
    }

    fun onTimeLimitChange(durationMillis: Long) {
        Timber.d("Time limit changed: %d ms", durationMillis)
        viewModelScope.launch {
            userSettingsStore.setActiveBlockOption(BlockOption.DailyLimit)
            userSettingsStore.setTimeLimit(durationMillis)
        }
    }

    fun onTimerOverlayToggled(enabled: Boolean) {
        Timber.d("Timer overlay toggled: %s", enabled)
        viewModelScope.launch {
            userSettingsStore.setTimerOverlayToggle(enabled)
        }
    }

    private fun calculateProgress(currentUsage: Long, timeLimit: Long): Int {
        return if (timeLimit > 0) {
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

    fun onSnackbarShown() {
        Timber.v("Snackbar dismissed")
        _showComingSoonSnackBar.value = false
    }

    fun onFeatureComingSoon() {
        Timber.i("Feature coming soon clicked")
        _showComingSoonSnackBar.value = true
    }

    fun onReviewRequested() {
        Timber.i("Review requested")
        _requestReview.value = true
    }

    fun onReviewRequestHandled() {
        Timber.v("Review request handled")
        _requestReview.value = false
    }

    fun setWaitingForAccessibility(waiting: Boolean) {
        Timber.d("Setting waiting for accessibility: %s", waiting)
        viewModelScope.launch {
            userSettingsStore.setWaitingForAccessibility(waiting)
        }
    }

    companion object {
        private const val PROGRESS_MAX = 100
    }
}

data class HomeUiState(
    val blockOption: BlockOption = BlockOption.NothingSelected,
    val timeLimit: Long = 0L,
    val currentUsage: Long = 0L,
    val progress: Int = 0,
    val timerOverlayEnabled: Boolean = false,
    val showComingSoonSnackBar: Boolean = false,
    val requestReview: Boolean = false,
    val isDevMode: Boolean = false,
    val playStoreUrl: String? = null,
)
