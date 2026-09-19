package com.zaaam.zreming.ui.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.model.PublicProfileDto
import com.zaaam.zreming.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowListUiState(
    val users: List<PublicProfileDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val username: String = checkNotNull(savedStateHandle["username"])
    private val direction: String = checkNotNull(savedStateHandle["direction"]) // "followers" | "following"

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    val title: String get() = if (direction == "followers") "Pengikut @$username" else "Mengikuti @$username"

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val list = if (direction == "followers") {
                    socialRepository.getFollowers(username)
                } else {
                    socialRepository.getFollowing(username)
                }
                _uiState.update { it.copy(isLoading = false, users = list) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Gagal memuat daftar") }
            }
        }
    }
}
