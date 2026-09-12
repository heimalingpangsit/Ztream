package com.zaaam.zreming.ui.nobar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.model.NobarRoomDto
import com.zaaam.zreming.data.model.PublicProfileDto
import com.zaaam.zreming.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NobarInviteUiState(
    val isCreatingRoom: Boolean = true,
    val room: NobarRoomDto? = null,
    val friends: List<PublicProfileDto> = emptyList(),
    val invitedUsernames: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

@HiltViewModel
class NobarInviteViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val contentId: String = checkNotNull(savedStateHandle["contentId"])
    private val contentTitle: String = savedStateHandle["contentTitle"] ?: "Nobar"
    private val isTv: Boolean = savedStateHandle["isTv"] ?: false
    private val season: Int = savedStateHandle["season"] ?: 1
    private val episode: Int = savedStateHandle["episode"] ?: 1
    private val posterUrl: String = savedStateHandle["posterUrl"] ?: ""

    private val _uiState = MutableStateFlow(NobarInviteUiState())
    val uiState: StateFlow<NobarInviteUiState> = _uiState.asStateFlow()

    init {
        createRoomAndLoadFriends()
    }

    private fun createRoomAndLoadFriends() {
        viewModelScope.launch {
            try {
                val room = authRepository.createNobarRoom(contentId, contentTitle, isTv, season, episode, posterUrl)
                val friends = try {
                    authRepository.getNobarFriends()
                } catch (_: Exception) {
                    emptyList()
                }
                _uiState.update { it.copy(isCreatingRoom = false, room = room, friends = friends) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreatingRoom = false, errorMessage = e.message ?: "Gagal membuat room Nobar") }
            }
        }
    }

    fun invite(username: String) {
        val roomId = _uiState.value.room?.id ?: return
        viewModelScope.launch {
            try {
                authRepository.inviteToNobar(roomId, username)
                _uiState.update { it.copy(invitedUsernames = it.invitedUsernames + username) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Gagal mengundang @$username") }
            }
        }
    }
}
