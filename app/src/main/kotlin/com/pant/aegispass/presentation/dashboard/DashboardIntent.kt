package com.pant.aegispass.presentation.dashboard

sealed interface DashboardIntent {
    object LoadPasswords : DashboardIntent
    data class DeletePassword(val id: Long) : DashboardIntent
    data class Refresh(val force: Boolean = false) : DashboardIntent
}