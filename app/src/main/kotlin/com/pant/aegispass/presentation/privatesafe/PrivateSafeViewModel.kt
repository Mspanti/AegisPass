package com.pant.aegispass.presentation.privatesafe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pant.aegispass.domain.usecase.FileMoveUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class PrivateSafeViewModel(private val fileMoveUseCase: FileMoveUseCase) : ViewModel() {

    private val _state = MutableStateFlow(PrivateSafeState())
    val state: StateFlow<PrivateSafeState> = _state

    fun handle(intent: PrivateSafeIntent) {
        when (intent) {
            is PrivateSafeIntent.LoadGallery -> loadGallery()
            is PrivateSafeIntent.MoveToPrivate -> moveToPrivate(intent.sourceUri, intent.fileName)
            is PrivateSafeIntent.DeletePrivate -> deletePrivate(intent.filePath)
        }
    }

    private fun loadGallery() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            fileMoveUseCase.listPrivateFiles()
                .catch { e -> _state.value = _state.value.copy(error = e.message, isLoading = false) }
                .collect { list ->
                    _state.value = PrivateSafeState(isLoading = false, items = list)
                }
        }
    }

    private fun moveToPrivate(sourceUri: android.net.Uri, fileName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val ok = fileMoveUseCase.moveToPrivate(sourceUri, fileName)
            _state.value = _state.value.copy(isLoading = false)
            if (ok) loadGallery()
        }
    }

    private fun deletePrivate(filePath: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val ok = fileMoveUseCase.deletePrivate(filePath)
            _state.value = _state.value.copy(isLoading = false)
            if (ok) loadGallery()
        }
    }
}