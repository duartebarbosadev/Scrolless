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

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.scrolless.app.core.model.ReplyLabels

/**
 * Searches this node and its descendants for a reply button directly below the video player.
 * Checks the button's text, content description and hint against [labels]. Requiring an enabled,
 * visible, clickable button outside the player helps reject captions and editable comment fields.
 * Stops at the first match so a confirmed DM does not require scanning the rest of the tree.
 *
 * [boundsOf] must use the same screen or window coordinates as [playerBounds]; mixing coordinate
 * systems would shift the button relative to the player and give an incorrect position check.
 */
internal fun AccessibilityNodeInfo.hasReplyBelowPlayer(
    labels: ReplyLabels,
    playerBounds: ContentBounds,
    boundsOf: (AccessibilityNodeInfo) -> ContentBounds,
): Boolean {
    val pending = ArrayDeque<AccessibilityNodeInfo>()
    pending.add(this)
    while (pending.isNotEmpty()) {
        val node = pending.removeFirst()
        val isButton = node.isVisibleToUser && node.isEnabled && node.isClickable &&
            !node.isEditable && node.className?.toString() == "android.widget.Button"
        if (isButton) {
            val matchesLabel = labels.matches(node.text) || labels.matches(node.contentDescription) ||
                labels.matches(AccessibilityNodeInfoCompat.wrap(node).hintText)
            if (matchesLabel && boundsOf(node).isDirectlyBelow(playerBounds)) return true
        }
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let(pending::addLast)
        }
    }
    return false
}
