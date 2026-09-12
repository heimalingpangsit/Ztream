package com.zaaam.zreming.ui.owner

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class OwnerPanelUiState(
    val query: String = "",
    val users: List<UserDto> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val showCreateDialog: Boolean = false,
    val isSavingDrama: Boolean = false,
    val dramaMessage: String? = null,
    val dramaIsError: Boolean = false,
)

@HiltViewModel
class OwnerPanelViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OwnerPanelUiState())
    val uiState: StateFlow<OwnerPanelUiState> = _uiState.asStateFlow()

    init {
        search()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun openCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun dismissCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }

    fun search() {
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            try {
                val users = authRepository.ownerListUsers(_uiState.value.query)
                _uiState.update { it.copy(isLoading = false, users = users) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isError = true, message = e.message ?: "Gagal memuat user") }
            }
        }
    }

    fun createUser(username: String, password: String, role: String, vip: Boolean, expiryDays: Int?) {
        viewModelScope.launch {
            try {
                val expiresAt = expiryDays?.let { isoDateInDays(it) }
                authRepository.ownerCreateUser(username, password, role, vip, expiresAt)
                _uiState.update { it.copy(showCreateDialog = false, isError = false, message = "User @$username berhasil dibuat") }
                search()
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, message = e.message ?: "Gagal membuat user") }
            }
        }
    }

    fun setVip(userId: String, vip: Boolean) {
        viewModelScope.launch {
            try {
                authRepository.ownerSetVip(userId, vip)
                search()
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, message = e.message ?: "Gagal mengubah VIP+") }
            }
        }
    }

    fun setExpiryDays(userId: String, days: Int?) {
        viewModelScope.launch {
            try {
                val expiresAt = days?.let { isoDateInDays(it) }
                authRepository.ownerSetExpiry(userId, expiresAt)
                _uiState.update {
                    it.copy(message = if (days == null) "Expiry dihapus (tidak pernah expired)" else "Expiry diset $days hari dari sekarang")
                }
                search()
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, message = e.message ?: "Gagal mengubah expiry") }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun clearDramaMessage() = _uiState.update { it.copy(dramaMessage = null) }

    fun setVerified(userId: String, verified: Boolean) {
        viewModelScope.launch {
            try {
                authRepository.ownerSetVerified(userId, verified)
                _uiState.update { it.copy(message = if (verified) "Centang biru diaktifkan" else "Centang biru dicabut") }
                search()
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, message = e.message ?: "Gagal mengubah status verified") }
            }
        }
    }

    fun resetHwid(userId: String, username: String) {
        viewModelScope.launch {
            try {
                authRepository.ownerResetHwid(userId)
                _uiState.update { it.copy(isError = false, message = "HWID @$username direset — bisa login dari HP baru") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, message = e.message ?: "Gagal reset HWID") }
            }
        }
    }

    fun changeUsername(userId: String, newUsername: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.ownerChangeUsername(userId, newUsername)
                _uiState.update { it.copy(isError = false, message = "Username diganti jadi @$newUsername") }
                search()
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, message = e.message ?: "Gagal mengubah username") }
            }
        }
    }

    fun changePassword(userId: String, newPassword: String, onDone: () -> Unit) {
        if (newPassword.length < 8) {
            _uiState.update { it.copy(isError = true, message = "Password minimal 8 karakter") }
            return
        }
        viewModelScope.launch {
            try {
                authRepository.ownerChangePassword(userId, newPassword)
                _uiState.update { it.copy(isError = false, message = "Password member berhasil diganti") }
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, message = e.message ?: "Gagal mengubah password") }
            }
        }
    }

    fun deleteUser(userId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val username = authRepository.ownerDeleteUser(userId)
                _uiState.update { it.copy(isError = false, message = "Akun @$username berhasil dihapus permanen") }
                search()
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, message = e.message ?: "Gagal menghapus akun") }
            }
        }
    }


    fun saveDrama(
        title: String,
        description: String,
        poster: String,
        genresCsv: String,
        year: String,
        totalEpisodes: String,
        streamUrl: String,
        onDone: () -> Unit,
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(dramaIsError = true, dramaMessage = "Judul wajib diisi") }
            return
        }
        if (streamUrl.isBlank()) {
            _uiState.update { it.copy(dramaIsError = true, dramaMessage = "Link streaming wajib diisi") }
            return
        }
        _uiState.update { it.copy(isSavingDrama = true, dramaMessage = null) }
        viewModelScope.launch {
            try {
                authRepository.ownerSaveDrama(
                    title = title.trim(),
                    description = description.trim(),
                    poster = poster.trim(),
                    genres = genresCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    year = year.toIntOrNull(),
                    totalEpisodes = totalEpisodes.toIntOrNull() ?: 0,
                    streamUrl = streamUrl.trim(),
                )
                _uiState.update {
                    it.copy(isSavingDrama = false, dramaIsError = false, dramaMessage = "Film \"$title\" berhasil disimpan & langsung tayang di app")
                }
                onDone()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSavingDrama = false, dramaIsError = true, dramaMessage = e.message ?: "Gagal menyimpan film")
                }
            }
        }
    }

    companion object {
        fun isoDateInDays(days: Int): String {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.add(Calendar.DAY_OF_YEAR, days)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(cal.time)
        }

        fun formatReadable(iso: String?): String {
            if (iso.isNullOrBlank()) return "Selamanya"
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(iso) ?: return iso
                val out = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
                out.format(date)
            } catch (_: Exception) {
                iso
            }
        }

        fun isExpired(iso: String?): Boolean {
            if (iso.isNullOrBlank()) return false
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(iso) ?: return false
                date.before(Calendar.getInstance().time)
            } catch (_: Exception) {
                false
            }
        }
    }
}
