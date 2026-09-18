package com.zaaam.zreming.data.remote

import com.zaaam.zreming.domain.model.SubtitleCue
import com.zaaam.zreming.util.SubtitleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * CATATAN PENTING (baca sebelum ubah endpoint):
 * Film di app ini diputar dari embed pihak ketiga (vidsrc/vidlink/dst) yang
 * TIDAK menyediakan file subtitle sama sekali. Untuk fitur CC, kita ambil
 * subtitle dari layanan pihak ketiga terpisah (Wyzie, community subtitle
 * index berbasis TMDB id — gratis, tanpa API key) lalu diterjemahkan pakai
 * endpoint Google Translate tidak resmi (juga gratis, tanpa API key).
 *
 * Kedua endpoint ini TIDAK RESMI/tidak ada jaminan SLA — sewaktu-waktu bisa
 * berubah format atau dimatikan. Kalau CC berhenti berfungsi, cek dulu:
 * 1. apakah SUBTITLE_SEARCH_BASE masih hidup (buka manual di browser)
 * 2. apakah TRANSLATE_BASE masih hidup / belum kena rate limit
 */
@Singleton
class SubtitleClient @Inject constructor(
    @Named("resolverClient") private val client: OkHttpClient,
) {
    private val subtitleSearchBase = "https://sub.wyzie.ru/search"
    private val translateBase = "https://translate.googleapis.com/translate_a/single"

    /** Cari & download subtitle sumber (bahasa Inggris kalau ada, kalau tidak ambil apa saja yang tersedia). */
    suspend fun fetchSourceCues(
        tmdbId: String,
        isTv: Boolean,
        season: Int,
        episode: Int,
    ): List<SubtitleCue> = withContext(Dispatchers.IO) {
        val url = buildString {
            append(subtitleSearchBase)
            append("?id=").append(tmdbId)
            if (isTv) {
                append("&season=").append(season)
                append("&episode=").append(episode)
            }
            append("&language=en")
        }

        val listJson = httpGetString(url) ?: return@withContext emptyList()
        val subUrl = firstSubtitleUrl(listJson) ?: return@withContext emptyList()
        val raw = httpGetString(subUrl) ?: return@withContext emptyList()
        SubtitleParser.parse(raw)
    }

    private fun firstSubtitleUrl(json: String): String? {
        return try {
            val arr = JSONArray(json)
            if (arr.length() == 0) return null
            arr.getJSONObject(0).optString("url").ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Terjemahkan sekumpulan teks cue ke bahasa target. Digabung per-batch
     * (dipisah delimiter unik) supaya tidak perlu 1 request per baris —
     * lebih cepat dan lebih kecil kemungkinan kena rate limit.
     */
    suspend fun translateBatch(texts: List<String>, targetLangCode: String): List<String> =
        withContext(Dispatchers.IO) {
            if (texts.isEmpty()) return@withContext emptyList()
            val delimiter = "\n||•||\n"
            val result = mutableListOf<String>()
            val chunkSize = 25

            texts.chunked(chunkSize).forEach { chunk ->
                val joined = chunk.joinToString(delimiter)
                val translated = translateText(joined, targetLangCode) ?: chunk.joinToString(delimiter)
                val parts = translated.split(delimiter.trim())
                if (parts.size == chunk.size) {
                    result.addAll(parts.map { it.trim() })
                } else {
                    // Delimiter kepotong translate engine — fallback: teks asli
                    // untuk chunk ini daripada nampilin hasil yang berantakan.
                    result.addAll(chunk)
                }
            }
            result
        }

    private fun translateText(text: String, targetLangCode: String): String? {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "$translateBase?client=gtx&sl=auto&tl=$targetLangCode&dt=t&q=$encoded"
        val body = httpGetString(url) ?: return null
        return try {
            val outer = JSONArray(body)
            val segments = outer.getJSONArray(0)
            buildString {
                for (i in 0 until segments.length()) {
                    append(segments.getJSONArray(i).getString(0))
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGetString(url: String): String? {
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            }
        } catch (_: Exception) {
            null
        }
    }
}
