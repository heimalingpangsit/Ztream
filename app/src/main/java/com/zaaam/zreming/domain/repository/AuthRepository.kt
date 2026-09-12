package com.zaaam.zreming.domain.repository

import com.zaaam.zreming.data.model.UserDto

interface AuthRepository {
    fun isLoggedIn(): Boolean
    fun currentUser(): UserDto?
    suspend fun login(username: String, password: String): UserDto
    fun logout()

    suspend fun ownerListUsers(query: String = ""): List<UserDto>
    suspend fun ownerCreateUser(username: String, password: String, role: String, vip: Boolean, expiresAt: String?)
    suspend fun ownerSetVip(userId: String, vip: Boolean)
    suspend fun ownerSetExpiry(userId: String, expiresAt: String?)

    suspend fun changeUsername(newUsername: String): UserDto
    suspend fun changePassword(oldPassword: String, newPassword: String)

    suspend fun ownerSaveDrama(
        title: String,
        description: String,
        poster: String,
        genres: List<String>,
        year: Int?,
        totalEpisodes: Int,
        streamUrl: String,
    )

    suspend fun ownerChangeUsername(userId: String, newUsername: String)
    suspend fun ownerChangePassword(userId: String, newPassword: String)
    suspend fun ownerSetVerified(userId: String, verified: Boolean)
    suspend fun ownerResetHwid(userId: String)
    suspend fun ownerDeleteUser(userId: String): String

    suspend fun redeemReferral(code: String, deviceId: String?): RedeemResult
    suspend fun getNotifications(): Pair<List<com.zaaam.zreming.data.model.NotificationDto>, Int>

    suspend fun getNobarFriends(): List<com.zaaam.zreming.data.model.PublicProfileDto>
    suspend fun createNobarRoom(
        contentId: String,
        contentTitle: String,
        isTv: Boolean,
        season: Int,
        episode: Int,
        posterUrl: String,
    ): com.zaaam.zreming.data.model.NobarRoomDto
    suspend fun getNobarRoom(roomId: String): com.zaaam.zreming.data.model.NobarRoomDto
    suspend fun inviteToNobar(roomId: String, username: String)
    suspend fun joinNobarRoom(roomId: String): com.zaaam.zreming.data.model.NobarRoomDto
}

data class RedeemResult(val referrerExpiresAt: String?, val myNewExpiresAt: String?)
