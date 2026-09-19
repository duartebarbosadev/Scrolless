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
package com.scrolless.app.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.data.repository.OnboardingPreferences
import com.scrolless.app.core.data.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingGateViewModel @Inject constructor(repository: OnboardingRepository) : ViewModel() {
    // Null prevents a flash of Home (and its permission sheet) before Room finishes loading.
    val completed: StateFlow<Boolean?> = repository.completed.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val repository: OnboardingRepository, private val savedState: SavedStateHandle) :
    ViewModel() {
    val preferences = savedState.getStateFlow<OnboardingPreferences?>("preferences", null)
    val saving = MutableStateFlow(false)
    val failed = MutableStateFlow(false)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            failed.value = false
            try {
                if (preferences.value == null) savedState["preferences"] = repository.load()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                failed.value = true
            }
        }
    }

    fun update(preferences: OnboardingPreferences) {
        savedState["preferences"] = preferences
    }

    fun finish(skip: Boolean = false, onFinished: () -> Unit) {
        if (saving.value) return
        val draft = preferences.value ?: return
        saving.value = true
        failed.value = false
        viewModelScope.launch {
            try {
                if (skip) repository.skip() else repository.save(draft)
                onFinished()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                failed.value = true
            } finally {
                saving.value = false
            }
        }
    }
}
