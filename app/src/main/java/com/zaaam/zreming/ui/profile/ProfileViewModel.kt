package com.zaaam.zreming.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.model.UserDto
import com.zaaam.zreming.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: UserDto? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(user = authRepository.currentUser()))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun logout() = authRepository.logout()

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    fun changeUsername(newUsername: String, onDone: () -> Unit) {
        if (newUsername.isBlank()) {
            _uiState.update { it.copy(isError = true, message = "Username tidak boleh kosong") }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val updated = authRepository.changeUsername(newUsername.trim())
                _uiState.update { it.copy(isSaving = false, isError = false, user = updated, message = "Username berhasil diubah") }
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, isError = true, message = e.message ?: "Gagal mengubah username") }
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, onDone: () -> Unit) {
        if (newPassword.length < 8) {
            _uiState.update { it.copy(isError = true, message = "Password baru minimal 8 karakter") }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                authRepository.changePassword(oldPassword, newPassword)
                _uiState.update { it.copy(isSaving = false, isError = false, message = "Password berhasil diubah") }
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, isError = true, message = e.message ?: "Gagal mengubah password") }
            }
        }
    }
}
