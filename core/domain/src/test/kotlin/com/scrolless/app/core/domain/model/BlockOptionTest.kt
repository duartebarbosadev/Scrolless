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
import com.scrolless.app.core.model.IntervalTimerWindow
import com.scrolless.app.core.model.withConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockOptionTest {

    @Test
    fun `expanding active interval preserves its original start and ten seconds usage`() {
        val originalWindow = IntervalTimerWindow(
            startMillis = 1_000L,
            lengthMillis = 30 * MINUTE_MILLIS,
            usageMillis = 10_000L,
        )
        val current = BlockOption.IntervalTimer(
            allowanceMillis = MINUTE_MILLIS,
            window = originalWindow,
        )

        val updated = current.withConfig(
            allowanceMillis = 2 * MINUTE_MILLIS,
            intervalLengthMillis = 60 * MINUTE_MILLIS,
        )

        assertEquals(2 * MINUTE_MILLIS, updated.allowanceMillis)
        assertEquals(originalWindow.startMillis, updated.window.startMillis)
        assertEquals(originalWindow.usageMillis, updated.window.usageMillis)
        assertEquals(60 * MINUTE_MILLIS, updated.window.lengthMillis)
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
