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

import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.DetectionMethod
import com.scrolless.app.core.model.ResolvedBlockableApp
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class NodeStructureTest {
    @Test
    fun `a part needs a class name or view ID`() {
        assertThrows(IllegalArgumentException::class.java) { DetectionMethod.NodeStructure() }
    }

    @Test
    fun `only the innermost part can name view IDs`() {
        assertThrows(IllegalArgumentException::class.java) {
            DetectionMethod.NodeStructure(viewIds = setOf("list"), descendant = DetectionMethod.NodeStructure(viewIds = setOf("player")))
        }
    }

    // The scanner only finds views by ID, and only checks the container top, when it starts from the rule's
    // innermost view. It tries each alternative of an AnyOf on its own, so each one must start that way.
    // A rule using either feature any other way would silently never match on a device.
    @Test
    fun `every rule that uses view IDs or a container top starts from its innermost view`() {
        for (app in BlockableApp.entries) {
            val resolved = ResolvedBlockableApp(app, app.getPackageIds().first())
            for (includeStories in listOf(false, true)) {
                for (method in app.getDetectionMethod(includeStories).alternatives()) {
                    if (method.nodeStructures().any { it.viewIds.isNotEmpty() || it.requireAtTopOf != null }) {
                        assertNotNull("$app (stories: $includeStories)", resolved.getAnchorViewIds(method))
                    }
                }
            }
        }
    }

    private fun DetectionMethod.alternatives(): List<DetectionMethod> = (this as? DetectionMethod.AnyOf)?.detectionMethods ?: listOf(this)

    private fun DetectionMethod.nodeStructures(): Sequence<DetectionMethod.NodeStructure> = when (this) {
        is DetectionMethod.NodeStructure -> generateSequence(this) { it.descendant }
        is DetectionMethod.AnyOf -> detectionMethods.asSequence().flatMap { it.nodeStructures() }
        else -> emptySequence()
    }
}
