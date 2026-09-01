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

/** The blocked screen we found and the action that applies to this particular screen. */
internal data class DetectedBlockedContent(
    val app: ResolvedBlockableApp,
    val blockingSuppressed: Boolean,
    val cover: ContentCover? = null,
) {
    val blockAction: ContentBlockAction
        get() = if (cover != null) ContentBlockAction.CoverVideoRegion else app.getBlockAction()

    /** Layout changes keep the session; changing the app or blocking action starts a new one. */
    fun canContinueWith(next: DetectedBlockedContent): Boolean = app == next.app && blockAction == next.blockAction
}

/**
 * Tracks one visit to blocked content.
 * Once covered, its viewing time is finished and cannot be returned a second time.
 */
internal class ContentSession(
    var content: DetectedBlockedContent,
    val startedAtMillis: Long,
) {
    val app: ResolvedBlockableApp get() = content.app
    val blockingSuppressed: Boolean get() = content.blockingSuppressed
    var isCovered: Boolean = false
        private set
    private var viewingFinished: Boolean = false

    fun cover(nowMillis: Long): FinishedViewing? {
        if (viewingFinished) return null
        isCovered = true
        return finishOnce(nowMillis)
    }

    fun finish(nowMillis: Long): FinishedViewing? {
        if (viewingFinished) return null
        return finishOnce(nowMillis)
    }

    private fun finishOnce(nowMillis: Long): FinishedViewing {
        viewingFinished = true
        return FinishedViewing(app, startedAtMillis, maxOf(startedAtMillis, nowMillis))
    }
}

/** Final viewing times passed to the usage tracker after the live session has ended. */
internal data class FinishedViewing(
    val app: ResolvedBlockableApp,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
) {
    val durationMillis: Long get() = endedAtMillis - startedAtMillis
}
