package com.pant.aegispass.presentation.dashboard

import com.pant.aegispass.data.local.PasswordEntry

data class DashboardState(
    val isLoading: Boolean = false,
    val passwords: List<PasswordEntry> = emptyList(),
    val unhiddenPasswordIds: Set<Long> = emptySet(),
    val error: String? = null
)