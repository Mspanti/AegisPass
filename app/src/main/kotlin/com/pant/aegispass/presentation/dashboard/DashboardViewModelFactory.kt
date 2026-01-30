package com.pant.aegispass.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pant.aegispass.data.local.AppDatabase
import com.pant.aegispass.data.repository.RoomPasswordRepository
import com.pant.aegispass.domain.usecase.GetAllPasswordsUseCase
import com.pant.aegispass.domain.usecase.DeletePasswordUseCase

class DashboardViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            val dao = AppDatabase.getDatabase(context).passwordDao()
            val repo = RoomPasswordRepository(dao)
            val getAll = GetAllPasswordsUseCase(repo)
            val delete = DeletePasswordUseCase(repo)
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(getAll, delete) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}