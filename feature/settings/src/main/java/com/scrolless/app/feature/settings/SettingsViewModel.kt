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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.repository.UserSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(private val userSettingsStore: UserSettingsStore) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userSettingsStore.getPauseDuration(),
        userSettingsStore.getExceptReelsSentByDm(),
        userSettingsStore.getTimerOverlayEnabled(),
    ) { pauseDurationMillis, exceptReelsSentByDm, timerOverlayEnabled ->
        SettingsUiState(
            pauseDurationMinutes = (pauseDurationMillis / 60_000L).toInt().coerceIn(1, 60),
            exceptReelsSentByDm = exceptReelsSentByDm,
            timerOverlayEnabled = timerOverlayEnabled,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun onPauseDurationChange(minutes: Int) {
        viewModelScope.launch {
            userSettingsStore.setPauseDuration(minutes * 60_000L)
        }
    }

    fun onExceptReelsSentByDmChange(checked: Boolean) {
        viewModelScope.launch {
            userSettingsStore.setExceptReelsSentByDm(checked)
        }
    }

    fun onTimerOverlayEnabledChange(checked: Boolean) {
        viewModelScope.launch {
            userSettingsStore.setTimerOverlayToggle(checked)
        }
    }
}

data class SettingsUiState(
    val pauseDurationMinutes: Int = 5,
    val exceptReelsSentByDm: Boolean = false,
    val timerOverlayEnabled: Boolean = false,
)
