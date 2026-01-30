package com.pant.aegispass.data.repository

import com.pant.aegispass.data.local.PasswordEntry
import kotlinx.coroutines.flow.Flow

interface PasswordRepository {
    fun getAllPasswords(): Flow<List<PasswordEntry>>
    suspend fun getPasswordById(id: Long): PasswordEntry?
    suspend fun insert(entry: PasswordEntry)
    suspend fun update(entry: PasswordEntry)
    suspend fun delete(entry: PasswordEntry)
}