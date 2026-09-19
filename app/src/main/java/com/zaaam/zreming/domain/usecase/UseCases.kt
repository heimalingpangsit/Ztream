package com.zaaam.zreming.domain.usecase

import com.zaaam.zreming.domain.model.ContentDetail
import com.zaaam.zreming.domain.model.ContentItem
import com.zaaam.zreming.domain.model.ContentSection
import com.zaaam.zreming.domain.model.ContinueWatchingItem
import com.zaaam.zreming.domain.model.Genre
import com.zaaam.zreming.domain.model.StreamSource
import com.zaaam.zreming.domain.repository.ContentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHomeContentUseCase @Inject constructor(
    private val repository: ContentRepository
) {
    suspend operator fun invoke(): Pair<List<ContentItem>, List<ContentSection>> = coroutineScope {
        val heroDeferred = async { runCatching { repository.getHero() }.getOrDefault(emptyList()) }
        val trendingDeferred = async { runCatching { repository.getTrending(1) }.getOrDefault(emptyList()) }
        val latestMoviesDeferred = async { runCatching { repository.getLatest("movie", 1) }.getOrDefault(emptyList()) }
        val popularMoviesDeferred = async { runCatching { repository.getPopular("movie", 1) }.getOrDefault(emptyList()) }
        val popularTvDeferred = async { runCatching { repository.getPopular("tv", 1) }.getOrDefault(emptyList()) }
        val topRatedDeferred = async { runCatching { repository.getTopRated("movie", 1) }.getOrDefault(emptyList()) }

        val heroList = heroDeferred.await()

        val sections = mutableListOf<ContentSection>()
        val trending = trendingDeferred.await()
        if (trending.isNotEmpty()) {
            sections.add(ContentSection("Trending Minggu Ini", trending))
        }

        val zarstreamOriginals = try {
            repository.getJepverseOriginals()
        } catch (_: Exception) {
            emptyList()
        }
        if (zarstreamOriginals.isNotEmpty()) {
            sections.add(ContentSection("Film ZarStream", zarstreamOriginals))
        }

        val latestMovies = latestMoviesDeferred.await()
        if (latestMovies.isNotEmpty()) {
            sections.add(ContentSection("Film Bioskop Terbaru", latestMovies))
        }

        val popularMovies = popularMoviesDeferred.await()
        if (popularMovies.isNotEmpty()) {
            sections.add(ContentSection("Film Populer", popularMovies))
        }

        val popularTv = popularTvDeferred.await()
        if (popularTv.isNotEmpty()) {
            sections.add(ContentSection("Serial TV Populer", popularTv))
        }

        val topRated = topRatedDeferred.await()
        if (topRated.isNotEmpty()) {
            sections.add(ContentSection("Rating Tertinggi", topRated))
        }

        Pair(heroList.take(6), sections)
    }
}

class SearchContentUseCase @Inject constructor(
    private val repository: ContentRepository
) {
    suspend operator fun invoke(query: String, page: Int = 1): List<ContentItem> {
        return repository.search(query, page)
    }
}

class GetDetailUseCase @Inject constructor(
    private val repository: ContentRepository
) {
    suspend operator fun invoke(slug: String): ContentDetail {
        return repository.getDetail(slug)
    }
}

class GetStreamUrlUseCase @Inject constructor(
    private val repository: ContentRepository
) {
    suspend operator fun invoke(
        tmdbId: String,
        isTv: Boolean = false,
        season: Int = 1,
        episode: Int = 1
    ): StreamSource {
        return repository.getStreamSource(tmdbId, isTv, season, episode)
    }
}

class WatchlistUseCase @Inject constructor(
    private val repository: ContentRepository
) {
    fun getWatchlist(): Flow<List<ContentItem>> = repository.getWatchlist()
    suspend fun isWatchlist(id: String): Boolean = repository.isWatchlist(id)
    suspend fun toggleWatchlist(item: ContentItem) = repository.toggleWatchlist(item)
}

class ContinueWatchingUseCase @Inject constructor(
    private val repository: ContentRepository
) {
    fun getContinueWatching(): Flow<List<ContinueWatchingItem>> = repository.getContinueWatching()
    suspend fun save(item: ContinueWatchingItem) = repository.saveContinueWatching(item)
    suspend fun remove(contentId: String) = repository.removeContinueWatching(contentId)
}

class SearchHistoryUseCase @Inject constructor(
    private val repository: ContentRepository
) {
    fun getHistory(): Flow<List<String>> = repository.getSearchHistory()
    suspend fun add(query: String) = repository.addSearchHistory(query)
    suspend fun clear() = repository.clearSearchHistory()
}

class GenreUseCase @Inject constructor(
    private val repository: ContentRepository
) {
    suspend fun getGenres(): List<Genre> = repository.getGenres()
    suspend fun getDiscover(genreId: Int, page: Int = 1): List<ContentItem> = repository.getDiscover(genreId, page)
}
