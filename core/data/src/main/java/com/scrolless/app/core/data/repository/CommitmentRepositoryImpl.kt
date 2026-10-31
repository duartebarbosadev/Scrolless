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
package com.scrolless.app.core.data.repository

import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.data.commitment.PinHasher
import com.scrolless.app.core.data.commitment.SettingsChangeCodec
import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.data.database.model.UserSettingsEntity
import com.scrolless.app.core.data.database.model.toBlockingConfig
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.ChangeRequestResult
import com.scrolless.app.core.model.CommitmentMode
import com.scrolless.app.core.model.CommitmentState
import com.scrolless.app.core.model.CurrentLimits
import com.scrolless.app.core.model.PendingChange
import com.scrolless.app.core.model.SettingsChange
import com.scrolless.app.core.model.isLoosening
import com.scrolless.app.core.repository.BlockingConfigRepository
import com.scrolless.app.core.repository.CommitmentRepository
import com.scrolless.app.core.repository.UserSettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Keeps commitment mode and its single queued change in the `user_settings` row.
 *
 * Only one change waits at a time. Asking for another loosening change replaces it and restarts
 * the delay, so changes can't be stacked up ahead of time.
 */
class CommitmentRepositoryImpl @Inject constructor(
    private val userSettingsDao: UserSettingsDao,
    private val blockingConfigRepository: BlockingConfigRepository,
    private val userSettingsStore: UserSettingsStore,
    private val timeProvider: TimeProvider,
) : CommitmentRepository {

    // The service and the app can both try to apply the queued change when it comes due.
    private val mutex = Mutex()

    override fun observeState(): Flow<CommitmentState> = userSettingsDao.observeUserSettings()
        .map { it.toCommitmentState() }
        .distinctUntilChanged()

    override suspend fun isLoosening(change: SettingsChange): Boolean =
        change.isLoosening(userSettingsDao.getUserSettings().toCurrentLimits())

    override suspend fun request(change: SettingsChange): ChangeRequestResult = mutex.withLock {
        val settings = userSettingsDao.getUserSettings()
        val mode = settings.commitmentMode
        if (mode == CommitmentMode.Off || !change.isLoosening(settings.toCurrentLimits())) {
            apply(change)
            return@withLock ChangeRequestResult.Applied
        }
        when (mode) {
            CommitmentMode.Pin -> ChangeRequestResult.PinRequired

            else -> {
                val applyAt = timeProvider.currentTimeInMillis() + settings.commitmentDelayMillis
                Timber.i("Queued loosening change until %d", applyAt)
                userSettingsDao.setPendingChange(SettingsChangeCodec.encode(change), applyAt)
                ChangeRequestResult.Scheduled(applyAt)
            }
        }
    }

    override suspend fun applyWithPin(change: SettingsChange, pin: String): Boolean = mutex.withLock {
        val settings = userSettingsDao.getUserSettings()
        if (!PinHasher.matches(pin, settings.commitmentPinHash)) {
            Timber.i("Wrong commitment PIN")
            return@withLock false
        }
        apply(change)
        true
    }

    override suspend fun cancelPendingChange() = mutex.withLock {
        userSettingsDao.setPendingChange(null, 0L)
    }

    override suspend fun applyDuePendingChange(): Boolean = mutex.withLock {
        val settings = userSettingsDao.getUserSettings()
        if (settings.pendingChange == null || settings.pendingChangeApplyAt > timeProvider.currentTimeInMillis()) {
            return@withLock false
        }
        val change = SettingsChangeCodec.decode(settings.pendingChange)
        userSettingsDao.setPendingChange(null, 0L)
        if (change == null) {
            Timber.w("Dropping unreadable pending change: %s", settings.pendingChange)
            return@withLock false
        }
        Timber.i("Applying queued change")
        apply(change)
        true
    }

    override suspend fun enableDelay(delayMillis: Long) = mutex.withLock {
        require(delayMillis > 0L) { "Commitment delay must be greater than zero" }
        // Switching straight from one mode to another would skip the protection of the current one.
        if (userSettingsDao.getUserSettings().commitmentMode != CommitmentMode.Off) {
            Timber.w("Ignoring delay mode: commitment is already on")
            return@withLock
        }
        userSettingsDao.setCommitment(CommitmentMode.Delay, delayMillis, pinHash = null)
    }

    override suspend fun enablePin(pin: String) = mutex.withLock {
        require(pin.length >= MIN_PIN_LENGTH) { "PIN is too short" }
        val settings = userSettingsDao.getUserSettings()
        if (settings.commitmentMode != CommitmentMode.Off) {
            Timber.w("Ignoring PIN mode: commitment is already on")
            return@withLock
        }
        userSettingsDao.setCommitment(CommitmentMode.Pin, settings.commitmentDelayMillis, PinHasher.hash(pin))
    }

    private suspend fun apply(change: SettingsChange) {
        when (change) {
            is SettingsChange.SelectOption -> {
                blockingConfigRepository.setActiveOption(change.option)
                if (change.option == BlockOption.NothingSelected) userSettingsStore.setPauseUntil(0L)
            }

            is SettingsChange.DailyLimit -> blockingConfigRepository.configureDailyLimit(change.limitMillis)

            is SettingsChange.IntervalTimer -> blockingConfigRepository.configureIntervalTimer(
                allowanceMillis = change.allowanceMillis,
                intervalLengthMillis = change.intervalLengthMillis,
            )

            is SettingsChange.PauseDuration -> userSettingsStore.setPauseDuration(change.durationMillis)

            is SettingsChange.IncludeStories -> userSettingsStore.setIncludeStories(change.enabled)

            is SettingsChange.AllowVideosSentByDm -> userSettingsStore.setAllowVideosSentByDm(change.allowed)

            SettingsChange.DisableCommitment -> {
                val settings = userSettingsDao.getUserSettings()
                userSettingsDao.setCommitment(CommitmentMode.Off, settings.commitmentDelayMillis, pinHash = null)
                userSettingsDao.setPendingChange(null, 0L)
            }
        }
    }

    companion object {
        const val MIN_PIN_LENGTH = 4
    }
}

private fun UserSettingsEntity.toCommitmentState(): CommitmentState = CommitmentState(
    mode = commitmentMode,
    delayMillis = commitmentDelayMillis,
    pendingChange = SettingsChangeCodec.decode(pendingChange)?.let { PendingChange(it, pendingChangeApplyAt) },
)

private fun UserSettingsEntity.toCurrentLimits(): CurrentLimits = CurrentLimits(
    activeOption = activeBlockOption,
    settings = toBlockingConfig().settings,
    pauseDurationMillis = pauseDurationMillis,
    includeStories = includeStories,
    allowVideosSentByDm = allowVideosSentByDm,
)
