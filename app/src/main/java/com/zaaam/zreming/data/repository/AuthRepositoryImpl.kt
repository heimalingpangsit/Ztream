package com.zaaam.zreming.data.repository

import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.data.model.ApiEnvelope
import com.zaaam.zreming.data.model.ChangePasswordRequest
import com.zaaam.zreming.data.model.ChangeUsernameRequest
import com.zaaam.zreming.data.model.CreateUserRequest
import com.zaaam.zreming.data.model.LoginRequest
import com.zaaam.zreming.data.model.OwnerChangePasswordRequest
import com.zaaam.zreming.data.model.OwnerChangeUsernameRequest
import com.zaaam.zreming.data.model.RedeemReferralRequest
import com.zaaam.zreming.data.model.SaveDramaRequest
import com.zaaam.zreming.data.model.SetExpiryRequest
import com.zaaam.zreming.data.model.SetVerifiedRequest
import com.zaaam.zreming.data.model.SetVipRequest
import com.zaaam.zreming.domain.repository.RedeemResult
import com.zaaam.zreming.data.model.UserDto
import com.zaaam.zreming.data.remote.AuthApi
import com.zaaam.zreming.domain.repository.AuthRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val session: SessionManager,
    private val json: Json,
) : AuthRepository {

    override fun isLoggedIn(): Boolean = session.isLoggedIn()

    override fun currentUser(): UserDto? = session.getUser()

    override fun logout() = session.clear()

    override suspend fun login(username: String, password: String): UserDto {
        val result = safeCall { api.login(LoginRequest(username, password)) }
        session.save(result.token, result.user)
        return result.user
    }

    override suspend fun ownerListUsers(query: String): List<UserDto> {
        val token = bearer()
        val result = safeCall { api.ownerListUsers(token, query.ifBlank { null }) }
        return result.users
    }

    override suspend fun ownerCreateUser(
        username: String,
        password: String,
        role: String,
        vip: Boolean,
        expiresAt: String?,
    ) {
        val token = bearer()
        safeCall { api.ownerCreateUser(token, CreateUserRequest(username, password, role, vip, expiresAt)) }
    }

    override suspend fun ownerSetVip(userId: String, vip: Boolean) {
        val token = bearer()
        safeCall { api.ownerSetVip(token, userId, SetVipRequest(vip)) }
    }

    override suspend fun ownerSetExpiry(userId: String, expiresAt: String?) {
        val token = bearer()
        safeCall { api.ownerSetExpiry(token, userId, SetExpiryRequest(expiresAt)) }
    }

    override suspend fun changeUsername(newUsername: String): UserDto {
        val token = bearer()
        val result = safeCall { api.changeUsername(token, ChangeUsernameRequest(newUsername)) }
        session.save(result.token, result.user)
        return result.user
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String) {
        val token = bearer()
        safeCall { api.changePassword(token, ChangePasswordRequest(oldPassword, newPassword)) }
    }

    override suspend fun ownerSaveDrama(
        title: String,
        description: String,
        poster: String,
        genres: List<String>,
        year: Int?,
        totalEpisodes: Int,
        streamUrl: String,
    ) {
        val token = bearer()
        safeCall {
            api.ownerSaveDrama(
                token,
                SaveDramaRequest(
                    title = title,
                    description = description,
                    poster = poster,
                    genres = genres,
                    year = year,
                    totalEpisodes = totalEpisodes,
                    streamUrl = streamUrl,
                )
            )
        }
    }

    private fun bearer(): String {
        val token = session.getToken() ?: throw IllegalStateException("Belum login")
        return "Bearer $token"
    }

    /**
     * Semua endpoint JepVerse membalas amplop {success, data, error}. Retrofit
     * melempar HttpException untuk status non-2xx (401/403/404/dst) sebelum
     * sempat di-parse body sukses — di sini kita tangkap itu dan baca pesan
     * error asli dari body-nya supaya pesan yang sampai ke UI tetap yang dari
     * backend (mis. "Akun kamu sudah expired...") bukan cuma "HTTP 403".
     */
    private suspend fun <T> safeCall(block: suspend () -> ApiEnvelope<T>): T {
        try {
            val envelope = block()
            if (!envelope.success || envelope.data == null) {
                throw IllegalStateException(envelope.error?.message ?: "Terjadi kesalahan")
            }
            return envelope.data
        } catch (e: HttpException) {
            val message = parseErrorMessage(e.response()?.errorBody())
            throw IllegalStateException(message ?: e.message())
        }
    }

    override suspend fun ownerChangeUsername(userId: String, newUsername: String) {
        val token = bearer()
        safeCall { api.ownerChangeUsername(token, userId, OwnerChangeUsernameRequest(newUsername)) }
    }

    override suspend fun ownerChangePassword(userId: String, newPassword: String) {
        val token = bearer()
        safeCall { api.ownerChangePassword(token, userId, OwnerChangePasswordRequest(newPassword)) }
    }

    override suspend fun ownerSetVerified(userId: String, verified: Boolean) {
        val token = bearer()
        safeCall { api.ownerSetVerified(token, userId, SetVerifiedRequest(verified)) }
    }

    override suspend fun ownerResetHwid(userId: String) {
        val token = bearer()
        safeCall { api.ownerResetHwid(token, userId) }
    }

    override suspend fun ownerDeleteUser(userId: String): String {
        val token = bearer()
        return safeCall { api.ownerDeleteUser(token, userId) }.username
    }

    override suspend fun redeemReferral(code: String, deviceId: String?): RedeemResult {
        val token = bearer()
        val result = safeCall { api.redeemReferral(token, RedeemReferralRequest(code, deviceId)) }
        return RedeemResult(result.referrerExpiresAt, result.myNewExpiresAt)
    }

    override suspend fun getNotifications(): Pair<List<com.zaaam.zreming.data.model.NotificationDto>, Int> {
        val token = bearer()
        val result = safeCall { api.getNotifications(token) }
        return result.notifications to result.unreadCount
    }

    override suspend fun getNobarFriends(): List<com.zaaam.zreming.data.model.PublicProfileDto> {
        val token = bearer()
        return safeCall { api.getNobarFriends(token) }.friends
    }

    override suspend fun createNobarRoom(
        contentId: String,
        contentTitle: String,
        isTv: Boolean,
        season: Int,
        episode: Int,
        posterUrl: String,
    ): com.zaaam.zreming.data.model.NobarRoomDto {
        val token = bearer()
        return safeCall {
            api.createNobarRoom(
                token,
                com.zaaam.zreming.data.model.CreateNobarRoomRequest(contentId, contentTitle, isTv, season, episode, posterUrl)
            )
        }
    }

    override suspend fun getNobarRoom(roomId: String): com.zaaam.zreming.data.model.NobarRoomDto {
        return safeCall { api.getNobarRoom(roomId) }
    }

    override suspend fun inviteToNobar(roomId: String, username: String) {
        val token = bearer()
        safeCall { api.inviteToNobar(token, roomId, com.zaaam.zreming.data.model.NobarInviteRequest(username)) }
    }

    override suspend fun joinNobarRoom(roomId: String): com.zaaam.zreming.data.model.NobarRoomDto {
        val token = bearer()
        return safeCall { api.joinNobarRoom(token, roomId) }
    }

    private fun parseErrorMessage(errorBody: ResponseBody?): String? {
        return try {
            val text = errorBody?.string() ?: return null
            val envelope = json.decodeFromString(ApiEnvelope.serializer(JsonElement.serializer()), text)
            envelope.error?.message
        } catch (_: Exception) {
            null
        }
    }
}
