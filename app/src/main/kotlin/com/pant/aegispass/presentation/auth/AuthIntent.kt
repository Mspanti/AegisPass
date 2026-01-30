package com.pant.aegispass.presentation.auth

sealed interface AuthIntent {
    data class Login(val password: String) : AuthIntent
    data class Setup(val password: String) : AuthIntent
    object Logout : AuthIntent
}