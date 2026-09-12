package com.zaaam.zreming.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.domain.model.ContentItem
import com.zaaam.zreming.domain.model.ContentSection
import com.zaaam.zreming.domain.model.ContinueWatchingItem
import com.zaaam.zreming.domain.usecase.ContinueWatchingUseCase
import com.zaaam.zreming.domain.usecase.GetHomeContentUseCase
import com.zaaam.zreming.domain.usecase.WatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val heroItems: List<ContentItem> = emptyList(),
    val sections: List<ContentSection> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeContentUseCase: GetHomeContentUseCase,
    private val continueWatchingUseCase: ContinueWatchingUseCase,
    private val watchlistUseCase: WatchlistUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
        observeContinueWatching()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (heroItems, sections) = getHomeContentUseCase()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        heroItems = heroItems,
                        sections = sections
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Gagal memuat beranda"
                    )
                }
            }
        }
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            continueWatchingUseCase.getContinueWatching().collect { list ->
                _uiState.update { it.copy(continueWatching = list) }
            }
        }
    }

    fun toggleWatchlist(item: ContentItem) {
        viewModelScope.launch {
            watchlistUseCase.toggleWatchlist(item)
        }
    }
}
