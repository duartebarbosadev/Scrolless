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

import com.scrolless.app.core.model.ContentBlockAction
import com.scrolless.app.core.model.ResolvedBlockableApp

/**
 * Holds the blocked screen we found.
 * It exists because the same app can use a cover on one screen and navigation on another.
 */
internal data class DetectedBlockedContent(
    val app: ResolvedBlockableApp,
    val blockingSuppressed: Boolean,
    val cover: ContentCover? = null,
) {
    /** The action required for this screen: cover its video when possible, otherwise use the app's normal action. */
    val blockAction: ContentBlockAction
        get() = if (cover != null) ContentBlockAction.CoverVideoRegion else app.getBlockAction()

    /** Layout changes keep the session; changing the app or blocking action starts a new one. */
    fun canContinueWith(next: DetectedBlockedContent): Boolean = app == next.app && blockAction == next.blockAction
}

/**
 * Tracks one visit to blocked content.
 * Once covered, its viewing time is finished and cannot be returned a second time.
 */
internal class ContentSession(var content: DetectedBlockedContent, val startedAtMillis: Long) {
    val app: ResolvedBlockableApp get() = content.app

    /** Whether blocking is temporarily skipped for this screen while viewing time is still counted. */
    val blockingSuppressed: Boolean get() = content.blockingSuppressed

    /** Whether a cover has stopped this session's viewing time but screen detection must continue. */
    var isCovered: Boolean = false
        private set

    /** Prevents more than one usage record from being produced for this visit. */
    private var viewingFinished: Boolean = false

    /**
     * Marks the video as covered and returns the viewing period that just ended.
     * Later calls return `null` so the same viewing time cannot be saved twice.
     */
    fun cover(nowMillis: Long): FinishedViewing? {
        if (viewingFinished) return null
        isCovered = true
        return finishOnce(nowMillis)
    }

    /**
     * Ends this visit and returns its viewing period.
     * Later calls return `null` because a visit must be saved only once.
     */
    fun finish(nowMillis: Long): FinishedViewing? {
        if (viewingFinished) return null
        return finishOnce(nowMillis)
    }

    private fun finishOnce(nowMillis: Long): FinishedViewing {
        viewingFinished = true
        return FinishedViewing(app, startedAtMillis, maxOf(startedAtMillis, nowMillis))
    }
}

/**
 * Holds a completed viewing period for the usage tracker.
 */
internal data class FinishedViewing(val app: ResolvedBlockableApp, val startedAtMillis: Long, val endedAtMillis: Long) {
    val durationMillis: Long get() = endedAtMillis - startedAtMillis
}
