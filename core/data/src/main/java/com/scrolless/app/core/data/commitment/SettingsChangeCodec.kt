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
package com.scrolless.app.core.data.commitment

import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.SettingsChange

/**
 * Stores a [SettingsChange] in the `pending_change` column as `type:arg:arg`.
 *
 * The column is free text, so [decode] returns `null` for anything it can't read instead of
 * throwing.
 */
internal object SettingsChangeCodec {

    private const val SEPARATOR = ":"

    fun encode(change: SettingsChange): String = when (change) {
        is SettingsChange.SelectOption -> join("option", change.option.name)
        is SettingsChange.DailyLimit -> join("daily", change.limitMillis)
        is SettingsChange.IntervalTimer -> join("interval", change.allowanceMillis, change.intervalLengthMillis)
        is SettingsChange.PauseDuration -> join("pause", change.durationMillis)
        is SettingsChange.IncludeStories -> join("stories", change.enabled)
        is SettingsChange.AllowVideosSentByDm -> join("dm", change.allowed)
        SettingsChange.DisableCommitment -> "commitment_off"
    }

    fun decode(value: String?): SettingsChange? {
        val parts = value?.split(SEPARATOR) ?: return null
        val args = parts.drop(1)
        return when (parts.first()) {
            "option" -> args.singleOrNull()
                ?.let { name -> BlockOption.entries.firstOrNull { it.name == name } }
                ?.let(SettingsChange::SelectOption)

            "daily" -> args.singleOrNull()?.toLongOrNull()?.let(SettingsChange::DailyLimit)

            "interval" -> {
                val allowance = args.getOrNull(0)?.toLongOrNull()
                val length = args.getOrNull(1)?.toLongOrNull()
                if (args.size == 2 && allowance != null && length != null) SettingsChange.IntervalTimer(allowance, length) else null
            }

            "pause" -> args.singleOrNull()?.toLongOrNull()?.let(SettingsChange::PauseDuration)

            "stories" -> args.singleOrNull()?.toBooleanStrictOrNull()?.let(SettingsChange::IncludeStories)

            "dm" -> args.singleOrNull()?.toBooleanStrictOrNull()?.let(SettingsChange::AllowVideosSentByDm)

            "commitment_off" -> if (args.isEmpty()) SettingsChange.DisableCommitment else null

            else -> null
        }
    }

    private fun join(type: String, vararg args: Any): String = (listOf(type) + args.map(Any::toString)).joinToString(SEPARATOR)
}
