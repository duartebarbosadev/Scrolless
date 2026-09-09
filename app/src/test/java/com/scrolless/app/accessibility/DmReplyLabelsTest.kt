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

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.DmExemptionRule
import com.scrolless.app.core.model.ReplyLabels
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.w3c.dom.Element

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class DmReplyLabelsTest {
    private val labels = requireNotNull(BlockableApp.TIKTOK.getDmExemptionRule()?.replyLabelsBelowPlayer)

    @Test
    fun `DM reply matches but feed sharing shortcut and comments do not`() {
        assertTrue(matchesFixture("dm"))
        for (name in listOf("feed", "comments")) {
            assertFalse(name, matchesFixture(name))
            // Even matching words in a caption, share shortcut or editor must not exempt the video.
            assertFalse(name, matchesFixture(name, replaceLabels = true))
        }
    }

    @Test
    fun `DM reply geometry is independent of pixel size offset and RTL`() {
        for (scale in listOf(0.5f, 1f, 2f)) {
            for (mirrored in listOf(false, true)) {
                assertTrue(matchesFixture("dm", scale = scale, mirrored = mirrored))
            }
        }
    }

    @Test
    fun `production traversal accepts text description and hint on nested buttons`() {
        for (source in listOf("text", "description", "hint")) {
            val root = AccessibilityNodeInfo.obtain()
            val container = AccessibilityNodeInfo.obtain()
            val reply = replyButton()
            when (source) {
                "text" -> reply.text = "Message [recipient]..."
                "description" -> reply.contentDescription = "Message [recipient]..."
                "hint" -> AccessibilityNodeInfoCompat.wrap(reply).hintText = "Message [recipient]..."
            }
            shadowOf(root).addChild(container)
            shadowOf(container).addChild(reply)
            assertTrue(source, root.hasReplyBelowPlayer(labels, ContentBounds(0, 0, 1000, 1800), ::screenBounds))
        }
    }

    @Test
    fun `production matcher rejects hidden disabled nonclickable editable and nonbutton controls`() {
        val mutations: List<(AccessibilityNodeInfo) -> Unit> = listOf(
            { it.isVisibleToUser = false },
            { it.isEnabled = false },
            { it.isClickable = false },
            { it.isEditable = true },
            { it.className = "android.widget.TextView" },
        )
        for (mutate in mutations) {
            val reply = replyButton().apply { text = "Message [recipient]..." }
            mutate(reply)
            assertFalse(reply.hasReplyBelowPlayer(labels, ContentBounds(0, 0, 1000, 1800), ::screenBounds))
        }
    }

    private fun replyButton() = AccessibilityNodeInfo.obtain().apply {
        className = "android.widget.Button"
        isVisibleToUser = true
        isEnabled = true
        isClickable = true
        setBoundsInScreen(Rect(100, 1810, 600, 1910))
    }

    @Test
    fun `other apps supply independent reply templates through the same rule`() {
        val rule = DmExemptionRule(replyLabelsBelowPlayer = ReplyLabels(setOf("Reply to {recipient}")))
        val otherLabels = requireNotNull(rule.replyLabelsBelowPlayer)
        assertTrue(otherLabels.matches("Reply to [recipient]"))
        assertFalse(otherLabels.matches("Message [recipient]..."))
        assertFalse(labels.matches("Reply to [recipient]"))
    }

    @Test
    fun `invalid template configurations are rejected`() {
        for (templates in listOf(emptySet(), setOf("Reply"), setOf("{recipient}"), setOf("{recipient} to {recipient}"))) {
            assertTrue(runCatching { ReplyLabels(templates) }.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun `packaged translations handle recipient at either end and ellipsis variations`() {
        for (label in listOf(
            "Message [recipient]...",
            "Enviar mensagem para [recipient]...",
            "Enviar mensaje a [recipient]…",
            "Nachricht an [recipient]\u00a0...",
            "مراسلة [recipient]...",
        )) {
            assertTrue(label, labels.matches(label))
        }
        val recipientFirst = ReplyLabels(setOf("{recipient}에게 메시지 보내기..."))
        assertTrue(recipientFirst.matches("[recipient]에게 메시지 보내기..."))
    }

    @Test
    fun `labels require the full template and a nonblank recipient`() {
        for (label in listOf(
            "Message ...", "Message    ...", "Message [recipient]", "Share with [recipient]",
            "Caption: Message [recipient]...", "Message [recipient]... extra", "Message \n[recipient]...", "",
        )) {
            assertFalse(label, labels.matches(label))
        }
        assertFalse(labels.matches(null))
    }

    @Test
    fun `reply must be below and within the player with a small relative gap`() {
        val player = ContentBounds(0, 0, 1000, 1800)
        assertTrue(ContentBounds(100, 1810, 600, 1910).isDirectlyBelow(player))
        assertFalse(ContentBounds(100, 1700, 600, 1800).isDirectlyBelow(player))
        assertFalse(ContentBounds(100, 2200, 600, 2300).isDirectlyBelow(player))
        assertFalse(ContentBounds(-100, 1810, 600, 1910).isDirectlyBelow(player))
        assertFalse(ContentBounds(100, 1810, 1100, 1910).isDirectlyBelow(player))
        assertFalse(ContentBounds(100, 1810, 100, 1910).isDirectlyBelow(player))
    }

    @Test
    fun `fixtures contain no personal labels or unexpected attributes`() {
        val allowed = setOf("class", "bounds", "clickable", "enabled", "resource-id", "text", "hint")
        for (name in listOf("dm", "feed", "comments")) {
            for (node in fixture(name)) {
                for (i in 0 until node.attributes.length) {
                    val a = node.attributes.item(i)
                    assertTrue(a.nodeName in allowed)
                    val safe = when (a.nodeName) {
                        "text", "hint" -> a.nodeValue in setOf("redacted", "Message [recipient]...")
                        "resource-id" -> a.nodeValue == "player_view"
                        "clickable", "enabled" -> a.nodeValue in setOf("true", "false")
                        "class" -> a.nodeValue in setOf("android.widget.Button", "android.widget.EditText", "android.widget.FrameLayout")
                        "bounds" -> Regex("\\[-?\\d+,-?\\d+\\]\\[-?\\d+,-?\\d+\\]").matches(a.nodeValue)
                        else -> false
                    }
                    assertTrue("Unexpected fixture content in $name", safe)
                }
                assertTrue(node.textContent.isBlank())
            }
        }
    }

    private fun matchesFixture(name: String, replaceLabels: Boolean = false, scale: Float = 1f, mirrored: Boolean = false): Boolean {
        val nodes = fixture(name)
        fun bounds(node: Element): ContentBounds {
            val b = Regex("-?\\d+").findAll(node.getAttribute("bounds")).map { it.value.toInt() }.toList()
            return ContentBounds(
                ((if (mirrored) 1080 - b[2] else b[0]) * scale).toInt() + 80,
                (b[1] * scale).toInt() + 40,
                ((if (mirrored) 1080 - b[0] else b[2]) * scale).toInt() + 80,
                (b[3] * scale).toInt() + 40,
            )
        }
        val player = nodes.firstOrNull { it.getAttribute("resource-id") == "player_view" } ?: return false
        val root = AccessibilityNodeInfo.obtain()
        for (element in nodes) {
            val node = AccessibilityNodeInfo.obtain().apply {
                className = element.getAttribute("class")
                isVisibleToUser = true
                isEnabled = element.getAttribute("enabled") == "true"
                isClickable = element.getAttribute("clickable") == "true"
                isEditable = className == "android.widget.EditText"
                text = if (replaceLabels && element.hasAttribute("text")) "Message [recipient]..." else element.getAttribute("text")
                AccessibilityNodeInfoCompat.wrap(this).hintText = element.getAttribute("hint")
                val b = bounds(element)
                setBoundsInScreen(Rect(b.left, b.top, b.right, b.bottom))
            }
            shadowOf(root).addChild(node)
        }
        return root.hasReplyBelowPlayer(labels, bounds(player), ::screenBounds)
    }

    private fun screenBounds(node: AccessibilityNodeInfo): ContentBounds {
        val b = Rect()
        node.getBoundsInScreen(b)
        return ContentBounds(b.left, b.top, b.right, b.bottom)
    }

    private fun fixture(name: String): List<Element> {
        val stream = requireNotNull(javaClass.getResourceAsStream("/tiktok/reply_labels/$name.xml"))
        val document = stream.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val elements = document.getElementsByTagName("node")
        return (0 until elements.length).map { elements.item(it) as Element }
    }
}
