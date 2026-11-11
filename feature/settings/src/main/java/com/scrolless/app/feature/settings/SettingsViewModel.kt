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
package com.scrolless.app.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.model.ChangeRequestResult
import com.scrolless.app.core.model.CommitmentMode
import com.scrolless.app.core.model.CommitmentState
import com.scrolless.app.core.model.SettingsChange
import com.scrolless.app.core.repository.CommitmentRepository
import com.scrolless.app.core.repository.UserSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
    private val commitmentRepository: CommitmentRepository,
) : ViewModel() {

    private val prompt = MutableStateFlow<SettingsPrompt?>(null)

    private val preferences = combine(
        userSettingsStore.getPauseDuration(),
        userSettingsStore.getAllowVideosSentByDm(),
        userSettingsStore.getTimerOverlayEnabled(),
        userSettingsStore.getIncludeStories(),
    ) { pauseDurationMillis, allowVideosSentByDm, timerOverlayEnabled, includeStories ->
        SettingsUiState(
            pauseDurationMinutes = (pauseDurationMillis / 60_000L).toInt().coerceIn(1, 60),
            allowVideosSentByDm = allowVideosSentByDm,
            timerOverlayEnabled = timerOverlayEnabled,
            includeStories = includeStories,
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences,
        commitmentRepository.observeState(),
        prompt,
    ) { preferences, commitment, prompt ->
        preferences.copy(commitment = commitment, prompt = prompt)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun onIncludeStoriesChange(enabled: Boolean) {
        submitChange(SettingsChange.IncludeStories(enabled))
    }

    fun onPauseDurationChange(minutes: Int) {
        submitChange(SettingsChange.PauseDuration(minutes * 60_000L))
    }

    fun onAllowVideosSentByDmChange(checked: Boolean) {
        submitChange(SettingsChange.AllowVideosSentByDm(checked))
    }

    fun onTimerOverlayEnabledChange(checked: Boolean) {
        viewModelScope.launch {
            userSettingsStore.setTimerOverlayToggle(checked)
        }
    }

    /** Picking a mode while another one is on asks to turn commitment off first, which is held back too. */
    fun onCommitmentModeSelected(mode: CommitmentMode) {
        val current = uiState.value.commitment.mode
        if (mode == current) return
        when {
            current != CommitmentMode.Off -> submitChange(SettingsChange.DisableCommitment)
            mode == CommitmentMode.Delay -> prompt.value = SettingsPrompt.ChooseDelay
            mode == CommitmentMode.Pin -> prompt.value = SettingsPrompt.ChoosePin()
        }
    }

    fun onCommitmentDelayChosen(delayMillis: Long) {
        prompt.value = null
        viewModelScope.launch {
            Timber.i("Commitment mode: delay of %d ms", delayMillis)
            commitmentRepository.enableDelay(delayMillis)
        }
    }

    /** The first entry is kept until the user types the same PIN again. */
    fun onNewPinEntered(pin: String) {
        val current = prompt.value as? SettingsPrompt.ChoosePin ?: return
        val firstEntry = current.firstEntry
        when {
            firstEntry == null -> prompt.value = SettingsPrompt.ChoosePin(firstEntry = pin)

            firstEntry != pin -> prompt.value = SettingsPrompt.ChoosePin(mismatch = true)

            else -> {
                prompt.value = null
                viewModelScope.launch {
                    Timber.i("Commitment mode: PIN")
                    commitmentRepository.enablePin(pin)
                }
            }
        }
    }

    fun onPinSubmitted(pin: String) {
        val current = prompt.value as? SettingsPrompt.EnterPin ?: return
        viewModelScope.launch {
            val applied = commitmentRepository.applyWithPin(current.change, pin)
            prompt.value = if (applied) null else current.copy(wrongPin = true)
        }
    }

    fun onPromptDismissed() {
        prompt.value = null
    }

    fun onCancelPendingChange() {
        viewModelScope.launch {
            commitmentRepository.cancelPendingChange()
        }
    }

    private fun submitChange(change: SettingsChange) {
        viewModelScope.launch {
            prompt.value = when (val result = commitmentRepository.request(change)) {
                ChangeRequestResult.Applied -> null
                ChangeRequestResult.PinRequired -> SettingsPrompt.EnterPin(change)
                is ChangeRequestResult.Scheduled -> SettingsPrompt.Scheduled(result.applyAtMillis)
            }
        }
    }
}

@Immutable
data class SettingsUiState(
    val includeStories: Boolean = false,
    val pauseDurationMinutes: Int = 5,
    val allowVideosSentByDm: Boolean = false,
    val timerOverlayEnabled: Boolean = true,
    val commitment: CommitmentState = CommitmentState(),
    val prompt: SettingsPrompt? = null,
)

/** A dialog the Settings screen needs the user to answer. */
@Immutable
sealed interface SettingsPrompt {

    data object ChooseDelay : SettingsPrompt

    /** [firstEntry] is set while the user confirms the PIN they just typed. */
    data class ChoosePin(val firstEntry: String? = null, val mismatch: Boolean = false) : SettingsPrompt

    data class EnterPin(val change: SettingsChange, val wrongPin: Boolean = false) : SettingsPrompt

    data class Scheduled(val applyAtMillis: Long) : SettingsPrompt
}
