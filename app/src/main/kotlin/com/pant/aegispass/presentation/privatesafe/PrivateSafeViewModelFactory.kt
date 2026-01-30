package com.pant.aegispass.presentation.privatesafe

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pant.aegispass.data.MediaRepository
import com.pant.aegispass.domain.usecase.FileMoveUseCase

class PrivateSafeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrivateSafeViewModel::class.java)) {
            val mediaRepository = MediaRepository(context, context.contentResolver)
            val useCase = FileMoveUseCase(mediaRepository)
            @Suppress("UNCHECKED_CAST")
            return PrivateSafeViewModel(useCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}