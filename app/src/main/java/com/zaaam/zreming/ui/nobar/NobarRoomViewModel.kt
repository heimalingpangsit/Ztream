package com.zaaam.zreming.ui.nobar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NobarRoomUiState(
    val isJoining: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class NobarRoomViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NobarRoomUiState())
    val uiState: StateFlow<NobarRoomUiState> = _uiState.asStateFlow()

    /**
     * Validasi kode room lalu panggil [onSuccess] dengan roomId.
     * Kode dinormalisasi: kalau user cuma ketik random part (misal "wksT7b"),
     * otomatis di-prefix jadi "ZarStream-wksT7b".
     *
     * PENTING: prefix "ZarStream-" di sini cuma jaga-jaga buat kode yang
     * diketik manual tanpa prefix. Kode YANG BENERAN DIBIKIN pas host tekan
     * "Mulai Nobar" itu di-generate di BACKEND (worker), bukan di app ini —
     * jadi kalau backend-nya masih ngasih prefix lain, ini gak akan nolong.
     * Itu perlu diubah di endpoint POST /api/nobar/create punya kamu.
     */
    fun join(rawCode: String, onSuccess: (String) -> Unit) {
        val code = normalizeRoomCode(rawCode)
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorMessage = null) }
            try {
                val room = authRepository.joinNobarRoom(code)
                _uiState.update { it.copy(isJoining = false) }
                onSuccess(room.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isJoining = false,
                        errorMessage = e.message ?: "Kode room tidak valid atau sudah berakhir",
                    )
                }
            }
        }
    }

    private fun normalizeRoomCode(input: String): String {
        val cleaned = input.trim()
        return if (cleaned.startsWith("ZarStream-", ignoreCase = true)) {
            cleaned
        } else {
            "ZarStream-$cleaned"
        }
    }
}