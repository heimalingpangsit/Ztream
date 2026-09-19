package com.zaaam.zreming.ui.nobar

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.model.NobarRoomDto
import com.zaaam.zreming.data.model.PublicProfileDto
import com.zaaam.zreming.data.nobar.NobarRoomMeta
import com.zaaam.zreming.data.nobar.NobarSyncRepository
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
    val invitingUsername: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

@HiltViewModel
class NobarInviteViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val nobarSyncRepository: NobarSyncRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Jangan pakai checkNotNull: argumen yang hilang bikin app mati mendadak.
    private val contentId: String = arg(savedStateHandle["contentId"]).ifBlank { "0" }
    private val contentTitle: String = arg(savedStateHandle["contentTitle"]).ifBlank { "Nobar" }
    private val isTv: Boolean = savedStateHandle["isTv"] ?: false
    private val season: Int = savedStateHandle["season"] ?: 1
    private val episode: Int = savedStateHandle["episode"] ?: 1
    private val posterUrl: String = arg(savedStateHandle["posterUrl"])

    private fun arg(value: String?): String =
        value?.takeIf { it.isNotBlank() && it != "-" }.orEmpty()

    private val _uiState = MutableStateFlow(NobarInviteUiState())
    val uiState: StateFlow<NobarInviteUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "init: contentId=$contentId, title=$contentTitle")
        createRoomAndLoadFriends()
    }

    private fun createRoomAndLoadFriends() {
        viewModelScope.launch {
            Log.d(TAG, "createRoomAndLoadFriends: mulai")

            val room = try {
                Log.d(TAG, "createNobarRoom: manggil API")
                val r = authRepository.createNobarRoom(
                    contentId, contentTitle, isTv, season, episode, posterUrl,
                )
                Log.d(TAG, "createNobarRoom: SUKSES roomId=${r.id}")
                r
            } catch (e: Throwable) {
                Log.e(TAG, "createNobarRoom GAGAL: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isCreatingRoom = false,
                        errorMessage = e.message ?: "Gagal membuat room Nobar",
                    )
                }
                return@launch
            }

            try {
                Log.d(TAG, "createRoomMeta: mulai")
                nobarSyncRepository.createRoomMeta(
                    room.id,
                    NobarRoomMeta(
                        hostUsername = room.hostUsername,
                        contentId = room.contentId,
                        contentTitle = room.contentTitle,
                        isTv = room.isTv,
                        season = room.season,
                        episode = room.episode,
                        posterUrl = room.posterUrl,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                Log.d(TAG, "createRoomMeta: SUKSES")
            } catch (e: Throwable) {
                Log.e(TAG, "createRoomMeta GAGAL: ${e.message}", e)
            }

            val friends = try {
                Log.d(TAG, "getNobarFriends: mulai")
                val f = authRepository.getNobarFriends()
                Log.d(TAG, "getNobarFriends: SUKSES count=${f.size}")
                f
            } catch (e: Throwable) {
                Log.e(TAG, "getNobarFriends GAGAL: ${e.message}", e)
                emptyList()
            }

            _uiState.update {
                it.copy(isCreatingRoom = false, room = room, friends = friends)
            }
            Log.d(TAG, "createRoomAndLoadFriends: SELESAI")
        }
    }

    fun invite(username: String) {
        val roomId = _uiState.value.room?.id ?: return
        if (_uiState.value.invitingUsername != null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(invitingUsername = username, errorMessage = null, infoMessage = null)
            }
            try {
                authRepository.inviteToNobar(roomId, username)
                _uiState.update {
                    it.copy(
                        invitedUsernames = it.invitedUsernames + username,
                        invitingUsername = null,
                        infoMessage = "@$username berhasil diundang",
                    )
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        invitingUsername = null,
                        errorMessage = e.message ?: "Gagal mengundang @$username",
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "NOBAR_INVITE"
    }
}