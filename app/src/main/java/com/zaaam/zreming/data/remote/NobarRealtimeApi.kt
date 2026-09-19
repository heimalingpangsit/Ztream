package com.zaaam.zreming.data.remote

import com.zaaam.zreming.data.model.NobarOkResultDto
import com.zaaam.zreming.data.model.NobarRealtimeInvitesResultDto
import com.zaaam.zreming.data.model.NobarRealtimeMessagesResultDto
import com.zaaam.zreming.data.model.NobarRegisterInviteRequest
import com.zaaam.zreming.data.model.NobarSendMessageRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Service Nobar realtime (worker terpisah dari backend auth).
 *
 * Dipakai untuk dua hal yang dulu gak jalan:
 *  1. Undangan nobar — dulu cuma ngandelin teks notifikasi backend, jadi yang
 *     diundang sering gak lihat apa-apa. Sekarang undangan juga dicatat di
 *     sini, jadi pasti kebaca yang diundang.
 *  2. Chat waktu nobar — kalau WebSocket gagal nyambung (jaringan operator
 *     nutup upgrade, dll), chat jatuh ke polling HTTP lewat endpoint ini,
 *     jadi pesan tetap nyampe.
 *
 * Kode worker-nya ada di worker-addon/nobar-realtime-worker.js (siap deploy).
 */
interface NobarRealtimeApi {

    @GET("api/nobar/invites")
    suspend fun listInvites(@Query("username") username: String): NobarRealtimeInvitesResultDto

    @POST("api/nobar/invites")
    suspend fun registerInvite(@Body body: NobarRegisterInviteRequest): NobarOkResultDto

    @DELETE("api/nobar/invites/{inviteId}")
    suspend fun dismissInvite(@Path("inviteId") inviteId: String): NobarOkResultDto

    @GET("api/nobar/room/{roomId}/messages")
    suspend fun getMessages(
        @Path("roomId") roomId: String,
        @Query("since") since: Long,
        @Query("username") username: String,
    ): NobarRealtimeMessagesResultDto

    @POST("api/nobar/room/{roomId}/messages")
    suspend fun postMessage(
        @Path("roomId") roomId: String,
        @Body body: NobarSendMessageRequest,
    ): NobarOkResultDto
}
