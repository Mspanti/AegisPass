package com.pant.aegispass

import java.security.SecureRandom
import kotlin.random.Random // Import kotlin.random.Random

object PasswordGenerator {
    private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SPECIAL_CHARS = "!@#$%^&*()_-+=<>?/{}[]|"
    private const val ALL_CHARS = LOWERCASE_CHARS + UPPERCASE_CHARS + DIGITS + SPECIAL_CHARS

    fun generateStrongPassword(length: Int = 16): String {
        if (length < 8) {
            throw IllegalArgumentException("Password length must be at least 8 characters.")
        }

        val random = SecureRandom() // Still use SecureRandom for character selection for strong randomness
        val password = StringBuilder(length)

        // Ensure at least one character from each required set
        password.append(LOWERCASE_CHARS[random.nextInt(LOWERCASE_CHARS.length)])
        password.append(UPPERCASE_CHARS[random.nextInt(UPPERCASE_CHARS.length)])
        password.append(DIGITS[random.nextInt(DIGITS.length)])
        password.append(SPECIAL_CHARS[random.nextInt(SPECIAL_CHARS.length)])

        // Fill the remaining length with random characters from all sets
        for (i in 4 until length) {
            password.append(ALL_CHARS[random.nextInt(ALL_CHARS.length)])
        }

        // Shuffle the characters to ensure randomness
        // Use kotlin.random.Random.Default for shuffling, or create a new Random instance
        return password.toString().toCharArray().apply { shuffle(Random.Default) }.joinToString("")
    }
}