package com.pant.aegispass.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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