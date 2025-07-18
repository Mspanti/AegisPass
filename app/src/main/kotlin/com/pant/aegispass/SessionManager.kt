package com.pant.aegispass

/**
 * A singleton object to manage the active plaintext master password in memory.
 * This password is held in a CharArray for easier clearing and should be set upon
 * successful login/setup and cleared when the user session ends (e.g., app exits, locks).
 *
 * IMPORTANT: This CharArray should NEVER be persisted to disk.
 * Always clear this array when it's no longer needed for security.
 */
object SessionManager {
    // Volatile ensures that changes to the masterPasswordCharArray are immediately
    // visible across different threads, which is important for concurrent access.
    @Volatile
    private var masterPasswordCharArray: CharArray? = null

    /**
     * Sets the plaintext master password for the current session.
     * Before setting a new password, it attempts to overwrite any existing
     * password in memory with zeros for better security.
     * @param password The master password as a String. It will be converted to CharArray.
     */
    fun setMasterPassword(password: String) {
        // Overwrite any existing password char array with '0' for security before nulling
        masterPasswordCharArray?.fill('0')
        // Convert the new password string to a char array and store it
        masterPasswordCharArray = password.toCharArray()
    }

    /**
     * Retrieves the plaintext master password for the current session.
     * It converts the internal CharArray back into a String for use by cryptographic functions.
     * @return The master password as a String, or null if no password is set in the session.
     */
    fun getMasterPassword(): String? {
        // Convert CharArray to String. This creates a new String object.
        // For security-critical operations, using the CharArray directly is preferred
        // if the API allows, to avoid String immutability issues. However, Java crypto
        // APIs often require String or byte[].
        return masterPasswordCharArray?.joinToString("")
    }

    /**
     * Clears the master password from memory. This method should be called on logout,
     * app exit, or when the app is moved to the background for an extended period
     * to enhance security by removing sensitive data from RAM.
     */
    fun clearMasterPassword() {
        // Overwrite the actual characters in the array with '0's to sanitize memory
        masterPasswordCharArray?.fill('0')
        // Set the reference to null to allow garbage collection
        masterPasswordCharArray = null
        // System.gc() is generally not recommended for direct calls as JVM manages it,
        // but for immediate memory pressure in highly sensitive apps, it might be considered.
        // For typical Android development, letting the GC handle it is sufficient.
    }
}