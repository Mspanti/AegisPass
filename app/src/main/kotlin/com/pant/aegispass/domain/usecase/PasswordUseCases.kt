package com.pant.aegispass.domain.usecase

import com.pant.aegispass.data.local.PasswordEntry
import com.pant.aegispass.data.repository.PasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GetAllPasswordsUseCase(private val repo: PasswordRepository) {
    operator fun invoke(): Flow<List<PasswordEntry>> = repo.getAllPasswords()
}

class GetPasswordByIdUseCase(private val repo: PasswordRepository) {
    suspend operator fun invoke(id: Long): PasswordEntry? = withContext(Dispatchers.IO) { repo.getPasswordById(id) }
}

class InsertPasswordUseCase(private val repo: PasswordRepository) {
    suspend operator fun invoke(entry: PasswordEntry) = withContext(Dispatchers.IO) { repo.insert(entry) }
}

class UpdatePasswordUseCase(private val repo: PasswordRepository) {
    suspend operator fun invoke(entry: PasswordEntry) = withContext(Dispatchers.IO) { repo.update(entry) }
}

class DeletePasswordUseCase(private val repo: PasswordRepository) {
    suspend operator fun invoke(entry: PasswordEntry) = withContext(Dispatchers.IO) { repo.delete(entry) }
}