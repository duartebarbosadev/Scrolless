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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.w3c.dom.Element

/** Fixtures were captured from Instagram 453.0.0.44.79 on a Galaxy S22 (1080x2340). */
class InstagramFeedDetectorTest {
    private val detector = InstagramFeedDetector

    @After
    fun tearDown() {
        detector.isEnabled = false
    }

    @Test
    fun `captured home feed covers posts below the Stories tray`() {
        assertEquals(ContentBounds(0, 638, 1080, 2151), detector.coverBounds(fixture("home")))
    }

    @Test
    fun `scrolled feed keeps the sticky Stories tray above the cover`() {
        assertEquals(ContentBounds(0, 318, 1080, 2151), detector.coverBounds(fixture("home_scrolled")))
    }

    @Test
    fun `profiles opened from Home and open Stories are not covered`() {
        assertNull(detector.coverBounds(fixture("profile_from_home")))
        assertNull(detector.coverBounds(fixture("home") + fixture("story")))
    }

    @Test
    fun `Messages reuses the feed list ID but is not covered`() {
        val hiddenHomeTopBar = ContentCoverNode("action_bar_LinearLayout", ContentBounds(0, 81, 1080, 249), isVisible = false)
        assertNull(detector.coverBounds(fixture("messages")))
        assertNull(detector.coverBounds(fixture("messages") + hiddenHomeTopBar))
    }

    @Test
    fun `feed rows without the home top bar are not covered`() {
        assertNull(detector.coverBounds(fixture("home").filterNot { it.viewId == "action_bar_LinearLayout" }))
    }

    @Test
    fun `hidden feed is not covered even while a cover is active`() {
        val nodes = fixture("home").map { if (it.viewId == "refreshable_container") it.copy(isVisible = false) else it }
        assertNull(detector.coverBounds(nodes, activeCoverBounds = ContentBounds(0, 638, 1080, 2151)))
    }

    @Test
    fun `Instagram keeps Back-only blocking until the setting is on`() {
        assertNull(BlockableApp.REELS.coverDetector)
        detector.isEnabled = true
        assertSame(detector, BlockableApp.REELS.coverDetector)
    }

    private fun fixture(name: String): List<ContentCoverNode> {
        val stream = requireNotNull(javaClass.getResourceAsStream("/instagram/$name.xml"))
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
