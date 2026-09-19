package com.zaaam.zreming.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.model.MessageDto
import com.zaaam.zreming.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageDto> = emptyList(),
    val streakCount: Int = 0,
    val draft: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val withUsername: String = checkNotNull(savedStateHandle["username"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val (messages, streakCount) = socialRepository.getMessages(withUsername)
                _uiState.update { it.copy(isLoading = false, messages = messages, streakCount = streakCount) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Gagal memuat chat") }
            }
        }
    }

    fun onDraftChange(value: String) = _uiState.update { it.copy(draft = value) }

    fun send() {
        val text = _uiState.value.draft.trim()
        if (text.isEmpty() || _uiState.value.isSending) return
        _uiState.update { it.copy(isSending = true, draft = "") }
        viewModelScope.launch {
            try {
                val (message, streakCount) = socialRepository.sendMessage(withUsername, text)
                _uiState.update {
                    it.copy(isSending = false, messages = it.messages + message, streakCount = streakCount)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, errorMessage = e.message ?: "Gagal mengirim pesan", draft = text) }
            }
        }
    }
}
