package com.pant.aegispass

/**
 * Utility class for sanitizing user input to prevent common injection attacks
 * and ensure data integrity.
 */
object InputSanitizer {

    /**
     * Sanitizes a string input by removing leading/trailing whitespace and
     * potentially harmful characters that could be used in SQL injection or script injection.
     * This method focuses on characters that are generally not expected in service names, usernames, or notes.
     *
     * @param input The raw string input from the user.
     * @return A sanitized string.
     */
    fun sanitizeText(input: String?): String {
        return input?.trim()?.replace("'", "") // Remove single quotes
            ?.replace("\"", "") // Remove double quotes
            ?.replace(";", "") // Remove semicolons
            ?.replace("--", "") // Remove SQL comment indicator
            ?.replace("<", "&lt;") // HTML entity encoding
            ?.replace(">", "&gt;") // HTML entity encoding
            ?.replace("&", "&amp;") // HTML entity encoding
            ?: "" // Return empty string if input is null
    }

    /**
     * Sanitizes a password string. Passwords should generally not be stripped of characters
     * as it might weaken them. This method primarily trims whitespace.
     * For passwords, strength checks are more important than sanitization of characters.
     *
     * @param password The raw password input.
     * @return A trimmed password string.
     */
    fun sanitizePassword(password: String?): String {
        return password?.trim() ?: "" // Just trim leading/trailing whitespace for passwords
    }

    /**
     * Validates if an email address is in a basic valid format.
     * This is a simple regex check and not a full RFC validation.
     *
     * @param email The email string to validate.
     * @return True if the email format is basic valid, false otherwise.
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}