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

// ---------------------------------------------------------------------------
// Realtime service (worker Nobar terpisah): undangan + chat room
// ---------------------------------------------------------------------------

@Serializable
data class NobarRealtimeInviteDto(
    val id: String = "",
    val roomId: String = "",
    val fromUsername: String = "",
    val toUsername: String = "",
    val contentTitle: String = "",
    val createdAt: Long = 0L,
)

@Serializable
data class NobarRealtimeInvitesResultDto(
    val invites: List<NobarRealtimeInviteDto> = emptyList(),
)

@Serializable
data class NobarRegisterInviteRequest(
    val roomId: String,
    val fromUsername: String,
    val toUsername: String,
    val contentTitle: String = "",
)

@Serializable
data class NobarRealtimeMessageDto(
    val id: String = "",
    val username: String = "",
    val text: String = "",
    val at: Long = 0L,
    val system: Boolean = false,
)

@Serializable
data class NobarRealtimeMessagesResultDto(
    val messages: List<NobarRealtimeMessageDto> = emptyList(),
    val participants: Int = 0,
    val now: Long = 0L,
)

@Serializable
data class NobarSendMessageRequest(
    val id: String,
    val username: String,
    val text: String,
)

@Serializable
data class NobarOkResultDto(val ok: Boolean = true)
