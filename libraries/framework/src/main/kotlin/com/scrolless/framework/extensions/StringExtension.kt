/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import java.math.BigInteger
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.pow

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val bigInt = BigInteger(1, md.digest(this.toByteArray(Charsets.UTF_8)))
    return String.format("%032x", bigInt)
}

fun String.toLocalDateTime(): LocalDateTime {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]")
    return LocalDateTime.parse(this, formatter)
}

fun String.toLocalDate(): LocalDate {
    if (this.length > 10) {
        return toLocalDateTime().toLocalDate()
    }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return LocalDate.parse(this, formatter)
}

fun String.toLocalTime(): LocalTime {
    if (this.length > 8) {
        return toLocalDateTime().toLocalTime()
    }
    val formatter = DateTimeFormatter.ofPattern("HH:mm[:ss]")
    return LocalTime.parse(this, formatter)
}

fun String.toSeconds(): Long {
    val parts = this.split(":")

    // Check if there are enough parts to convert to seconds
    if (parts.size < 2 || parts.size > 3) {
        throw IllegalArgumentException("Invalid time format")
    }

    // Use the reversed() function to iterate from the last part to the first
    val seconds = parts.reversed().withIndex().sumOf { (index, part) ->
        val partInSeconds =
            part.toIntOrNull() ?: throw IllegalArgumentException("Invalid time format")

        // Use the power operator (**) to calculate the multiplier (60 ^ index)
        partInSeconds * 60.0.pow(index.toDouble()).toInt()
    }

    return seconds.toLong()
}

fun String.toDuration(): Duration =
    Duration.ofSeconds(
        LocalTime
            .parse(this, DateTimeFormatter.ofPattern("HH:mm:ss"))
            .toSecondOfDay()
            .toLong(),
    )

// Format the name considering the preference and the surname (which could be empty)
fun String.formatName(
    surname: String?,
    surnameFirst: Boolean
): String =
    if (surname.isNullOrBlank()) {
        this
    } else {
        if (!surnameFirst) {
            "$this $surname"
        } else {
            "$surname $this"
        }
    }

// Format a name, considering other uppercase letter, multiple words, the apostrophe and inner spaces
fun String.smartFixName(forceCapitalize: Boolean = false): String = replace(Regex("(\\s)+"), " ")
    .trim()
    .split(" ").joinToString(" ") { it ->
        if (forceCapitalize) {
            it.lowercase(Locale.ROOT)
            it.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(Locale.ROOT)
                } else {
                    it.toString()
                }
            }
        } else {
            it
        }
    }
    .split("'").joinToString("'") { it ->
        if (forceCapitalize) {
            it.lowercase(Locale.ROOT)
            it.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(Locale.ROOT)
                } else {
                    it.toString()
                }
            }
        } else {
            it
        }
    }
    .split("-").joinToString("-") { it ->
        if (forceCapitalize) {
            it.lowercase(Locale.ROOT)
            it.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(Locale.ROOT)
                } else {
                    it.toString()
                }
            }
        } else {
            it
        }
    }

// Check if the string is written using letters, numbers, emoticons and only particular symbols
fun String.isSimpleText(): Boolean {
    var apostropheFound = false
    var hyphenFound = false
    var ampersandFound = false
    var openParFound = false
    var closedParFound = false

    if (this == "\'") return false
    if (this.startsWith('-')) return false
    if (this.contains("-\'")) return false
    loop@ for (s in this.replace("\\s".toRegex(), "")) {
        // Stop when the first invalid character is found
        when {
            // A surely improvable way to support non the red heart and more "ancient" emojis
            s == '♥' -> continue@loop
            s == '❤' -> continue@loop
            s == '☹' -> continue@loop
            s == '☺' -> continue@loop
            s == '️' -> continue@loop
            s.isSurrogate() -> continue@loop
            // Seems like numbers are allowed in certain countries!
            s.isDigit() -> continue@loop
            s.isLetter() -> continue@loop
            s == '(' && !openParFound -> openParFound = true
            s == ')' && !closedParFound -> closedParFound = true
            s == '-' && !hyphenFound -> hyphenFound = true
            s == '\'' && !apostropheFound -> apostropheFound = true
            s == '&' && !ampersandFound -> ampersandFound = true
            else -> return false
        }
    }
    return true
}
