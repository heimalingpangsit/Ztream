package com.zaaam.zreming.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NobarRoomDto(
    val id: String = "",
    val hostId: String = "",
    val hostUsername: String = "",
    val contentId: String = "",
    val contentTitle: String = "",
    val isTv: Boolean = false,
    val season: Int = 1,
    val episode: Int = 1,
    val posterUrl: String = "",
    val participantIds: List<String> = emptyList(),
    val participantUsernames: List<String> = emptyList(),
    val createdAt: String = "",
)

@Serializable
data class CreateNobarRoomRequest(
    val contentId: String,
    val contentTitle: String,
    val isTv: Boolean,
    val season: Int,
    val episode: Int,
    val posterUrl: String,
)

@Serializable
data class NobarInviteRequest(val username: String)

@Serializable
data class NobarInviteResultDto(val invited: String = "")

@Serializable
data class NobarFriendsResultDto(val friends: List<PublicProfileDto> = emptyList())
