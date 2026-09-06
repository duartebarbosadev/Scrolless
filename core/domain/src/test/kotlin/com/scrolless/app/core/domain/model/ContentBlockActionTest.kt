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
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.ContentBlockAction
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentBlockActionTest {
    @Test
    fun `regular TikTok covers its video without leaving the app`() {
        assertEquals(ContentBlockAction.CoverVideoRegion, BlockableApp.TIKTOK.getBlockAction())
    }

    @Test
    fun `other apps retain their existing navigation action`() {
        for (app in BlockableApp.entries.filter { it != BlockableApp.TIKTOK }) {
            val action = if (app == BlockableApp.TIKTOK_LITE) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK
            assertEquals(ContentBlockAction.PerformGlobalAction(action), app.getBlockAction())
        }
    }
}
