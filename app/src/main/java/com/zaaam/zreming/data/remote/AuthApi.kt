package com.zaaam.zreming.data.remote

import com.zaaam.zreming.data.model.ApiEnvelope
import com.zaaam.zreming.data.model.ChangePasswordRequest
import com.zaaam.zreming.data.model.ChangeUsernameRequest
import com.zaaam.zreming.data.model.ChangeUsernameResultDto
import com.zaaam.zreming.data.model.ConversationMessagesDto
import com.zaaam.zreming.data.model.ConversationsResultDto
import com.zaaam.zreming.data.model.CreateUserRequest
import com.zaaam.zreming.data.model.DeleteUserResultDto
import com.zaaam.zreming.data.model.DramaDto
import com.zaaam.zreming.data.model.DramasListResultDto
import com.zaaam.zreming.data.model.FollowActionResultDto
import com.zaaam.zreming.data.model.LoginRequest
import com.zaaam.zreming.data.model.LoginResultDto
import com.zaaam.zreming.data.model.NotificationsResultDto
import com.zaaam.zreming.data.model.CreateNobarRoomRequest
import com.zaaam.zreming.data.model.NobarFriendsResultDto
import com.zaaam.zreming.data.model.NobarInviteRequest
import com.zaaam.zreming.data.model.NobarInviteResultDto
import com.zaaam.zreming.data.model.NobarRoomDto
import com.zaaam.zreming.data.model.OwnerChangePasswordRequest
import com.zaaam.zreming.data.model.OwnerChangeUsernameRequest
import com.zaaam.zreming.data.model.OwnerUsersResultDto
import com.zaaam.zreming.data.model.PublicProfileDto
import com.zaaam.zreming.data.model.RedeemReferralRequest
import com.zaaam.zreming.data.model.RedeemReferralResultDto
import com.zaaam.zreming.data.model.SaveDramaRequest
import com.zaaam.zreming.data.model.SendMessageRequest
import com.zaaam.zreming.data.model.SendMessageResultDto
import com.zaaam.zreming.data.model.SetExpiryRequest
import com.zaaam.zreming.data.model.SetVerifiedRequest
import com.zaaam.zreming.data.model.SetVipRequest
import com.zaaam.zreming.data.model.UsersResultDto
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Backend auth/role JepVerse (Cloudflare Worker). Terpisah dari MovieZoneApi
 * (yang tetap dipakai untuk konten film/drama, tidak diubah).
 */
interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiEnvelope<LoginResultDto>

    @GET("api/owner/users")
    suspend fun ownerListUsers(
        @Header("Authorization") bearerToken: String,
        @Query("q") query: String? = null,
    ): ApiEnvelope<OwnerUsersResultDto>

    @POST("api/owner/users/create")
    suspend fun ownerCreateUser(
        @Header("Authorization") bearerToken: String,
        @Body body: CreateUserRequest,
    ): ApiEnvelope<JsonElement>

    @POST("api/owner/users/{id}/vip")
    suspend fun ownerSetVip(
        @Header("Authorization") bearerToken: String,
        @Path("id") userId: String,
        @Body body: SetVipRequest,
    ): ApiEnvelope<JsonElement>

    @POST("api/owner/users/{id}/expiry")
    suspend fun ownerSetExpiry(
        @Header("Authorization") bearerToken: String,
        @Path("id") userId: String,
        @Body body: SetExpiryRequest,
    ): ApiEnvelope<JsonElement>

    @POST("api/owner/dramas/save")
    suspend fun ownerSaveDrama(
        @Header("Authorization") bearerToken: String,
        @Body body: SaveDramaRequest,
    ): ApiEnvelope<JsonElement>

    @POST("api/owner/users/{id}/username")
    suspend fun ownerChangeUsername(
        @Header("Authorization") bearerToken: String,
        @Path("id") userId: String,
        @Body body: OwnerChangeUsernameRequest,
    ): ApiEnvelope<JsonElement>

    @POST("api/owner/users/{id}/password")
    suspend fun ownerChangePassword(
        @Header("Authorization") bearerToken: String,
        @Path("id") userId: String,
        @Body body: OwnerChangePasswordRequest,
    ): ApiEnvelope<JsonElement>

    @POST("api/owner/users/{id}/verified")
    suspend fun ownerSetVerified(
        @Header("Authorization") bearerToken: String,
        @Path("id") userId: String,
        @Body body: SetVerifiedRequest,
    ): ApiEnvelope<JsonElement>

    @POST("api/owner/users/{id}/reset-hwid")
    suspend fun ownerResetHwid(
        @Header("Authorization") bearerToken: String,
        @Path("id") userId: String,
    ): ApiEnvelope<JsonElement>

    @DELETE("api/owner/users/{id}")
    suspend fun ownerDeleteUser(
        @Header("Authorization") bearerToken: String,
        @Path("id") userId: String,
    ): ApiEnvelope<DeleteUserResultDto>

    @POST("api/referral/redeem")
    suspend fun redeemReferral(
        @Header("Authorization") bearerToken: String,
        @Body body: RedeemReferralRequest,
    ): ApiEnvelope<RedeemReferralResultDto>

    @GET("api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") bearerToken: String,
    ): ApiEnvelope<NotificationsResultDto>

    // ---------- Katalog film manual (owner CMS) — dipakai buat gabungin ke Search/Home ----------

    @GET("api/search")
    suspend fun searchJepverseDramas(@Query("q") query: String): ApiEnvelope<DramasListResultDto>

    @GET("api/dramas")
    suspend fun listJepverseDramas(): ApiEnvelope<DramasListResultDto>

    @GET("api/dramas/{id}")
    suspend fun getJepverseDrama(@Path("id") id: String): ApiEnvelope<DramaDto>

    // ---------- Nobar (Watch Party) ----------

    @GET("api/nobar/friends")
    suspend fun getNobarFriends(@Header("Authorization") bearerToken: String): ApiEnvelope<NobarFriendsResultDto>

    @POST("api/nobar/create")
    suspend fun createNobarRoom(
        @Header("Authorization") bearerToken: String,
        @Body body: CreateNobarRoomRequest,
    ): ApiEnvelope<NobarRoomDto>

    @GET("api/nobar/{roomId}")
    suspend fun getNobarRoom(@Path("roomId") roomId: String): ApiEnvelope<NobarRoomDto>

    @POST("api/nobar/{roomId}/invite")
    suspend fun inviteToNobar(
        @Header("Authorization") bearerToken: String,
        @Path("roomId") roomId: String,
        @Body body: NobarInviteRequest,
    ): ApiEnvelope<NobarInviteResultDto>

    @POST("api/nobar/{roomId}/join")
    suspend fun joinNobarRoom(
        @Header("Authorization") bearerToken: String,
        @Path("roomId") roomId: String,
    ): ApiEnvelope<NobarRoomDto>

    // ---------- Sosial: discover, profil publik, follow ----------

    @GET("api/discover/users")
    suspend fun discoverUsers(@Query("q") query: String? = null): ApiEnvelope<UsersResultDto>

    @GET("api/users/{username}")
    suspend fun getPublicProfile(
        @Header("Authorization") bearerToken: String?,
        @Path("username") username: String,
    ): ApiEnvelope<PublicProfileDto>

    @GET("api/users/{username}/followers")
    suspend fun getFollowers(@Path("username") username: String): ApiEnvelope<UsersResultDto>

    @GET("api/users/{username}/following")
    suspend fun getFollowing(@Path("username") username: String): ApiEnvelope<UsersResultDto>

    @POST("api/users/{username}/follow")
    suspend fun follow(
        @Header("Authorization") bearerToken: String,
        @Path("username") username: String,
    ): ApiEnvelope<FollowActionResultDto>

    @DELETE("api/users/{username}/follow")
    suspend fun unfollow(
        @Header("Authorization") bearerToken: String,
        @Path("username") username: String,
    ): ApiEnvelope<FollowActionResultDto>

    // ---------- Akun mandiri ----------

    @POST("api/user/username")
    suspend fun changeUsername(
        @Header("Authorization") bearerToken: String,
        @Body body: ChangeUsernameRequest,
    ): ApiEnvelope<ChangeUsernameResultDto>

    @POST("api/user/password")
    suspend fun changePassword(
        @Header("Authorization") bearerToken: String,
        @Body body: ChangePasswordRequest,
    ): ApiEnvelope<JsonElement>

    // ---------- Chat ----------

    @GET("api/chat/conversations")
    suspend fun listConversations(@Header("Authorization") bearerToken: String): ApiEnvelope<ConversationsResultDto>

    @GET("api/chat/{username}/messages")
    suspend fun getConversationMessages(
        @Header("Authorization") bearerToken: String,
        @Path("username") username: String,
    ): ApiEnvelope<ConversationMessagesDto>

    @POST("api/chat/{username}/messages")
    suspend fun sendMessage(
        @Header("Authorization") bearerToken: String,
        @Path("username") username: String,
        @Body body: SendMessageRequest,
    ): ApiEnvelope<SendMessageResultDto>
}
