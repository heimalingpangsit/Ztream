package com.zaaam.zreming.ui.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.domain.model.ContentItem
import com.zaaam.zreming.domain.usecase.WatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val watchlistUseCase: WatchlistUseCase
) : ViewModel() {

    val watchlist: StateFlow<List<ContentItem>> = watchlistUseCase.getWatchlist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeFromWatchlist(item: ContentItem) {
        viewModelScope.launch {
            watchlistUseCase.toggleWatchlist(item)
        }
    }
}
