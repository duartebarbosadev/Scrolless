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
import android.os.SystemClock

/**
 * Limits repeated scans while allowing urgent screen changes to be checked immediately.
 * Keeps only the latest pending scan at a fixed deadline, so continuous updates cannot postpone it.
 * Call all methods on [handler]'s thread; this schedules work there rather than moving it off-thread.
 */
internal class ContentScanScheduler(private val handler: Handler, private val intervalMillis: Long = 250L) {
    private var nextScanAt = 0L
    private var pending: (() -> Unit)? = null
    private val runPending = Runnable {
        val scan = pending ?: return@Runnable
        pending = null
        nextScanAt = SystemClock.uptimeMillis() + intervalMillis
        scan()
    }

    /**
     * Runs [scan] now, or replaces the pending scan until the current interval ends.
     * Set [coalesce] to false for window or app changes that must cancel stale work and run immediately.
     * The callback should read the current screen and must not retain a recycled accessibility event.
     */
    fun submit(coalesce: Boolean, scan: () -> Unit) {
        cancel()
        pending = scan
        if (coalesce && SystemClock.uptimeMillis() < nextScanAt) {
            handler.postAtTime(runPending, nextScanAt)
        } else {
            runPending.run()
        }
    }

    /**
     * Discards queued work when tracking ends or the service stops, so an old screen is not scanned later.
     * Leaves the current interval deadline intact; an urgent submission can still run immediately.
     */
    fun cancel() {
        handler.removeCallbacks(runPending)
        pending = null
    }
}
