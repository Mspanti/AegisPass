package com.pant.aegispass.domain.usecase

import com.pant.aegispass.RiskAssessor
import com.pant.aegispass.data.local.PasswordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RiskAssessmentUseCase {
    suspend operator fun invoke(entry: PasswordEntry, decryptedPassword: String, allEntries: List<PasswordEntry>) =
        withContext(Dispatchers.Default) {
            RiskAssessor.assessRisk(entry, decryptedPassword, allEntries)
        }
}