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

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.ContentBlockAction
import com.scrolless.app.core.model.DetectionNode
import com.scrolless.app.core.model.ResolvedBlockableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentBlockActionTest {
    @Test
    fun `both TikTok variants cover their videos without leaving the app`() {
        for (app in listOf(BlockableApp.TIKTOK, BlockableApp.TIKTOK_LITE)) {
            assertEquals(ContentBlockAction.CoverVideoRegion, app.getBlockAction())
        }
    }

    @Test
    fun `other apps retain their existing navigation action`() {
        for (app in BlockableApp.entries.filter { it !in setOf(BlockableApp.TIKTOK, BlockableApp.TIKTOK_LITE) }) {
            assertEquals(ContentBlockAction.PerformGlobalAction(GLOBAL_ACTION_BACK), app.getBlockAction())
        }
    }

    /** Keeps Lite on its own rule and rejects hidden players left behind when opening a native tab. */
    @Test
    fun `TikTok Lite resolves to its own visible player rule`() {
        val packageId = "com.zhiliaoapp.musically.go"
        val matches = BlockableApp.entries.filter { it.resolvePackage(packageId) != null }
        assertEquals(listOf(BlockableApp.TIKTOK_LITE), matches)
        val app = ResolvedBlockableApp(matches.single(), packageId)
        val player = DetectionNode(nodeId = 1, viewId = "$packageId:id/simplayer_api_player_view")
        assertTrue(app.matchesFastDetectionNode(player))
        assertFalse(app.matchesFastDetectionNode(player.copy(isVisible = false)))
        assertFalse(app.matchesFastDetectionNode(player.copy(viewId = "$packageId:id/player_view")))
    }
}
