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

import com.scrolless.app.core.model.BlockableApp
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.w3c.dom.Element

class TikTokScreenDetectorTest {
    private val detector = TikTokScreenDetector(BlockableApp.TIKTOK)

    @Test
    fun `captured feed covers the player but not native navigation`() {
        assertEquals(ContentBounds(0, 0, 1080, 2160), detector.coverBounds(fixture("home")))
    }

    @Test
    fun `captured DM video covers the player correctly`() {
        assertEquals(ContentBounds(0, 97, 1080, 2160), detector.coverBounds(fixture("dm_video")))
    }

    @Test
    fun `captured Inbox contains no blockable video`() {
        assertNull(detector.coverBounds(fixture("inbox")))
    }

    @Test
    fun `invisible players are not covered without active cover`() {
        val nodes = fixture("home").map {
            if (it.viewId == "player_view") it.copy(isVisible = false) else it
        }
        assertNull(detector.coverBounds(nodes))
    }

    @Test
    fun `invisible player occluded by active cover remains covered`() {
        val player = fixture("home").first { it.viewId == "player_view" }
        val nodes = fixture("home").map {
            if (it.viewId == "player_view") it.copy(isVisible = false) else it
        }
        assertEquals(player.bounds, detector.coverBounds(nodes, activeCoverBounds = player.bounds))
    }

    @Test
    fun `offscreen empty players do not match`() {
        val nodes = listOf(ContentCoverNode("player_view", ContentBounds(0, 2160, 1080, 2160), true))
        assertNull(detector.coverBounds(nodes))
    }

    @Test
    fun `visible videos opened from a native tab still get detected`() {
        val player = fixture("home").first { it.viewId == "player_view" }
        assertEquals(player.bounds, detector.coverBounds(fixture("inbox") + player))
    }

    /** Verifies Lite uses its own player bounds and keeps the cover when it obscures that player. */
    @Test
    fun `Lite covers its player without matching the regular TikTok ID`() {
        val lite = TikTokScreenDetector(BlockableApp.TIKTOK_LITE)
        val bounds = ContentBounds(0, 97, 1080, 2160)
        val player = ContentCoverNode("simplayer_api_player_view", bounds, true)
        assertEquals(bounds, lite.coverBounds(listOf(player)))
        assertEquals(bounds, lite.coverBounds(listOf(player.copy(isVisible = false)), bounds))
        assertNull(lite.coverBounds(listOf(player.copy(isVisible = false))))
        assertNull(lite.coverBounds(listOf(player.copy(viewId = "player_view"))))
        assertNull(lite.coverBounds(emptyList(), bounds))
    }

    private fun fixture(name: String): List<ContentCoverNode> {
        val stream = requireNotNull(javaClass.getResourceAsStream("/tiktok/$name.xml"))
        val document = stream.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val nodes = document.getElementsByTagName("node")
        return (0 until nodes.length).map { index ->
            val node = nodes.item(index) as Element
            val bounds = Regex("-?\\d+").findAll(node.getAttribute("bounds")).map { it.value.toInt() }.toList()
            ContentCoverNode(
                node.getAttribute("resource-id"),
                ContentBounds(bounds[0], bounds[1], bounds[2], bounds[3]),
                node.getAttribute("visible").toBoolean(),
            )
        }
    }
}
