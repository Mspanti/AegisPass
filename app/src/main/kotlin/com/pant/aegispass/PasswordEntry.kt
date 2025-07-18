package com.pant.aegispass

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single password entry to be stored in the local database.
 * The actual password and other sensitive fields will be stored as encrypted strings.
 *
 * @param id Unique identifier for the password entry (auto-generated).
 * @param serviceName The name of the service/website (e.g., "Google", "Facebook"). This can be plaintext.
 * @param username The username for the service. This can be plaintext for display.
 * @param encryptedPassword The actual password, stored as an encrypted string.
 * @param encryptedNotes Optional notes, stored as an encrypted string.
 * @param entrySalt The unique salt used for deriving the encryption key for this entry.
 * This is crucial for security and must be stored with each encrypted entry.
 * @param lastUpdated Timestamp of when the entry was last created or updated.
 */
@Entity(tableName = "password_entries")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "service_name") val serviceName: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "encrypted_password") val encryptedPassword: String,
    @ColumnInfo(name = "encrypted_notes") val encryptedNotes: String?,
    @ColumnInfo(name = "entry_salt") val entrySalt: String,
    val lastUpdated: Long = System.currentTimeMillis()
)