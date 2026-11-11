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
package com.scrolless.app.core.domain.model

import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.model.BlockingSettings
import com.scrolless.app.core.model.CurrentLimits
import com.scrolless.app.core.model.SettingsChange
import com.scrolless.app.core.model.isLoosening
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsChangeTest {

    @Test
    fun `turning blocking off loosens every mode`() {
        BlockOption.entries.filter { it != BlockOption.NothingSelected }.forEach { option ->
            assertTrue(SettingsChange.SelectOption(BlockOption.NothingSelected).isLoosening(limits(option)))
        }
    }

    @Test
    fun `moving to block all is never loosening`() {
        BlockOption.entries.forEach { option ->
            assertFalse(SettingsChange.SelectOption(BlockOption.BlockAll).isLoosening(limits(option)))
        }
    }

    @Test
    fun `leaving block all for a limit is loosening`() {
        assertTrue(SettingsChange.SelectOption(BlockOption.DailyLimit).isLoosening(limits(BlockOption.BlockAll)))
        assertTrue(SettingsChange.DailyLimit(MINUTE).isLoosening(limits(BlockOption.BlockAll)))
        assertTrue(SettingsChange.IntervalTimer(MINUTE, HOUR).isLoosening(limits(BlockOption.BlockAll)))
    }

    @Test
    fun `switching between daily limit and interval timer is loosening`() {
        assertTrue(SettingsChange.SelectOption(BlockOption.IntervalTimer).isLoosening(limits(BlockOption.DailyLimit)))
        assertTrue(SettingsChange.DailyLimit(MINUTE).isLoosening(limits(BlockOption.IntervalTimer)))
    }

    @Test
    fun `turning blocking on from nothing is not loosening`() {
        val current = limits(BlockOption.NothingSelected)
        assertFalse(SettingsChange.SelectOption(BlockOption.DailyLimit).isLoosening(current))
        assertFalse(SettingsChange.DailyLimit(HOUR).isLoosening(current))
        assertFalse(SettingsChange.IntervalTimer(HOUR, MINUTE).isLoosening(current))
    }

    @Test
    fun `selecting the active mode again is not loosening`() {
        assertFalse(SettingsChange.SelectOption(BlockOption.DailyLimit).isLoosening(limits(BlockOption.DailyLimit)))
    }

    @Test
    fun `only a higher daily limit is loosening`() {
        val current = limits(BlockOption.DailyLimit, BlockingSettings(dailyLimitMillis = 10 * MINUTE))
        assertTrue(SettingsChange.DailyLimit(11 * MINUTE).isLoosening(current))
        assertFalse(SettingsChange.DailyLimit(10 * MINUTE).isLoosening(current))
        assertFalse(SettingsChange.DailyLimit(5 * MINUTE).isLoosening(current))
    }

    @Test
    fun `more watch time or a shorter break is loosening`() {
        val current = limits(
            BlockOption.IntervalTimer,
            BlockingSettings(intervalAllowanceMillis = 5 * MINUTE, intervalLengthMillis = HOUR),
        )
        assertTrue(SettingsChange.IntervalTimer(6 * MINUTE, HOUR).isLoosening(current))
        assertTrue(SettingsChange.IntervalTimer(5 * MINUTE, HOUR - MINUTE).isLoosening(current))
        assertFalse(SettingsChange.IntervalTimer(4 * MINUTE, 2 * HOUR).isLoosening(current))
    }

    @Test
    fun `other settings loosen only in their weaker direction`() {
        val current = limits(BlockOption.DailyLimit).copy(pauseDurationMillis = 5 * MINUTE, includeStories = true)
        assertTrue(SettingsChange.PauseDuration(6 * MINUTE).isLoosening(current))
        assertFalse(SettingsChange.PauseDuration(4 * MINUTE).isLoosening(current))
        assertTrue(SettingsChange.IncludeStories(false).isLoosening(current))
        assertFalse(SettingsChange.IncludeStories(true).isLoosening(current))
        assertTrue(SettingsChange.AllowVideosSentByDm(true).isLoosening(current))
        assertFalse(SettingsChange.AllowVideosSentByDm(false).isLoosening(current))
        assertTrue(SettingsChange.DisableCommitment.isLoosening(current))
    }

    private fun limits(option: BlockOption, settings: BlockingSettings = BlockingSettings()) = CurrentLimits(
        activeOption = option,
        settings = settings,
        pauseDurationMillis = 5 * MINUTE,
        includeStories = false,
        allowVideosSentByDm = false,
    )

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
