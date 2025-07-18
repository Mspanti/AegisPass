package com.pant.aegispass

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Utility class to provide a secure SharedPreferences instance using EncryptedSharedPreferences.
 * This encrypts data at rest using the Android Keystore system.
 */
object SecurePrefsUtil {

    private const val PREFS_FILE_NAME = "aegispass_secure_prefs"
    private var securePrefs: SharedPreferences? = null

    /**
     * Initializes and returns a singleton instance of EncryptedSharedPreferences.
     * @param context The application context.
     * @return An instance of EncryptedSharedPreferences.
     */
    fun getSecurePrefs(context: Context): SharedPreferences {
        return securePrefs ?: synchronized(this) {
            securePrefs ?: createEncryptedSharedPreferences(context).also {
                securePrefs = it
            }
        }
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        // Create or retrieve the Master Key for encryption/decryption
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        return EncryptedSharedPreferences.create(
            PREFS_FILE_NAME, // First parameter: fileName
            masterKeyAlias,  // Second parameter: masterKeyAlias
            context,         // Third parameter: context (Corrected position)
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Clears all data from the secure SharedPreferences.
     * Use with caution, as this will remove sensitive data like master password hash.
     */
    fun clearSecurePrefs(context: Context) {
        getSecurePrefs(context).edit().clear().apply()
        securePrefs = null // Invalidate the cached instance
    }
}