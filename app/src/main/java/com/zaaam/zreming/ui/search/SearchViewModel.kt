package com.zaaam.zreming.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.domain.model.ContentItem
import com.zaaam.zreming.domain.model.Genre
import com.zaaam.zreming.domain.usecase.GenreUseCase
import com.zaaam.zreming.domain.usecase.SearchContentUseCase
import com.zaaam.zreming.domain.usecase.SearchHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searchResults: List<ContentItem> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    val searchHistory: List<String> = emptyList(),
    val errorMessage: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchContentUseCase: SearchContentUseCase,
    private val genreUseCase: GenreUseCase,
    private val searchHistoryUseCase: SearchHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        loadGenres()
        observeHistory()

        viewModelScope.launch {
            queryFlow
                .debounce(400) // PRD FR-2: 400ms debounce
                .distinctUntilChanged()
                .collectLatest { q ->
                    if (q.length >= 2) { // PRD FR-2: Minimal 2 karakter
                        performSearch(q)
                    } else if (q.isEmpty() && _uiState.value.selectedGenreId == null) {
                        _uiState.update { it.copy(searchResults = emptyList()) }
                    }
                }
        }
    }

    private fun loadGenres() {
        viewModelScope.launch {
            try {
                val list = genreUseCase.getGenres()
                _uiState.update { it.copy(genres = list) }
            } catch (_: Exception) {}
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            searchHistoryUseCase.getHistory().collect { hist ->
                _uiState.update { it.copy(searchHistory = hist) }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        queryFlow.value = newQuery
    }

    fun onGenreSelected(genreId: Int?) {
        _uiState.update { it.copy(selectedGenreId = genreId, query = "") }
        if (genreId == null) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val items = genreUseCase.getDiscover(genreId, 1)
                _uiState.update { it.copy(isLoading = false, searchResults = items) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                searchHistoryUseCase.add(query)
                val results = searchContentUseCase(query, 1)
                _uiState.update { it.copy(isLoading = false, searchResults = results) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryUseCase.clear()
        }
    }
}
