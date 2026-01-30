package com.pant.aegispass.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pant.aegispass.PasswordHasher
import com.pant.aegispass.SecurePrefsUtil
import com.pant.aegispass.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    fun handleIntent(intent: AuthIntent, appContext: android.content.Context) {
        when (intent) {
            is AuthIntent.Login -> login(intent.password, appContext)
            is AuthIntent.Setup -> setup(intent.password, appContext)
            is AuthIntent.Logout -> logout()
        }
    }

    private fun login(password: String, ctx: android.content.Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val securePref = SecurePrefsUtil.getSecurePrefs(ctx)
            val storedHash = securePref.getString("MASTER_PASSWORD_HASH", null)
            val ok = withContext(Dispatchers.Default) {
                storedHash != null && PasswordHasher.verifyPassword(password, storedHash)
            }
            _state.value = _state.value.copy(isLoading = false, isAuthenticated = ok)
            if (ok) SessionManager.setMasterPassword(password)
            else _state.value = _state.value.copy(error = "Invalid password")
        }
    }

    private fun setup(password: String, ctx: android.content.Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val hash = withContext(Dispatchers.Default) { PasswordHasher.hashPassword(password) }
            val securePref = SecurePrefsUtil.getSecurePrefs(ctx)
            securePref.edit().putString("MASTER_PASSWORD_HASH", hash).apply()
            SessionManager.setMasterPassword(password)
            _state.value = _state.value.copy(isLoading = false, isAuthenticated = true)
        }
    }

    private fun logout() {
        SessionManager.clearMasterPassword()
        _state.value = AuthState()
    }
}