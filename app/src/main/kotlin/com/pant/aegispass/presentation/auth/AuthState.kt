package com.pant.aegispass.presentation.auth

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)