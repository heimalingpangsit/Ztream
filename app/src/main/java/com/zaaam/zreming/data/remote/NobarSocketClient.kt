package com.zaaam.zreming.data.remote

import com.zaaam.zreming.data.model.NobarSendMessageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

object NobarRealtimeConfig {
    /**
     * Satu-satunya tempat domain service Nobar ditulis. Kalau worker-nya
     * dipindah/ganti nama, ubah di sini saja.
     */
    const val BASE_HOST = "zarstream-nobar.pokaycore.workers.dev"
    const val BASE_HTTP_URL = "https://$BASE_HOST/"
    const val BASE_WS_URL = "wss://$BASE_HOST"
}

sealed class NobarEvent {
    /** [viaWebSocket] false berarti chat jalan lewat polling HTTP (fallback). */
    data class Connected(val viaWebSocket: Boolean) : NobarEvent()
    data class System(val text: String, val participants: Int) : NobarEvent()
    data class Chat(val id: String, val username: String, val text: String, val atMs: Long) : NobarEvent()
    data class Sync(val type: String, val positionMs: Long, val byUsername: String) : NobarEvent()
    data class ConnectionClosed(val reason: String) : NobarEvent()
}

/**
 * Transport chat & sinkronisasi Nobar.
 *
 * Versi lama cuma pakai WebSocket sekali coba, tanpa reconnect dan tanpa
 * jalur cadangan: begitu upgrade WebSocket gagal (sering kejadian di jaringan
 * operator atau kalau worker realtime-nya belum aktif), chat jadi seolah
 * "terkirim" di HP pengirim tapi gak pernah sampai ke peserta lain, karena
 * pesannya cuma ditambahin ke daftar lokal.
 *
 * Versi ini:
 *  - reconnect otomatis dengan backoff,
 *  - ping berkala biar koneksi gak diputus perantara,
 *  - fallback polling HTTP kalau WebSocket gak bisa dipakai,
 *  - pesan dikirim dengan id unik biar bisa dideteksi ganda (echo dari server
 *    gak bikin pesan dobel di layar).
 */
