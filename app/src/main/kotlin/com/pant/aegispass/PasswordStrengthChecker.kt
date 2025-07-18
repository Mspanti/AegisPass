package com.pant.aegispass

import android.graphics.Color

/**
 * Singleton object to check password strength and provide corresponding UI feedback (text and color).
 * This checker considers length and diversity of character types (uppercase, lowercase, digits, symbols).
 */
object PasswordStrengthChecker {

    /**
     * Enum class to represent different levels of password strength.
     */
    enum class Strength {
        WEAK,
        MEDIUM,
        STRONG,
        VERY_STRONG
    }

    /**
     * Calculates the strength of a given password based on predefined criteria.
     * The score increases with length and the presence of diverse character types.
     *
     * @param password The password string to evaluate.
     * @return A Pair containing the [Strength] enum and a descriptive String message.
     */
    fun checkStrength(password: String): Pair<Strength, String> {
        var score = 0

        // 1. Length criteria
        if (password.length >= 8) score += 1 // Basic minimum length
        if (password.length >= 12) score += 1 // Better length
        if (password.length >= 16) score += 1 // Strong length

        // 2. Character type diversity
        val hasUpperCase = password.matches(Regex(".*[A-Z].*"))
        val hasLowerCase = password.matches(Regex(".*[a-z].*"))
        val hasDigit = password.matches(Regex(".*[0-9].*"))
        val hasSymbol = password.matches(Regex(".*[^a-zA-Z0-9].*"))

        if (hasUpperCase) score += 1
        if (hasLowerCase) score += 1
        if (hasDigit) score += 1
        if (hasSymbol) score += 1

        // 3. Bonus for combinations of character types
        var distinctCharTypes = 0
        if (hasUpperCase) distinctCharTypes++
        if (hasLowerCase) distinctCharTypes++
        if (hasDigit) distinctCharTypes++
        if (hasSymbol) distinctCharTypes++

        if (distinctCharTypes >= 3) score += 1 // Bonus for having at least 3 types
        if (distinctCharTypes >= 4) score += 1 // Bonus for having all 4 types

        // Map the calculated score to a Strength level and a user-friendly message
        return when {
            score < 4 -> Pair(Strength.WEAK, "Password Strength: Weak (Min 8 chars, combine letters, numbers, symbols)")
            score < 6 -> Pair(Strength.MEDIUM, "Password Strength: Medium (Aim for >12 chars, add more types)")
            score < 8 -> Pair(Strength.STRONG, "Password Strength: Strong (Good balance of length and complexity!)")
            else -> Pair(Strength.VERY_STRONG, "Password Strength: Very Strong (Excellent security!)")
        }
    }

    /**
     * Returns a specific color for UI feedback based on the given password strength.
     * @param strength The [Strength] enum.
     * @return An integer representing the color value.
     */
    fun getStrengthColor(strength: Strength): Int {
        return when (strength) {
            Strength.WEAK -> Color.RED
            Strength.MEDIUM -> Color.parseColor("#FFA500") // Orange color
            Strength.STRONG -> Color.parseColor("#00B060") // Darker green
            Strength.VERY_STRONG -> Color.parseColor("#0000FF") // Blue
        }
    }
}