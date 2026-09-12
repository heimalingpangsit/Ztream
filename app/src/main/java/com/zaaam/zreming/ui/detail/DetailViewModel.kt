package com.zaaam.zreming.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.domain.model.ContentDetail
import com.zaaam.zreming.domain.model.ContentItem
import com.zaaam.zreming.domain.usecase.GetDetailUseCase
import com.zaaam.zreming.domain.usecase.WatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: ContentDetail? = null,
    val isWatchlist: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getDetailUseCase: GetDetailUseCase,
    private val watchlistUseCase: WatchlistUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle["slug"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val detail = getDetailUseCase(slug)
                val isSaved = watchlistUseCase.isWatchlist(detail.id)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        detail = detail,
                        isWatchlist = isSaved
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Gagal memuat detail"
                    )
                }
            }
        }
    }

    fun toggleWatchlist() {
        val current = _uiState.value.detail ?: return
        viewModelScope.launch {
            val item = ContentItem(
                id = current.id,
                slug = current.slug,
                title = current.title,
                posterUrl = current.posterUrl,
                backdropUrl = current.backdropUrl,
                type = current.type,
                rating = current.rating,
                releaseDate = current.releaseDate,
                overview = current.overview
            )
            watchlistUseCase.toggleWatchlist(item)
            _uiState.update { it.copy(isWatchlist = !it.isWatchlist) }
        }
    }
}
