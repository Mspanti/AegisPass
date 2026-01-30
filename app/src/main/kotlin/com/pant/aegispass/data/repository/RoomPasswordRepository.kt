package com.pant.aegispass.data.repository

import com.pant.aegispass.data.local.PasswordDao
import com.pant.aegispass.data.local.PasswordEntry
import kotlinx.coroutines.flow.Flow

class RoomPasswordRepository(private val dao: PasswordDao) : PasswordRepository {
    override fun getAllPasswords(): Flow<List<PasswordEntry>> = dao.getAllPasswords()
    override suspend fun getPasswordById(id: Long): PasswordEntry? = dao.getEntryById(id)
    override suspend fun insert(entry: PasswordEntry) = dao.insert(entry)
    override suspend fun update(entry: PasswordEntry) = dao.update(entry)
    override suspend fun delete(entry: PasswordEntry) = dao.delete(entry)
}