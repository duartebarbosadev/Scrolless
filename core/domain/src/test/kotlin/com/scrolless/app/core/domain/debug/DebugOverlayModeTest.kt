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
package com.scrolless.app.core.domain.debug

import com.scrolless.app.core.debug.DebugOverlayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugOverlayModeTest {
    @Test
    fun `auto follows the real API level`() {
        for (sdk in 23..37) {
            assertEquals(sdk >= 34, DebugOverlayMode.AUTO.usesWindowAttachment(sdk, isDebug = true))
        }
    }

    @Test
    fun `legacy can be tested on a modern phone`() {
        for (sdk in 23..37) {
            assertFalse(DebugOverlayMode.LEGACY.usesWindowAttachment(sdk, isDebug = true))
        }
    }

    @Test
    fun `attached mode still requires a supported API`() {
        for (sdk in 23..33) {
            assertFalse(DebugOverlayMode.WINDOW_ATTACHED.usesWindowAttachment(sdk, isDebug = true))
        }
        assertTrue(DebugOverlayMode.WINDOW_ATTACHED.usesWindowAttachment(34, isDebug = true))
    }

    @Test
    fun `release ignores every override`() {
        DebugOverlayMode.entries.forEach { mode ->
            for (sdk in 23..37) {
                assertEquals(sdk >= 34, mode.usesWindowAttachment(sdk, isDebug = false))
            }
        }
    }
}
