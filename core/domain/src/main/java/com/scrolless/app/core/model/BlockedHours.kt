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

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * The hours of the day when blocked content is closed right away, whatever allowance is left.
 *
 * Bit `n` of [mask] marks the hour that starts at `n:00` local time, so `0` blocks no hours.
 */
@JvmInline
value class BlockedHours(val mask: Int) {

    val isEmpty: Boolean
        get() = (mask and ALL_HOURS_MASK) == 0

    fun isBlocked(hour: Int): Boolean = hour in HOURS && (mask and (1 shl hour)) != 0

    fun isBlockedAt(dateTime: LocalDateTime): Boolean = isBlocked(dateTime.hour)

    fun toggle(hour: Int): BlockedHours {
        require(hour in HOURS) { "Hour must be between 0 and 23" }
        return BlockedHours(mask xor (1 shl hour))
    }

    /**
     * Milliseconds from [dateTime] until the next blocked hour starts, or `null` if no hour is blocked.
     *
     * Returns `0` when [dateTime] is already inside a blocked hour.
     */
    fun millisUntilNextBlocked(dateTime: LocalDateTime): Long? {
        if (isEmpty) return null
        if (isBlockedAt(dateTime)) return 0L

        val nextHourStart = dateTime.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        val millisToNextHour = ChronoUnit.MILLIS.between(dateTime, nextHourStart)
        val hoursAhead = (0 until HOURS_PER_DAY).first { isBlocked((dateTime.hour + 1 + it) % HOURS_PER_DAY) }
        return millisToNextHour + hoursAhead * HOUR_MILLIS
    }

    /**
     * The blocked hours as continuous ranges of start hour to end hour, ordered by start.
     *
     * The end is exclusive, so 20:00 to 06:00 is `20 to 6`. A range that crosses midnight is kept
     * whole, and blocking every hour returns `0 to 0`.
     */
    fun ranges(): List<Pair<Int, Int>> {
        if (isEmpty) return emptyList()
        if ((mask and ALL_HOURS_MASK) == ALL_HOURS_MASK) return listOf(0 to 0)

        return HOURS
            .filter { isBlocked(it) && !isBlocked((it + HOURS_PER_DAY - 1) % HOURS_PER_DAY) }
            .map { start ->
                var end = start
                while (isBlocked(end % HOURS_PER_DAY)) end++
                start to end % HOURS_PER_DAY
            }
    }

    companion object {

        const val HOURS_PER_DAY = 24
        private const val ALL_HOURS_MASK = (1 shl HOURS_PER_DAY) - 1
        private const val HOUR_MILLIS = 60 * 60 * 1000L
        private val HOURS = 0 until HOURS_PER_DAY

        val NONE = BlockedHours(0)

        fun of(vararg hours: Int): BlockedHours = hours.fold(NONE) { blocked, hour ->
            if (blocked.isBlocked(hour)) blocked else blocked.toggle(hour)
        }
    }
}
