package com.zaaam.zreming.domain.model

enum class ContentType {
    MOVIE,
    SERIES
}

data class ContentItem(
    val id: String,
    val slug: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val type: ContentType,
    val rating: Float,
    val releaseDate: String,
    val overview: String
)

data class ContentSection(
    val title: String,
    val items: List<ContentItem>
)

data class ContentDetail(
    val id: String,
    val slug: String,
    val title: String,
    val type: ContentType,
    val overview: String,
    val posterUrl: String,
    val backdropUrl: String,
    val rating: Float,
    val voteCount: Int,
    val releaseDate: String,
    val runtime: Int,
    val genres: List<Genre>,
    val episodes: List<Episode> = emptyList()
)

data class Genre(
    val id: Int,
    val name: String
)

data class Episode(
    val episodeNumber: Int,
    val seasonNumber: Int,
    val title: String,
    val overview: String,
    val stillUrl: String,
    val airDate: String,
    val runtime: Int
)

data class StreamSource(
    val serverName: String,
    val m3u8Url: String,
    val referer: String,
    val isWebEmbed: Boolean = false,
    val fallbackEmbedUrls: List<String> = emptyList()
)

data class ContinueWatchingItem(
    val contentId: String,
    val slug: String,
    val title: String,
    val posterUrl: String,
    val episodeId: String?,
    val positionSec: Long,
    val durationSec: Long,
    val lastWatchedAt: Long
)
