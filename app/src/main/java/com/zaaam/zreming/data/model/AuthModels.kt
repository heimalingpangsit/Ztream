package com.zaaam.zreming.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: ApiErrorDto? = null,
)

@Serializable
data class ApiErrorDto(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class UserDto(
    val id: String = "",
    val username: String = "",
    val role: String = "USER",
    val vip: Boolean = false,
    val verified: Boolean = false,
    val banned: Boolean = false,
    val points: Int? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val expiresAt: String? = null,
    val referralCode: String? = null,
    val deviceCount: Int = 0,
    val maxDevices: Int = 1,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResultDto(
    val token: String = "",
    val user: UserDto = UserDto(),
)

@Serializable
data class OwnerUsersResultDto(
    val users: List<UserDto> = emptyList(),
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val password: String,
    val role: String,
    val vip: Boolean = false,
    @SerialName("expiresAt") val expiresAt: String? = null,
)

@Serializable
data class SetVipRequest(val vip: Boolean)

@Serializable
data class SetExpiryRequest(val expiresAt: String?)

@Serializable
data class PublicProfileDto(
    val id: String = "",
    val username: String = "",
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val role: String = "USER",
    val vip: Boolean = false,
    val verified: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val streakCount: Int = 0,
)

@Serializable
data class UsersResultDto(val users: List<PublicProfileDto> = emptyList())

@Serializable
data class FollowActionResultDto(val following: Boolean = false)

@Serializable
data class MessageDto(
    val id: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val text: String = "",
    val createdAt: String = "",
)

@Serializable
data class ConversationSummaryDto(
    val withUsername: String = "",
    val lastMessage: MessageDto? = null,
    val unreadCount: Int = 0,
    val streakCount: Int = 0,
    val updatedAt: String = "",
)

@Serializable
data class ConversationsResultDto(val conversations: List<ConversationSummaryDto> = emptyList())

@Serializable
data class ConversationMessagesDto(
    val withUsername: String = "",
    val messages: List<MessageDto> = emptyList(),
    val streakCount: Int = 0,
)

@Serializable
data class SendMessageRequest(val text: String)

@Serializable
data class SendMessageResultDto(
    val message: MessageDto = MessageDto(),
    val streakCount: Int = 0,
)

@Serializable
data class ChangeUsernameRequest(val newUsername: String)

@Serializable
data class ChangeUsernameResultDto(
    val token: String = "",
    val user: UserDto = UserDto(),
)

@Serializable
data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)

@Serializable
data class SaveDramaRequest(
    val id: String? = null,
    val title: String,
    val description: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val genres: List<String>? = null,
    val country: String? = null,
    val year: Int? = null,
    val totalEpisodes: Int? = null,
    val status: String? = null,
    val streamUrl: String? = null,
)

@Serializable
data class OwnerChangeUsernameRequest(val newUsername: String)

@Serializable
data class OwnerChangePasswordRequest(val newPassword: String)

@Serializable
data class SetVerifiedRequest(val verified: Boolean)

@Serializable
data class DeleteUserResultDto(val deleted: Boolean = false, val username: String = "")

@Serializable
data class RedeemReferralRequest(val code: String, val deviceId: String? = null)

@Serializable
data class RedeemReferralResultDto(
    val referrerExpiresAt: String? = null,
    val myNewExpiresAt: String? = null,
)

@Serializable
data class NotificationDto(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val createdAt: String = "",
)

@Serializable
data class NotificationsResultDto(
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
)

@Serializable
data class DramaDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val poster: String = "",
    val backdrop: String = "",
    val rating: Double = 0.0,
    val genres: List<String> = emptyList(),
    val year: Int = 0,
    val totalEpisodes: Int = 0,
    val premium: Boolean = false,
    val streamUrl: String = "",
)

@Serializable
data class DramasListResultDto(val dramas: List<DramaDto> = emptyList())

