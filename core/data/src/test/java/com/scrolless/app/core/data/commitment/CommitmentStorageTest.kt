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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitmentStorageTest {

    @Test
    fun `every change survives a round trip`() {
        val changes = listOf(
            SettingsChange.SelectOption(BlockOption.NothingSelected),
            SettingsChange.DailyLimit(600_000L),
            SettingsChange.IntervalTimer(allowanceMillis = 300_000L, intervalLengthMillis = 3_600_000L),
            SettingsChange.PauseDuration(900_000L),
            SettingsChange.IncludeStories(false),
            SettingsChange.AllowVideosSentByDm(true),
            SettingsChange.DisableCommitment,
        )

        changes.forEach { change ->
            assertEquals(change, SettingsChangeCodec.decode(SettingsChangeCodec.encode(change)))
        }
    }

    @Test
    fun `unreadable values decode to null`() {
        listOf(null, "", "option:Nope", "daily:abc", "interval:1", "dm:yes", "commitment_off:1", "unknown:1").forEach {
            assertNull(it, SettingsChangeCodec.decode(it))
        }
    }

    @Test
    fun `the right pin matches and a wrong one does not`() {
        val stored = PinHasher.hash("1234")

        assertTrue(PinHasher.matches("1234", stored))
        assertFalse(PinHasher.matches("1235", stored))
        assertFalse(PinHasher.matches("1234", null))
        assertFalse(PinHasher.matches("1234", "garbage"))
    }

    @Test
    fun `the same pin hashes differently each time`() {
        assertNotEquals(PinHasher.hash("1234"), PinHasher.hash("1234"))
    }
}
