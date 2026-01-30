package com.pant.aegispass.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pant.aegispass.domain.usecase.DeletePasswordUseCase
import com.pant.aegispass.domain.usecase.GetAllPasswordsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getAllPasswords: GetAllPasswordsUseCase,
    private val deletePassword: DeletePasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    fun handle(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadPasswords -> loadPasswords()
            is DashboardIntent.DeletePassword -> deleteById(intent.id)
            is DashboardIntent.Refresh -> loadPasswords()
        }
    }

    private fun loadPasswords() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            getAllPasswords()
                .catch { e -> _state.value = _state.value.copy(error = e.message, isLoading = false) }
                .collectLatest { list ->
                    _state.value = DashboardState(isLoading = false, passwords = list)
                }
        }
    }

    private fun deleteById(id: Long) {
        viewModelScope.launch {
            // Retrieve the item and call delete
            val entry = getAllPasswords().let { flow ->
                // This is a simple approach: collect once to find the item
                var found: com.pant.aegispass.data.local.PasswordEntry? = null
                flow.take(1).collect { list -> found = list.firstOrNull { it.id == id } }
                found
            }
            entry?.let { deletePassword(it) }
        }
    }

    // Delete entry helper
    suspend fun deleteEntry(entry: com.pant.aegispass.data.local.PasswordEntry) {
        deletePassword(entry)
    }
}
