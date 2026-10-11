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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Keeps feed scans off the UI thread without accumulating a backlog of scroll events. */
internal class FeedContentScanQueue(
    private val scope: CoroutineScope,
    private val onResult: (ContentScanner.Result) -> Unit,
    private val worker: CoroutineDispatcher = Dispatchers.Default,
) {
    private var pending: (() -> ContentScanner.Result)? = null
    private var running = false
    private var revision = 0L

    /** Called on the main thread with a scan whose inputs have already been captured. */
    fun submit(scan: () -> ContentScanner.Result) {
        pending = scan
        if (running) return
        running = true
        scope.launch {
            try {
                while (true) {
                    val next = pending ?: break
                    pending = null
                    val startedRevision = revision
                    val result = withContext(worker) { next() }
                    if (startedRevision == revision) onResult(result)
                }
            } finally {
                running = false
            }
        }
    }

    /** Navigation or preference changes make both queued and in-flight results obsolete. */
    fun invalidate() {
        revision++
        pending = null
    }
}
