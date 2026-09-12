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

data class UserProfileUiState(
    val profile: PublicProfileDto? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val username: String = checkNotNull(savedStateHandle["username"])

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val profile = socialRepository.getProfile(username)
                _uiState.update { it.copy(isLoading = false, profile = profile) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Gagal memuat profil") }
            }
        }
    }

    fun toggleFollow() {
        val current = _uiState.value.profile ?: return
        viewModelScope.launch {
            try {
                val nowFollowing = if (current.isFollowing) {
                    socialRepository.unfollow(username); false
                } else {
                    socialRepository.follow(username); true
                }
                _uiState.update {
                    it.copy(
                        profile = current.copy(
                            isFollowing = nowFollowing,
                            followersCount = current.followersCount + if (nowFollowing) 1 else -1,
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Gagal follow") }
            }
        }
    }
}
