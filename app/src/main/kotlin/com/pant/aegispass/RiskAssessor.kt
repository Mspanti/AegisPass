package com.pant.aegispass

import com.pant.aegispass.data.local.PasswordEntry
import com.pant.aegispass.core.security.PasswordCipher
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RiskAssessor {

    data class PasswordRisk(
        val entry: PasswordEntry,
        val riskScore: Int, // 0-100, higher is riskier
        val reasons: List<String>,
        val isPasswordBreached: Boolean, // Renamed for clarity
        val isEmailBreached: Boolean // New field for email breach status
    )

    suspend fun assessRisk(
        entry: PasswordEntry,
        decryptedPassword: String,
        allEntries: List<PasswordEntry>
    ): PasswordRisk = withContext(Dispatchers.Default) { // Run heavy ops on Default dispatcher
        val reasons = mutableListOf<String>()
        var score = 0
        var isPwnedPassword = false
        var isPwnedEmail = false

        // 1. Password Strength
        val (strength, _) = PasswordStrengthChecker.checkStrength(decryptedPassword)
        when (strength) {
            PasswordStrengthChecker.Strength.WEAK -> {
                reasons.add("Weak password")
                score += 40
            }
            PasswordStrengthChecker.Strength.MEDIUM -> {
                reasons.add("Medium strength password")
                score += 20
            }
            else -> { /* Strong or Very Strong, no penalty */ }
        }

        // 2. Password Reuse
        val reusedCount = allEntries.count {
            it.id != entry.id && // Don't compare with itself
                    try {
                        // Decrypt and compare passwords
                        val otherDecryptedPassword = PasswordCipher.decrypt(
                            it.encryptedPassword,
                            SessionManager.getMasterPassword() ?: "", // Use session master password for decryption
                            android.util.Base64.decode(it.entrySalt, android.util.Base64.DEFAULT)
                        )
                        otherDecryptedPassword == decryptedPassword
                    } catch (e: Exception) {
                        false // If decryption fails, assume not reused
                    }
        }
        if (reusedCount > 0) {
            reasons.add("Password reused across $reusedCount other entries")
            score += (reusedCount * 10).coerceAtMost(30) // Max 30 for reuse
        }

        // 3. Password Age (Older passwords are riskier)
        val ageMillis = System.currentTimeMillis() - entry.lastUpdated
        val ageDays = TimeUnit.MILLISECONDS.toDays(ageMillis)

        if (ageDays > 365) { // Older than 1 year
            reasons.add("Password is over 1 year old")
            score += 20
        } else if (ageDays > 180) { // Older than 6 months
            reasons.add("Password is over 6 months old")
            score += 10
        }

        // 4. Have I Been Pwned check for Password (Network call)
        if (decryptedPassword != "DECRYPTION_ERROR") {
            try {
                val isPwnedResult = DataBreachChecker.isPasswordPwned(decryptedPassword)
                if (isPwnedResult) {
                    reasons.add("Password found in data breach (HIBP)")
                    score = (score + 50).coerceAtMost(100) // Significant penalty for breach
                    isPwnedPassword = true
                }
            } catch (e: Exception) {
                println("Error during HIBP Password check: ${e.message}")
                // Do not add score or breach status if check fails
            }
        }

        // 5. Have I Been Pwned check for Email/Username (Network call)
        if (entry.username.contains("@") && entry.username != "DECRYPTION_ERROR") { // Check if it's likely an email
            try {
                val isPwnedResult = DataBreachChecker.isEmailPwned(entry.username)
                if (isPwnedResult) {
                    reasons.add("Email/Username found in data breach (HIBP)")
                    score = (score + 40).coerceAtMost(100) // Significant penalty for email breach
                    isPwnedEmail = true
                }
            } catch (e: Exception) {
                println("Error during HIBP Email check: ${e.message}")
                // Do not add score or breach status if check fails
            }
        }


        // 6. Decryption Error (Critical issue)
        if (decryptedPassword == "DECRYPTION_ERROR") {
            reasons.add("Decryption failed (Master password mismatch or corrupted data)")
            score = 100 // Highest possible risk
        }

        // Ensure score is within 0-100 range
        score = score.coerceIn(0, 100)

        PasswordRisk(entry, score, reasons, isPwnedPassword, isPwnedEmail)
    }
}