@Singleton
class NobarSocketClient @Inject constructor(
    @Named("nobarClient") private val client: OkHttpClient,
    private val realtimeApi: NobarRealtimeApi,
) {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var socketOpen = false

    @Volatile
    private var activeRoomId: String? = null

    @Volatile
    private var activeUsername: String = ""

    fun connect(roomId: String, username: String): Flow<NobarEvent> = channelFlow {
        activeRoomId = roomId
        activeUsername = username
        socketOpen = false

        val seenIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        val announcedPolling = AtomicBoolean(false)
        var lastSeen = 0L

        fun handleChat(id: String, user: String, text: String, at: Long, system: Boolean, participants: Int) {
            val key = id.ifBlank { "$user|$text|$at" }
            if (!seenIds.add(key)) return
            if (system) {
                trySend(NobarEvent.System(text, participants))
            } else {
                trySend(NobarEvent.Chat(key, user, text, at))
            }
        }

        // ---------- WebSocket: koneksi utama, reconnect otomatis ----------
        val wsJob = launch {
            var attempt = 0
            while (isActive) {
                if (!socketOpen) {
                    attempt++
                    socket?.cancel()
                    val url = NobarRealtimeConfig.BASE_WS_URL +
                        "/websocket?roomId=" + encode(roomId) +
                        "&username=" + encode(username)
                    val listener = object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            socketOpen = true
                            trySend(NobarEvent.Connected(viaWebSocket = true))
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            val obj = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return
                            when (obj.str("type")?.uppercase()) {
                                "SYSTEM" -> handleChat(
                                    id = obj.str("id").orEmpty(),
                                    user = "Sistem",
                                    text = obj.str("text").orEmpty(),
                                    at = obj.num("at") ?: System.currentTimeMillis(),
                                    system = true,
                                    participants = obj.num("participants")?.toInt() ?: 0,
                                )
                                "CHAT" -> handleChat(
                                    id = obj.str("id").orEmpty(),
                                    user = obj.str("username") ?: "?",
                                    text = obj.str("text").orEmpty(),
                                    at = obj.num("at") ?: System.currentTimeMillis(),
                                    system = false,
                                    participants = 0,
                                )
                                "PLAY", "PAUSE", "SEEK" -> trySend(
                                    NobarEvent.Sync(
                                        type = obj.str("type")!!.uppercase(),
                                        positionMs = obj.num("positionMs") ?: 0L,
                                        byUsername = obj.str("by") ?: "?",
                                    ),
                                )
                                "PONG" -> Unit
                            }
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            socketOpen = false
                            socket = null
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            socketOpen = false
                            socket = null
                        }
                    }
                    socket = runCatching {
                        client.newWebSocket(Request.Builder().url(url).build(), listener)
                    }.getOrNull()

                    // tunggu sebentar sebelum coba lagi (backoff maksimal 15 detik)
                    delay(minOf(2500L * attempt, 15_000L))
                    if (socketOpen) attempt = 0
                } else {
                    attempt = 0
                    delay(1000)
                }
            }
        }

        // ---------- Polling HTTP: jalan hanya selama WebSocket belum nyambung ----------
        val pollJob = launch {
            while (isActive) {
                if (!socketOpen) {
                    val result = runCatching { realtimeApi.getMessages(roomId, lastSeen, username) }.getOrNull()
                    if (result != null) {
                        if (announcedPolling.compareAndSet(false, true)) {
                            trySend(NobarEvent.Connected(viaWebSocket = false))
                        }
                        result.messages.forEach { msg ->
                            handleChat(
                                id = msg.id,
                                user = msg.username.ifBlank { "Sistem" },
                                text = msg.text,
                                at = msg.at,
                                system = msg.system,
                                participants = result.participants,
                            )
                            if (msg.at > lastSeen) lastSeen = msg.at
                        }
                        if (result.now > lastSeen) lastSeen = result.now
                    }
                    delay(2000)
                } else {
                    announcedPolling.set(false)
                    delay(1500)
                }
            }
        }

        awaitClose {
            wsJob.cancel()
            pollJob.cancel()
            socket?.close(1000, "leave room")
            socket = null
            socketOpen = false
            activeRoomId = null
        }
    }

    /**
     * Kirim chat. Mengembalikan id pesan supaya pemanggil bisa menandai pesan
     * yang sudah ditampilkan sendiri (biar gak dobel waktu server echo balik).
     */
    fun sendChat(text: String): String {
        val id = UUID.randomUUID().toString()
        val roomId = activeRoomId
        val payload = JsonObject(
            mapOf(
                "type" to JsonPrimitive("CHAT"),
                "id" to JsonPrimitive(id),
                "username" to JsonPrimitive(activeUsername),
                "text" to JsonPrimitive(text),
            ),
        ).toString()

        val sentOverSocket = socketOpen && socket?.send(payload) == true
        if (!sentOverSocket && roomId != null) {
            // WebSocket gak siap — lempar lewat HTTP biar pesannya tetap nyampe
            ioScope.launch {
                runCatching {
                    realtimeApi.postMessage(
                        roomId,
                        NobarSendMessageRequest(id = id, username = activeUsername, text = text),
                    )
                }
            }
        }
        return id
    }

    fun sendSync(type: String, positionMs: Long) {
        val payload = JsonObject(
            mapOf(
                "type" to JsonPrimitive(type),
                "positionMs" to JsonPrimitive(positionMs),
                "by" to JsonPrimitive(activeUsername),
            ),
        ).toString()
        socket?.send(payload)
    }

    fun disconnect() {
        socket?.close(1000, "leave room")
        socket = null
        socketOpen = false
        activeRoomId = null
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.num(key: String): Long? = this[key]?.jsonPrimitive?.let {
        it.longOrNull ?: it.contentOrNull?.toLongOrNull()
    }
}
