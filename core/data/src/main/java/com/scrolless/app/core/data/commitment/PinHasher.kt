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
package com.scrolless.app.core.data.commitment

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hashes the commitment PIN with a random salt, stored as `salt:hash` in hex.
 *
 * The PIN only adds friction against the user's own impulses, so this keeps it out of plain
 * sight rather than defending against a determined attacker.
 */
internal object PinHasher {

    // SHA-256 PBKDF2 needs API 26; minSdk is 24.
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    private val random = SecureRandom()

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return "${salt.toHex()}:${derive(pin, salt).toHex()}"
    }

    fun matches(pin: String, stored: String?): Boolean {
        val (saltHex, hashHex) = stored?.split(":")?.takeIf { it.size == 2 } ?: return false
        val salt = saltHex.hexToBytesOrNull() ?: return false
        val expected = hashHex.hexToBytesOrNull() ?: return false
        return MessageDigest.isEqual(derive(pin, salt), expected)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toIntOrNull(16)?.toByte() ?: return null
        }
    }
}
