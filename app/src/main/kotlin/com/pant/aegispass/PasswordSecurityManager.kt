package com.pant.aegispass

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

object PasswordSecurityManager {
    // ... existing properties and methods ...
    var shouldShowPasswordScreen: Boolean = false
    private var currentMasterPassword: String? = null // Temporarily holds master password after successful auth

    // Call this method after a successful master password setup/login
    fun setMasterPassword(password: String, context: Context) {
        currentMasterPassword = password
        val sharedPrefs = context.getSharedPreferences("AegisPassPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Generate and save salt if not already present
        var saltString = sharedPrefs.getString("master_password_salt", null)
        if (saltString == null) {
            val salt = EncryptionUtil.generateSalt() // Use EncryptionUtil to generate salt
            saltString = Base64.encodeToString(salt, Base64.DEFAULT)
            editor.putString("master_password_salt", saltString)
        }
        // ... (rest of your password hashing and saving logic)
        editor.apply()
    }

    // Call this method after successful authentication to provide the password for key derivation
    fun getMasterPassword(): String? {
        return currentMasterPassword
    }

    // Call this when the app is backgrounded or session ends to clear sensitive data
    fun clearMasterPassword() {
        currentMasterPassword = null
    }

    // ... rest of your PasswordSecurityManager logic ...
}