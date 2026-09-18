package com.zaaam.zreming.data.remote

import com.zaaam.zreming.domain.model.StreamSource
import com.zaaam.zreming.util.StringFog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class StreamResolver @Inject constructor(
    @Named("resolverClient") private val client: OkHttpClient
) {
    // Obfuscated endpoints to prevent reverse engineering scraping
    private val userAgent = StringFog.decrypt("FzUgMzY2O3VvdGp6cg0zND41LSl6FA56a2p0amF6DTM0bG5heiJsbnN6GyoqNj8NPzgRMy51b2ltdGlsenIREg4XFnZ6NjMxP3odPzkxNXN6GTIoNTc/dWtobnRqdGp0anoJOzw7KDN1b2ltdGls")
    private val videmTvEmbedBase = StringFog.decrypt("Mi4uKilgdXUsMz4/N3QiIyB1Pzc4Pz51Lix1")
    private val videmMovieEmbedBase = StringFog.decrypt("Mi4uKilgdXUsMz4/N3QiIyB1Pzc4Pz51NzUsMz91")
    private val videmApiBase = StringFog.decrypt("Mi4uKilgdXUsMz4/N3QiIyB1OyozdCoyKg==")
    private val videmBase = StringFog.decrypt("Mi4uKilgdXUsMz4/N3QiIyB1")
    private val movieUniverseRef = StringFog.decrypt("Mi4uKilgdXU3NSwzPy80Myw/KCk/dCkxMzR1")
    private val vidsrcBase = StringFog.decrypt("Mi4uKilgdXUsMz4pKDl0LjV1")

    suspend fun resolveStream(
        tmdbId: String,
        isTv: Boolean = false,
        season: Int = 1,
        episode: Int = 1
    ): StreamSource = withContext(Dispatchers.IO) {
        val embeds = if (isTv) {
            listOf(
                "https://vidsrc.to/embed/tv/$tmdbId/$season/$episode",
                "https://vidlink.pro/tv/$tmdbId/$season/$episode",
                "https://multiembed.mov/?video_id=$tmdbId&tmdb=1&s=$season&e=$episode",
                "https://www.2embed.cc/embedtv/$tmdbId&s=$season&e=$episode"
            )
        } else {
            listOf(
                "https://vidsrc.to/embed/movie/$tmdbId",
                "https://vidlink.pro/movie/$tmdbId",
                "https://multiembed.mov/?video_id=$tmdbId&tmdb=1",
                "https://www.2embed.cc/embed/$tmdbId"
            )
        }

        val embedUrl = if (isTv) {
            "$videmTvEmbedBase$tmdbId/$season/$episode?autoplay=true"
        } else {
            "$videmMovieEmbedBase$tmdbId?autoplay=true"
        }

        try {
            // 1. Ambil token Q dari embed page
            val req1 = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", userAgent)
                .header("Referer", movieUniverseRef)
                .build()

            val html = client.newCall(req1).execute().body?.string() ?: ""
            val matcher = Pattern.compile("var Q = (\\{.*?\\});").matcher(html)
            if (!matcher.find()) {
                return@withContext StreamSource(
                    serverName = "VidSrc Server",
                    m3u8Url = embeds[0],
                    referer = vidsrcBase,
                    isWebEmbed = true,
                    fallbackEmbedUrls = embeds
                )
            }

            val token = JSONObject(matcher.group(1) ?: "{}").optString("t")
            if (token.isEmpty()) {
                return@withContext StreamSource(
                    serverName = "VidSrc Server",
                    m3u8Url = embeds[0],
                    referer = vidsrcBase,
                    isWebEmbed = true,
                    fallbackEmbedUrls = embeds
                )
            }

            // 2. Ambil list server
            val sourcesUrl = "$videmApiBase?a=sources&id=$tmdbId&t=${URLEncoder.encode(token, "UTF-8")}"
            val req2 = Request.Builder()
                .url(sourcesUrl)
                .header("User-Agent", userAgent)
                .header("Referer", embedUrl)
                .build()

            val sourcesJsonStr = client.newCall(req2).execute().body?.string() ?: "{}"
            val sourcesJson = JSONObject(sourcesJsonStr)
            val servers = sourcesJson.optJSONArray("servers")
            if (servers == null || servers.length() == 0) {
                return@withContext StreamSource(
                    serverName = "VidSrc Server",
                    m3u8Url = embeds[0],
                    referer = vidsrcBase,
                    isWebEmbed = true,
                    fallbackEmbedUrls = embeds
                )
            }

            for (i in 0 until minOf(servers.length(), 5)) {
                try {
                    val selectedServer = servers.getJSONObject(i)
                    val ref = selectedServer.getString("ref")
                    val serverName = selectedServer.optString("name", "Videm Server")

                    // 3. Ambil URL stream m3u8
                    val playUrl = "$videmApiBase?a=play&ref=${URLEncoder.encode(ref, "UTF-8")}"
                    val req3 = Request.Builder()
                        .url(playUrl)
                        .header("User-Agent", userAgent)
                        .header("Referer", embedUrl)
                        .build()

                    val playJsonStr = client.newCall(req3).execute().body?.string() ?: "{}"
                    val playJson = JSONObject(playJsonStr)
                    val streamPath = playJson.optString("url")

                    if (streamPath.isNotEmpty()) {
                        val fullM3u8Url = if (streamPath.startsWith("http")) streamPath else "$videmBase$streamPath"
                        return@withContext StreamSource(
                            serverName = serverName,
                            m3u8Url = fullM3u8Url,
                            referer = videmBase,
                            isWebEmbed = false,
                            fallbackEmbedUrls = embeds
                        )
                    }
                } catch (_: Exception) {}
            }

            StreamSource(
                serverName = "VidSrc Server",
                m3u8Url = embeds[0],
                referer = vidsrcBase,
                isWebEmbed = true,
                fallbackEmbedUrls = embeds
            )
        } catch (e: Exception) {
            StreamSource(
                serverName = "VidSrc Server",
                m3u8Url = embeds[0],
                referer = vidsrcBase,
                isWebEmbed = true,
                fallbackEmbedUrls = embeds
            )
        }
    }
}
