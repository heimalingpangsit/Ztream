package com.zaaam.zreming.domain.repository

import com.zaaam.zreming.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    suspend fun getHero(): List<ContentItem>
    suspend fun getTrending(page: Int = 1): List<ContentItem>
    suspend fun getPopular(type: String = "all", page: Int = 1): List<ContentItem>
    suspend fun getTopRated(type: String = "movie", page: Int = 1): List<ContentItem>
    suspend fun getLatest(type: String = "all", page: Int = 1): List<ContentItem>
    suspend fun getJepverseOriginals(): List<ContentItem>
    suspend fun search(query: String, page: Int = 1): List<ContentItem>
    suspend fun getGenres(): List<Genre>
    suspend fun getDiscover(genreId: Int, page: Int = 1): List<ContentItem>
    suspend fun getDetail(slug: String): ContentDetail
    suspend fun getEpisodes(slug: String, season: Int = 1): List<Episode>
    suspend fun getStreamSource(tmdbId: String, isTv: Boolean, season: Int = 1, episode: Int = 1): StreamSource

    // Local Watchlist
    fun getWatchlist(): Flow<List<ContentItem>>
    suspend fun isWatchlist(id: String): Boolean
    suspend fun toggleWatchlist(item: ContentItem)

    // Local Continue Watching
    fun getContinueWatching(): Flow<List<ContinueWatchingItem>>
    suspend fun saveContinueWatching(item: ContinueWatchingItem)
    suspend fun removeContinueWatching(contentId: String)

    // Search History
    fun getSearchHistory(): Flow<List<String>>
    suspend fun addSearchHistory(query: String)
    suspend fun clearSearchHistory()
}
