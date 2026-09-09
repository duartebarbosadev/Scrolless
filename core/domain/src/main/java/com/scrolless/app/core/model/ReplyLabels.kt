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
package com.scrolless.app.core.model

/**
 * App-provided reply labels, with exactly one {recipient} placeholder per template.
 * Matches complete labels in any supplied language; contains no app-specific detection logic.
 * Templates are split once and matched literally, without interpreting them as regular expressions.
 */
class ReplyLabels(templates: Set<String>) {
    private val parts = templates.map { template ->
        val parts = normalize(template).split("{recipient}")
        require(parts.size == 2 && parts.any { it.isNotBlank() })
        parts[0] to parts[1]
    }

    init {
        require(parts.isNotEmpty())
    }

    /**
     * Checks whether [label] matches a complete translated template with a nonblank recipient.
     * Matching both sides of the placeholder rejects unrelated text that merely shares a prefix
     * and supports languages where the recipient comes first. Blank or multiline labels do not match.
     */
    fun matches(label: CharSequence?): Boolean {
        if (label.isNullOrBlank()) return false
        val text = normalize(label.toString())
        if ('\n' in text || '\r' in text) return false
        return parts.any { (before, after) ->
            text.length > before.length + after.length &&
                text.startsWith(before) && text.endsWith(after) &&
                text.substring(before.length, text.length - after.length).isNotBlank()
        }
    }

    /** Treats nonbreaking spaces and the two ellipsis forms alike so typography does not break a match. */
    private fun normalize(text: String): String = text.replace('\u00a0', ' ').replace("…", "...")
}
