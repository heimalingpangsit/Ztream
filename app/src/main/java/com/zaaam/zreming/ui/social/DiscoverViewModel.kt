package com.zaaam.zreming.ui.social

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

data class DiscoverUiState(
    val query: String = "",
    val users: List<PublicProfileDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        search()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun search() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val users = socialRepository.discoverUsers(_uiState.value.query)
                _uiState.update { it.copy(isLoading = false, users = users) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Gagal memuat user") }
            }
        }
    }

    fun toggleFollow(user: PublicProfileDto) {
        viewModelScope.launch {
            try {
                val nowFollowing = if (user.isFollowing) {
                    socialRepository.unfollow(user.username); false
                } else {
                    socialRepository.follow(user.username); true
                }
                _uiState.update { state ->
                    state.copy(users = state.users.map { if (it.username == user.username) it.copy(isFollowing = nowFollowing) else it })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Gagal follow") }
            }
        }
    }
}
