package com.pant.aegispass

import java.security.MessageDigest
import java.util.Base64 // For API 26+

object PasswordHasher {

    // Hashes a string using SHA-256 algorithm
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        // Convert bytes to Base64 string for easy storage and comparison
        return Base64.getEncoder().encodeToString(digest)
    }

    // Verifies a given plaintext password against a stored hashed password
    fun verifyPassword(plaintextPassword: String, hashedPassword: String): Boolean {
        return hashPassword(plaintextPassword) == hashedPassword
    }
}