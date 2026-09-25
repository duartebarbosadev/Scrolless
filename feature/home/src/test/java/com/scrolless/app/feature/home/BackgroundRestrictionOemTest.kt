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
package com.scrolless.app.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundRestrictionOemTest {

    @Test
    fun `xiaomi family maps to xiaomi`() {
        assertEquals(BackgroundRestrictionOem.Xiaomi, BackgroundRestrictionOem.from("Xiaomi", "POCO"))
        assertEquals(BackgroundRestrictionOem.Xiaomi, BackgroundRestrictionOem.from("Xiaomi", "Redmi"))
        assertEquals(BackgroundRestrictionOem.Xiaomi, BackgroundRestrictionOem.from(null, "poco"))
    }

    @Test
    fun `other aggressive oems are detected`() {
        assertEquals(BackgroundRestrictionOem.Huawei, BackgroundRestrictionOem.from("HUAWEI", "HUAWEI"))
        assertEquals(BackgroundRestrictionOem.Honor, BackgroundRestrictionOem.from("HONOR", "HONOR"))
        assertEquals(BackgroundRestrictionOem.Oppo, BackgroundRestrictionOem.from("realme", "realme"))
        assertEquals(BackgroundRestrictionOem.OnePlus, BackgroundRestrictionOem.from("OnePlus", "OnePlus"))
        assertEquals(BackgroundRestrictionOem.Vivo, BackgroundRestrictionOem.from("vivo", "iQOO"))
        assertEquals(BackgroundRestrictionOem.Samsung, BackgroundRestrictionOem.from("samsung", "samsung"))
    }

    @Test
    fun `stock devices need no oem guidance`() {
        assertNull(BackgroundRestrictionOem.from("Google", "google"))
        assertNull(BackgroundRestrictionOem.from(null, null))
    }

    @Test
    fun `xiaomi tries autostart manager first`() {
        val first = BackgroundRestrictionOem.Xiaomi.settingsComponents.first()
        assertEquals("com.miui.securitycenter", first.packageName)
        assertTrue(first.className.endsWith("AutoStartManagementActivity"))
    }
}
