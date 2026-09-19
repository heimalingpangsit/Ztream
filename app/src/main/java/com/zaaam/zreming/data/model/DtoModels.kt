package com.zaaam.zreming.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MovieItemDto(
    val id: Long? = null,
    val slug: String? = null,
    val tmdbId: Long? = null,
    val title: String? = null,
    val type: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val rating: String? = null,
    val voteCount: Int? = null,
    val releaseDate: String? = null,
    val overview: String? = null,
    val genres: List<String>? = null
)

@Serializable
data class MovieListResponse(
    val results: List<MovieItemDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int? = null,
    @SerialName("total_results") val totalResults: Int? = null
)

@Serializable
data class GenreDto(
    val id: Int,
    val name: String
)

@Serializable
data class GenreListResponse(
    val genres: List<GenreDto> = emptyList()
)

@Serializable
data class MovieDetailDto(
    val id: Long? = null,
    val slug: String? = null,
    val tmdbId: Long? = null,
    val title: String? = null,
    val type: String? = null,
    val tagline: String? = null,
    val overview: String? = null,
    val synopsis: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val rating: String? = null,
    val voteCount: Int? = null,
    val releaseDate: String? = null,
    val runtime: Int? = null,
    val duration: String? = null,
    val genres: List<JsonElement> = emptyList()
)

@Serializable
data class EpisodeDto(
    val episode: Int,
    val season: Int,
    val title: String? = null,
    val overview: String? = null,
    val still: String? = null,
    val airDate: String? = null,
    val runtime: Int? = null
)

@Serializable
data class EpisodeListResponse(
    val episodes: List<EpisodeDto> = emptyList()
)
