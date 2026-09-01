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

import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.w3c.dom.Element

class TikTokScreenDetectorTest {
    @Test
    fun `captured feed covers the player but not native navigation`() {
        assertEquals(ContentBounds(0, 0, 1080, 2160), TikTokScreenDetector.coverBounds(fixture("home")))
    }

    @Test
    fun `captured Inbox contains no blockable video`() {
        assertNull(TikTokScreenDetector.coverBounds(fixture("inbox")))
    }

    @Test
    fun `retained hidden players do not cover an allowed tab`() {
        val nodes = fixture("home").map {
            if (it.viewId == TikTokScreenDetector.PLAYER) it.copy(isVisible = false) else it
        }
        assertNull(TikTokScreenDetector.coverBounds(nodes))
    }

    @Test
    fun `offscreen empty players do not match`() {
        val nodes = listOf(ContentCoverNode("player_view", ContentBounds(0, 2160, 1080, 2160), true))
        assertNull(TikTokScreenDetector.coverBounds(nodes))
    }

    @Test
    fun `attached cover hiding the player does not toggle detection`() {
        val home = fixture("home")
        val bounds = TikTokScreenDetector.coverBounds(home)!!
        val cover = ContentCoverTarget.Window(12, 0, bounds)
        val occluded = home.map { if (it.viewId == TikTokScreenDetector.PLAYER) it.copy(isVisible = false) else it }
        // Cover appears, the player becomes occluded, and subsequent events keep the same target.
        repeat(5) {
            assertEquals(bounds, TikTokScreenDetector.coverBounds(occluded, windowId = 12, attachedCover = cover))
        }
        // Removing the cover removes the exception, too.
        assertNull(TikTokScreenDetector.coverBounds(occluded, windowId = 12))
    }

    @Test
    fun `an attached cover cannot justify a hidden player in another window`() {
        val home = fixture("home").map { it.copy(isVisible = false) }
        val cover = ContentCoverTarget.Window(12, 0, ContentBounds(0, 0, 1080, 2160))
        assertNull(TikTokScreenDetector.coverBounds(home, windowId = 13, attachedCover = cover))
    }

    @Test
    fun `Inbox selection clears the cover even if TikTok retains a hidden player`() {
        val player = fixture("home").first { it.viewId == TikTokScreenDetector.PLAYER }.copy(isVisible = false)
        val inbox = fixture("inbox") + player
        val cover = ContentCoverTarget.Window(12, 0, player.bounds)
        assertNull(TikTokScreenDetector.coverBounds(inbox, windowId = 12, attachedCover = cover))
    }

    @Test
    fun `Profile and Create selections also end a covered feed`() {
        val cover = ContentCoverTarget.Window(12, 0, ContentBounds(0, 0, 1080, 2160))
        for (tab in listOf(TikTokScreenDetector.PROFILE, TikTokScreenDetector.CREATE)) {
            val nodes = fixture("inbox").map {
                if (it.viewId == TikTokScreenDetector.INBOX) it.copy(viewId = tab) else it
            } + ContentCoverNode(TikTokScreenDetector.PLAYER, cover.bounds, false)
            assertNull(TikTokScreenDetector.coverBounds(nodes, windowId = 12, attachedCover = cover))
        }
    }

    @Test
    fun `a missing or differently positioned player cannot inherit an old cover`() {
        val cover = ContentCoverTarget.Window(12, 0, ContentBounds(0, 0, 1080, 2160))
        assertNull(TikTokScreenDetector.coverBounds(emptyList(), windowId = 12, attachedCover = cover))
        val moved = listOf(ContentCoverNode(TikTokScreenDetector.PLAYER, ContentBounds(50, 50, 500, 500), false))
        assertNull(TikTokScreenDetector.coverBounds(moved, windowId = 12, attachedCover = cover))
    }

    @Test
    fun `visible videos opened from a native tab still get detected`() {
        val player = fixture("home").first { it.viewId == TikTokScreenDetector.PLAYER }
        assertEquals(player.bounds, TikTokScreenDetector.coverBounds(fixture("inbox") + player))
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
                node.getAttribute("selected").toBoolean(),
            )
        }
    }
}
