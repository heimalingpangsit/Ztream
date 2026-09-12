package com.zaaam.zreming.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * GANTI URL INI setelah kamu deploy nobar-realtime-worker.js sebagai worker
 * BARU & TERPISAH (butuh Durable Object, lihat komentar di file worker itu).
 * Ini BUKAN base URL JepVerse yang biasa (ApiConfig beda file) — worker ini
 * murni buat WebSocket realtime doang.
 */
object NobarRealtimeConfig {
    const val BASE_WS_URL = "wss://zarstream-nobar-realtime.YOUR_SUBDOMAIN.workers.dev"
}

sealed class NobarEvent {
    data class System(val text: String, val participants: Int) : NobarEvent()
    data class Chat(val username: String, val text: String, val atMs: Long) : NobarEvent()
    data class Sync(val type: String, val positionMs: Long, val byUsername: String) : NobarEvent() // type: PLAY|PAUSE|SEEK
    data class ConnectionClosed(val reason: String) : NobarEvent()
}

@Singleton
class NobarSocketClient @Inject constructor(
    @Named("resolverClient") private val client: OkHttpClient,
) {
    private var socket: WebSocket? = null

    fun connect(roomId: String, username: String): Flow<NobarEvent> = callbackFlow {
        val url = "${NobarRealtimeConfig.BASE_WS_URL}/websocket?roomId=$roomId&username=${java.net.URLEncoder.encode(username, "UTF-8")}"
        val request = Request.Builder().url(url).build()

        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = parseEvent(text) ?: return
                trySend(event)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                trySend(NobarEvent.ConnectionClosed(reason.ifBlank { "closed" }))
                close()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(NobarEvent.ConnectionClosed(t.message ?: "connection error"))
                close()
            }
        }

        socket = client.newWebSocket(request, listener)

        awaitClose {
            socket?.close(1000, "leave room")
            socket = null
        }
    }

    fun sendChat(text: String) {
        socket?.send("""{"type":"CHAT","text":"${escapeJson(text)}"}""")
    }

    fun sendSync(type: String, positionMs: Long) {
        socket?.send("""{"type":"$type","positionMs":$positionMs}""")
    }

    fun disconnect() {
        socket?.close(1000, "leave room")
        socket = null
    }

    private fun escapeJson(text: String): String =
        text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun parseEvent(raw: String): NobarEvent? {
        return try {
            val obj = Json.parseToJsonElement(raw) as? JsonObject ?: return null
            when (obj["type"]?.jsonPrimitive?.contentOrNull) {
                "SYSTEM" -> NobarEvent.System(
                    text = obj["text"]?.jsonPrimitive?.contentOrNull ?: "",
                    participants = obj["participants"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                )
                "CHAT" -> NobarEvent.Chat(
                    username = obj["username"]?.jsonPrimitive?.contentOrNull ?: "?",
                    text = obj["text"]?.jsonPrimitive?.contentOrNull ?: "",
                    atMs = obj["at"]?.jsonPrimitive?.long ?: System.currentTimeMillis(),
                )
                "PLAY", "PAUSE", "SEEK" -> NobarEvent.Sync(
                    type = obj["type"]!!.jsonPrimitive.content,
                    positionMs = obj["positionMs"]?.jsonPrimitive?.long ?: 0L,
                    byUsername = obj["by"]?.jsonPrimitive?.contentOrNull ?: "?",
                )
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
