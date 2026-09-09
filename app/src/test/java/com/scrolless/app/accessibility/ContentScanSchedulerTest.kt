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
package com.scrolless.app.accessibility

import android.os.Handler
import android.os.Looper
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class ContentScanSchedulerTest {
    private val scheduler = ContentScanScheduler(Handler(Looper.getMainLooper()))
    private val scans = mutableListOf<Int>()

    /** Keeps events arriving before the deadline to verify that updates neither starve the scan nor replay stale work. */
    @Test
    fun `content bursts run immediately then scan the latest update at the deadline`() {
        scheduler.submit(coalesce = true) { scans.add(0) }
        for (i in 1..4) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            scheduler.submit(coalesce = true) { scans.add(i) }
        }
        assertEquals(listOf(0), scans)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
        assertEquals(listOf(0, 4), scans)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(listOf(0, 4), scans)
    }

    /** Verifies that an urgent screen change runs without waiting and prevents the queued old scan from running later. */
    @Test
    fun `window changes run immediately and cancel a stale content update`() {
        scheduler.submit(coalesce = true) { scans.add(0) }
        scheduler.submit(coalesce = true) { scans.add(1) }
        scheduler.submit(coalesce = false) { scans.add(2) }
        assertEquals(listOf(0, 2), scans)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(listOf(0, 2), scans)
    }

    /** Checks cancellation and reuse so ending a session leaves no queued scan and does not disable future scans. */
    @Test
    fun `exit or shutdown cancels the pending scan`() {
        scheduler.submit(coalesce = true) { scans.add(0) }
        scheduler.submit(coalesce = true) { scans.add(1) }
        scheduler.cancel()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(listOf(0), scans)
        scheduler.submit(coalesce = true) { scans.add(2) }
        assertEquals(listOf(0, 2), scans)
    }
}
