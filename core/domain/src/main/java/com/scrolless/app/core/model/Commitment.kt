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
package com.scrolless.app.core.model

import androidx.compose.runtime.Immutable

/** How Scrolless protects the user's limits from being loosened on impulse. */
enum class CommitmentMode {

    /** Every change applies right away. */
    Off,

    /** Loosening changes wait [CommitmentState.delayMillis] before they apply. */
    Delay,

    /** Loosening changes need the PIN. */
    Pin,
}

/** The commitment settings and the loosening change that is waiting to apply, if any. */
@Immutable
data class CommitmentState(
    val mode: CommitmentMode = CommitmentMode.Off,
    val delayMillis: Long = DEFAULT_COMMITMENT_DELAY_MILLIS,
    val pendingChange: PendingChange? = null,
)

/** A loosening [change] queued by [CommitmentMode.Delay] that applies at [applyAtMillis]. */
@Immutable
data class PendingChange(val change: SettingsChange, val applyAtMillis: Long)

/** One change the user asked for that commitment mode may hold back. */
sealed interface SettingsChange {

    data class SelectOption(val option: BlockOption) : SettingsChange

    /** Saves the daily limit and selects [BlockOption.DailyLimit]. */
    data class DailyLimit(val limitMillis: Long) : SettingsChange

    /** Saves the interval settings and selects [BlockOption.IntervalTimer]. */
    data class IntervalTimer(val allowanceMillis: Long, val intervalLengthMillis: Long) : SettingsChange

    data class PauseDuration(val durationMillis: Long) : SettingsChange

    data class IncludeStories(val enabled: Boolean) : SettingsChange

    data class AllowVideosSentByDm(val allowed: Boolean) : SettingsChange

    data object DisableCommitment : SettingsChange
}

/** What [SettingsChange.isLoosening] compares a change against. */
@Immutable
data class CurrentLimits(
    val activeOption: BlockOption,
    val settings: BlockingSettings,
    val pauseDurationMillis: Long,
    val includeStories: Boolean,
    val allowVideosSentByDm: Boolean,
)

/** What happened to a change sent through commitment mode. */
sealed interface ChangeRequestResult {

    data object Applied : ChangeRequestResult

    data class Scheduled(val applyAtMillis: Long) : ChangeRequestResult

    data object PinRequired : ChangeRequestResult
}

const val DEFAULT_COMMITMENT_DELAY_MILLIS = 60 * 60 * 1000L

/** The delays the user can pick for [CommitmentMode.Delay]. */
val COMMITMENT_DELAY_OPTIONS_MILLIS = listOf(15 * 60 * 1000L, DEFAULT_COMMITMENT_DELAY_MILLIS, 24 * 60 * 60 * 1000L)

/**
 * Whether this change makes blocking weaker than [current].
 *
 * Daily limit and interval timer can't be ranked against each other, so switching between them
 * counts as loosening.
 */
fun SettingsChange.isLoosening(current: CurrentLimits): Boolean = when (this) {
    is SettingsChange.SelectOption -> option != current.activeOption && option.strictness() <= current.activeOption.strictness()

    is SettingsChange.DailyLimit -> when (current.activeOption) {
        BlockOption.NothingSelected -> false
        BlockOption.DailyLimit -> limitMillis > current.settings.dailyLimitMillis
        else -> true
    }

    is SettingsChange.IntervalTimer -> when (current.activeOption) {
        BlockOption.NothingSelected -> false

        BlockOption.IntervalTimer ->
            allowanceMillis > current.settings.intervalAllowanceMillis ||
                intervalLengthMillis < current.settings.intervalLengthMillis

        else -> true
    }

    is SettingsChange.PauseDuration -> durationMillis > current.pauseDurationMillis

    is SettingsChange.IncludeStories -> !enabled && current.includeStories

    is SettingsChange.AllowVideosSentByDm -> allowed && !current.allowVideosSentByDm

    SettingsChange.DisableCommitment -> true
}

private fun BlockOption.strictness(): Int = when (this) {
    BlockOption.BlockAll -> 2
    BlockOption.DailyLimit, BlockOption.IntervalTimer -> 1
    BlockOption.NothingSelected -> 0
}
