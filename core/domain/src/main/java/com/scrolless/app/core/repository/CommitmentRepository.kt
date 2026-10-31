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
package com.scrolless.app.core.repository

import com.scrolless.app.core.model.ChangeRequestResult
import com.scrolless.app.core.model.CommitmentState
import com.scrolless.app.core.model.SettingsChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Applies the user's settings changes, holding back the ones that loosen blocking while
 * commitment mode is on.
 */
interface CommitmentRepository {

    fun observeState(): Flow<CommitmentState>

    /** Whether [change] would make blocking weaker than it is now. */
    suspend fun isLoosening(change: SettingsChange): Boolean

    /** Applies [change] now, or queues it or asks for the PIN when it loosens blocking. */
    suspend fun request(change: SettingsChange): ChangeRequestResult

    /** Applies [change] if [pin] is right. Returns `false` and changes nothing otherwise. */
    suspend fun applyWithPin(change: SettingsChange, pin: String): Boolean

    suspend fun cancelPendingChange()

    /** Applies the queued change if its time has come. Returns `true` when it applied one. */
    suspend fun applyDuePendingChange(): Boolean

    /** Turns on [com.scrolless.app.core.model.CommitmentMode.Delay]. Does nothing while commitment is already on. */
    suspend fun enableDelay(delayMillis: Long)

    /** Turns on [com.scrolless.app.core.model.CommitmentMode.Pin]. Does nothing while commitment is already on. */
    suspend fun enablePin(pin: String)
}

/** Applies each queued change as soon as it comes due. Runs until cancelled. */
suspend fun CommitmentRepository.applyPendingChangesWhenDue(currentTimeMillis: () -> Long) {
    observeState()
        .map { it.pendingChange?.applyAtMillis }
        .distinctUntilChanged()
        .collectLatest { applyAt ->
            if (applyAt == null) return@collectLatest
            delay((applyAt - currentTimeMillis()).coerceAtLeast(0L))
            try {
                applyDuePendingChange()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A bad queued change must not take the caller down; the next one gets a new try.
                Timber.e(e, "Could not apply the queued change")
            }
        }
}
