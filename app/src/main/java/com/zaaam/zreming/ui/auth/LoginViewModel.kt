package com.zaaam.zreming.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.domain.repository.AuthRepository
import com.zaaam.zreming.util.DeviceIdProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val accountSaved: Boolean = false,
    val loginSuccess: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        run {
            val saved = sessionManager.getSavedCredentials()
            val remembered = sessionManager.getRememberedUsername()
            LoginUiState(
                username = saved?.first ?: remembered ?: "",
                password = saved?.second ?: "",
                rememberMe = remembered != null,
                accountSaved = saved != null,
            )
        }
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onToggleRememberMe(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    /**
     * Tombol "SIMPAN" di kanan atas kartu login: simpan / hapus kredensial
     * yang kesimpan di perangkat ini.
     */
    fun toggleSaveAccount() {
        val state = _uiState.value
        if (state.accountSaved) {
            sessionManager.clearSavedCredentials()
            _uiState.update {
                it.copy(accountSaved = false, infoMessage = "Akun tersimpan sudah dihapus", errorMessage = null)
            }
            return
        }
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Isi username & password dulu sebelum menyimpan", infoMessage = null)
            }
            return
        }
        sessionManager.saveCredentials(state.username.trim(), state.password)
        _uiState.update {
            it.copy(accountSaved = true, infoMessage = "Akun disimpan di perangkat ini", errorMessage = null)
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username dan password wajib diisi") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            try {
                authRepository.login(
                    username = state.username.trim(),
                    password = state.password,
                    deviceId = DeviceIdProvider.getDeviceId(appContext),
                    deviceName = DeviceIdProvider.getDeviceName(),
                )
                if (state.rememberMe) {
                    sessionManager.saveRememberedUsername(state.username.trim())
                } else {
                    sessionManager.clearRememberedUsername()
                }
                if (state.accountSaved) {
                    sessionManager.saveCredentials(state.username.trim(), state.password)
                }
                _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Login gagal") }
            }
        }
    }
}
