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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundAppWindowTest {
    private val tiktok = InteractiveWindowState("com.zhiliaoapp.musically", true, true, true)
    private val launcher = InteractiveWindowState("launcher", true, true, true)

    @Test
    fun `home takes over even while TikTok retains an inactive window`() {
        val windows = listOf(tiktok.copy(isActive = false, isFocused = false), launcher)
        assertEquals("launcher", foregroundAppPackage(windows))
    }

    @Test
    fun `focused app takes over when no application window is active`() {
        val windows = listOf(
            tiktok.copy(isActive = false, isFocused = false),
            launcher.copy(packageName = "another.app", isActive = false),
        )
        assertEquals("another.app", foregroundAppPackage(windows))
    }

    @Test
    fun `new input focus wins while the old touched window is still active`() {
        val windows = listOf(tiktok.copy(isFocused = false), launcher.copy(isActive = false))
        assertEquals("launcher", foregroundAppPackage(windows))
    }

    @Test
    fun `active application is used when no application has input focus`() {
        assertEquals(tiktok.packageName, foregroundAppPackage(listOf(tiktok.copy(isFocused = false))))
    }

    @Test
    fun `returning to TikTok restores its foreground package without an event package`() {
        val windows = listOf(launcher.copy(isActive = false, isFocused = false), tiktok)
        assertEquals(tiktok.packageName, foregroundAppPackage(windows))
    }

    @Test
    fun `touching accessibility cover does not hide the focused application`() {
        val cover = InteractiveWindowState("com.scrolless.app", false, true, false)
        assertEquals(tiktok.packageName, foregroundAppPackage(listOf(cover, tiktok.copy(isActive = false))))
    }

    @Test
    fun `keyboard activity preserves the focused application`() {
        val keyboard = InteractiveWindowState("keyboard", false, true, false)
        assertEquals(tiktok.packageName, foregroundAppPackage(listOf(keyboard, tiktok.copy(isActive = false))))
    }

    @Test
    fun `retained windows without focus cannot keep a cover visible`() {
        assertNull(foregroundAppPackage(listOf(tiktok.copy(isActive = false, isFocused = false))))
        assertNull(foregroundAppPackage(emptyList()))
    }

    @Test
    fun `unavailable focused root does not fall back to an old TikTok window`() {
        val windows = listOf(tiktok.copy(isFocused = false), launcher.copy(packageName = null, isActive = false))
        assertNull(foregroundAppPackage(windows))
    }
}
