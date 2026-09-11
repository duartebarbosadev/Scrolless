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

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedContentScanQueueTest {
    private val main = QueuedDispatcher()
    private val worker = QueuedDispatcher()
    private val job = Job()
    private val results = mutableListOf<ContentScanner.Result>()
    private val queue = FeedContentScanQueue(CoroutineScope(job + main), {
        assertTrue(main.executing)
        results += it
    }, worker)
    private val result = ContentScanner.Result(null, false, true, null)

    @Test
    fun `scans run on worker and bursts keep only the latest pending scan`() {
        val executed = mutableListOf<Int>()
        fun submit(id: Int) = queue.submit {
            assertTrue(worker.executing)
            executed += id
            result
        }
        submit(1)
        main.runNext()
        submit(2)
        submit(3)
        assertTrue(executed.isEmpty())
        assertEquals(1, worker.size)
        worker.runNext()
        assertTrue(results.isEmpty())
        main.runNext()
        assertEquals(listOf(result), results)
        assertEquals(1, worker.size)
        worker.runNext()
        main.runNext()
        assertEquals(listOf(1, 3), executed)
        assertEquals(listOf(result, result), results)
        assertEquals(0, worker.size)
    }

    @Test
    fun `navigation rejects in-flight results and clears pending scans`() {
        queue.submit { result }
        main.runNext()
        queue.submit { error("Obsolete pending scan must not execute") }
        queue.invalidate()
        worker.runNext()
        main.runNext()
        assertTrue(results.isEmpty())
        assertEquals(0, worker.size)
        queue.submit { result }
        main.runNext()
        worker.runNext()
        main.runNext()
        assertEquals(listOf(result), results)
    }

    @Test
    fun `new request waits for an invalidated scan without overlapping workers`() {
        queue.submit { result.copy(rootAvailable = false) }
        main.runNext()
        queue.invalidate()
        queue.submit { result }
        assertEquals(1, worker.size)
        worker.runNext()
        main.runNext()
        assertTrue(results.isEmpty())
        worker.runNext()
        main.runNext()
        assertEquals(listOf(result), results)
    }

    @Test
    fun `service destruction prevents result delivery`() {
        queue.submit { result }
        main.runNext()
        worker.runNext()
        job.cancel()
        main.runNext()
        assertTrue(results.isEmpty())
    }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        var executing = false
            private set
        val size get() = tasks.size

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            tasks.addLast(block)
        }

        fun runNext() {
            executing = true
            try {
                tasks.removeFirst().run()
            } finally {
                executing = false
            }
        }
    }
}
