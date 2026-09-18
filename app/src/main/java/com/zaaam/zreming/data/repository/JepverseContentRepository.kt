package com.zaaam.zreming.data.repository

import com.zaaam.zreming.data.model.DramaDto
import com.zaaam.zreming.data.remote.AuthApi
import com.zaaam.zreming.domain.model.ContentDetail
import com.zaaam.zreming.domain.model.ContentItem
import com.zaaam.zreming.domain.model.ContentType
import com.zaaam.zreming.domain.model.Genre
import com.zaaam.zreming.domain.model.StreamSource
import javax.inject.Inject
import javax.inject.Singleton

/** Prefix ini yang dipakai buat bedain "film manual JepVerse" dari film MovieZoneApi
 *  di semua lapisan (search, detail, player) — supaya alurnya bisa nyambung ke
 *  streamUrl langsung tanpa lewat StreamResolver (scraping vidsrc/vidlink dst). */
const val JEPVERSE_ID_PREFIX = "jv_"
const val JEPVERSE_SLUG_PREFIX = "jv-"

/**
 * Terpisah dari ContentRepositoryImpl (yang urus MovieZoneApi) supaya nggak
 * mengganggu kode film TMDB yang sudah ada — ini murni lapisan tambahan yang
 * di-merge di titik-titik tertentu (search, home, detail, player).
 */
@Singleton
class JepverseContentRepository @Inject constructor(
    private val api: AuthApi,
) {
    suspend fun search(query: String): List<ContentItem> {
        if (query.isBlank()) return emptyList()
        return try {
            val result = api.searchJepverseDramas(query)
            if (!result.success) return emptyList()
            (result.data?.dramas ?: emptyList()).map { it.toContentItem() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun listAll(): List<ContentItem> {
        return try {
            val result = api.listJepverseDramas()
            if (!result.success) return emptyList()
            (result.data?.dramas ?: emptyList()).map { it.toContentItem() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getDetail(slug: String): ContentDetail {
        val id = slug.removePrefix(JEPVERSE_SLUG_PREFIX)
        val result = api.getJepverseDrama(id)
        val drama = result.data ?: throw IllegalStateException(
            result.error?.message ?: "Film tidak ditemukan"
        )
        return ContentDetail(
            id = JEPVERSE_ID_PREFIX + drama.id,
            slug = JEPVERSE_SLUG_PREFIX + drama.id,
            title = drama.title,
            type = ContentType.MOVIE,
            overview = drama.description,
            posterUrl = drama.poster,
            backdropUrl = drama.backdrop.ifBlank { drama.poster },
            rating = drama.rating.toFloat(),
            voteCount = 0,
            releaseDate = if (drama.year > 0) drama.year.toString() else "",
            runtime = 0,
            genres = drama.genres.mapIndexed { i, name -> Genre(id = i, name = name) },
            episodes = emptyList(),
        )
    }

    /** streamUrl yang diisi owner dianggap link embed pihak ketiga (kayak
     *  vidsrc/vidlink dsb) — makanya defaultnya isWebEmbed = true, dirender
     *  lewat WebView di PlayerScreen, bukan ExoPlayer langsung. Kalau
     *  ternyata linknya .m3u8 murni, ExoPlayer tetap dicoba duluan (lihat
     *  PlayerViewModel/StreamResolver untuk logic pemilihannya). */
    suspend fun getStreamSource(dramaId: String): StreamSource {
        val result = api.getJepverseDrama(dramaId)
        val drama = result.data ?: throw IllegalStateException(
            result.error?.message ?: "Film tidak ditemukan"
        )
        if (drama.streamUrl.isBlank()) {
            throw IllegalStateException("Link streaming untuk film ini belum diisi owner")
        }
        val looksLikeDirectFile = drama.streamUrl.contains(".m3u8") || drama.streamUrl.contains(".mp4")
        return StreamSource(
            serverName = "ZarStream Manual",
            m3u8Url = drama.streamUrl,
            referer = "",
            isWebEmbed = !looksLikeDirectFile,
        )
    }

    private fun DramaDto.toContentItem(): ContentItem = ContentItem(
        id = JEPVERSE_ID_PREFIX + id,
        slug = JEPVERSE_SLUG_PREFIX + id,
        title = title,
        posterUrl = poster,
        backdropUrl = backdrop.ifBlank { poster },
        type = ContentType.MOVIE,
        rating = rating.toFloat(),
        releaseDate = if (year > 0) year.toString() else "",
        overview = description,
    )
}
