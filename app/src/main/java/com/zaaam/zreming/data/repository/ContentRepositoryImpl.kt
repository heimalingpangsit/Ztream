package com.zaaam.zreming.data.repository

import com.zaaam.zreming.data.local.ContentDao
import com.zaaam.zreming.data.local.ContinueWatchingEntity
import com.zaaam.zreming.data.local.SearchHistoryEntity
import com.zaaam.zreming.data.local.WatchlistEntity
import com.zaaam.zreming.data.model.EpisodeDto
import com.zaaam.zreming.data.model.MovieDetailDto
import com.zaaam.zreming.data.model.MovieItemDto
import com.zaaam.zreming.data.remote.MovieZoneApi
import com.zaaam.zreming.data.remote.StreamResolver
import com.zaaam.zreming.data.repository.JepverseContentRepository
import com.zaaam.zreming.domain.model.*
import com.zaaam.zreming.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val api: MovieZoneApi,
    private val dao: ContentDao,
    private val streamResolver: StreamResolver,
    private val jepverseContentRepository: JepverseContentRepository,
) : ContentRepository {

    override suspend fun getHero(): List<ContentItem> {
        return api.getHero().results.map { it.toDomain() }
    }

    override suspend fun getTrending(page: Int): List<ContentItem> {
        return api.getTrending(page).results.map { it.toDomain() }
    }

    override suspend fun getPopular(type: String, page: Int): List<ContentItem> {
        return api.getPopular(type, page).results.map { it.toDomain() }
    }

    override suspend fun getTopRated(type: String, page: Int): List<ContentItem> {
        return api.getTopRated(type, page).results.map { it.toDomain() }
    }

    override suspend fun getLatest(type: String, page: Int): List<ContentItem> {
        return api.getLatest(type, page).results.map { it.toDomain() }
    }

    /** "Film ZarStream" — katalog yang ditambah manual owner lewat Owner Panel. */
    override suspend fun getJepverseOriginals(): List<ContentItem> {
        return jepverseContentRepository.listAll()
    }

    override suspend fun search(query: String, page: Int): List<ContentItem> {
        if (query.isBlank()) return emptyList()
        val jepverseResults = jepverseContentRepository.search(query)
        val movieZoneResults = try {
            api.search(query, page).results.map { it.toDomain() }
        } catch (e: Exception) {
            if (jepverseResults.isEmpty()) throw e else emptyList()
        }
        // Film manual ZarStream ditaruh duluan — itu konten eksklusif app ini.
        return jepverseResults + movieZoneResults
    }

    override suspend fun getGenres(): List<Genre> {
        return api.getGenres().genres.map { Genre(id = it.id, name = it.name) }
    }

    override suspend fun getDiscover(genreId: Int, page: Int): List<ContentItem> {
        return api.getDiscover(genreId, page).results.map { it.toDomain() }
    }

    override suspend fun getDetail(slug: String): ContentDetail {
        if (slug.startsWith(com.zaaam.zreming.data.repository.JEPVERSE_SLUG_PREFIX)) {
            return jepverseContentRepository.getDetail(slug)
        }
        val dto = api.getDetail(slug)
        val episodes = if (dto.type?.equals("tv", ignoreCase = true) == true) {
            try {
                getEpisodes(slug, 1)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        return dto.toDomain(episodes)
    }

    override suspend fun getEpisodes(slug: String, season: Int): List<Episode> {
        return api.getEpisodes(slug, season).episodes.map { it.toDomain() }
    }

    override suspend fun getStreamSource(
        tmdbId: String,
        isTv: Boolean,
        season: Int,
        episode: Int
    ): StreamSource {
        if (tmdbId.startsWith(com.zaaam.zreming.data.repository.JEPVERSE_ID_PREFIX)) {
            val dramaId = tmdbId.removePrefix(com.zaaam.zreming.data.repository.JEPVERSE_ID_PREFIX)
            return jepverseContentRepository.getStreamSource(dramaId)
        }
        return streamResolver.resolveStream(tmdbId, isTv, season, episode)
    }

    override fun getWatchlist(): Flow<List<ContentItem>> {
        return dao.getWatchlist().map { list ->
            list.map {
                ContentItem(
                    id = it.id,
                    slug = it.slug,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    backdropUrl = "",
                    type = if (it.type.equals("series", ignoreCase = true) || it.type.equals("tv", ignoreCase = true)) ContentType.SERIES else ContentType.MOVIE,
                    rating = it.rating,
                    releaseDate = it.releaseDate,
                    overview = ""
                )
            }
        }
    }

    override suspend fun isWatchlist(id: String): Boolean {
        return dao.isWatchlist(id)
    }

    override suspend fun toggleWatchlist(item: ContentItem) {
        if (dao.isWatchlist(item.id)) {
            dao.deleteWatchlist(item.id)
        } else {
            dao.insertWatchlist(
                WatchlistEntity(
                    id = item.id,
                    slug = item.slug,
                    title = item.title,
                    posterUrl = item.posterUrl,
                    type = item.type.name,
                    rating = item.rating,
                    releaseDate = item.releaseDate
                )
            )
        }
    }

    override fun getContinueWatching(): Flow<List<ContinueWatchingItem>> {
        return dao.getContinueWatching().map { list ->
            list.map {
                ContinueWatchingItem(
                    contentId = it.contentId,
                    slug = it.slug,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    episodeId = it.episodeId,
                    positionSec = it.positionSec,
                    durationSec = it.durationSec,
                    lastWatchedAt = it.lastWatchedAt
                )
            }
        }
    }

    override suspend fun saveContinueWatching(item: ContinueWatchingItem) {
        val progress = if (item.durationSec > 0) item.positionSec.toFloat() / item.durationSec.toFloat() else 0f
        // PRD FR-5: Progress >= 95% dianggap selesai, hapus dari continue watching
        if (progress >= 0.95f) {
            dao.deleteContinueWatching(item.contentId)
        } else if (progress >= 0.05f) {
            dao.saveContinueWatching(
                ContinueWatchingEntity(
                    contentId = item.contentId,
                    slug = item.slug,
                    title = item.title,
                    posterUrl = item.posterUrl,
                    episodeId = item.episodeId,
                    positionSec = item.positionSec,
                    durationSec = item.durationSec,
                    lastWatchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun removeContinueWatching(contentId: String) {
        dao.deleteContinueWatching(contentId)
    }

    override fun getSearchHistory(): Flow<List<String>> {
        return dao.getRecentSearches().map { list -> list.map { it.query } }
    }

    override suspend fun addSearchHistory(query: String) {
        if (query.isNotBlank()) {
            dao.insertSearch(SearchHistoryEntity(query.trim()))
        }
    }

    override suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }
}

private fun MovieItemDto.toDomain(): ContentItem {
    val rawId = (tmdbId ?: id ?: 0L).toString()
    val isSeries = type?.equals("tv", ignoreCase = true) == true || slug?.startsWith("tv-") == true
    val computedSlug = slug ?: (if (isSeries) "tv-$rawId" else "movie-$rawId")
    return ContentItem(
        id = rawId,
        slug = computedSlug,
        title = title ?: "Tanpa Judul",
        posterUrl = poster ?: "",
        backdropUrl = backdrop ?: poster ?: "",
        type = if (isSeries) ContentType.SERIES else ContentType.MOVIE,
        rating = rating?.toFloatOrNull() ?: 0f,
        releaseDate = releaseDate ?: "",
        overview = overview ?: ""
    )
}

private fun MovieDetailDto.toDomain(episodes: List<Episode>): ContentDetail {
    val rawId = (tmdbId ?: id ?: 0L).toString()
    val isSeries = type?.equals("tv", ignoreCase = true) == true || slug?.startsWith("tv-") == true
    val computedSlug = slug ?: (if (isSeries) "tv-$rawId" else "movie-$rawId")

    val parsedGenres = genres.mapNotNull { element ->
        when (element) {
            is JsonPrimitive -> {
                val name = element.contentOrNull
                if (!name.isNullOrBlank()) Genre(id = 0, name = name) else null
            }
            is JsonObject -> {
                val id = element["id"]?.let { if (it is JsonPrimitive) it.intOrNull ?: 0 else 0 } ?: 0
                val name = element["name"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
                if (!name.isNullOrBlank()) Genre(id = id, name = name) else null
            }
            else -> null
        }
    }

    val parsedSynopsis = synopsis?.takeIf { it.isNotBlank() }
        ?: overview?.takeIf { it.isNotBlank() }
        ?: "Tidak ada sinopsis tersedia."

    val parsedRuntime = runtime ?: duration?.filter { it.isDigit() }?.toIntOrNull() ?: 0

    return ContentDetail(
        id = rawId,
        slug = computedSlug,
        title = title ?: "Tanpa Judul",
        type = if (isSeries) ContentType.SERIES else ContentType.MOVIE,
        overview = parsedSynopsis,
        posterUrl = poster ?: "",
        backdropUrl = backdrop ?: poster ?: "",
        rating = rating?.toFloatOrNull() ?: 0f,
        voteCount = voteCount ?: 0,
        releaseDate = releaseDate ?: "",
        runtime = parsedRuntime,
        genres = parsedGenres,
        episodes = episodes
    )
}

private fun EpisodeDto.toDomain(): Episode {
    return Episode(
        episodeNumber = episode,
        seasonNumber = season,
        title = title ?: "Episode $episode",
        overview = overview ?: "",
        stillUrl = still ?: "",
        airDate = airDate ?: "",
        runtime = runtime ?: 0
    )
}
