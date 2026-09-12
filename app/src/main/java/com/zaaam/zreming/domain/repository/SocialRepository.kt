package com.zaaam.zreming.domain.repository

import com.zaaam.zreming.data.model.ConversationSummaryDto
import com.zaaam.zreming.data.model.MessageDto
import com.zaaam.zreming.data.model.PublicProfileDto

interface SocialRepository {
    suspend fun discoverUsers(query: String = ""): List<PublicProfileDto>
    suspend fun getProfile(username: String): PublicProfileDto
    suspend fun getFollowers(username: String): List<PublicProfileDto>
    suspend fun getFollowing(username: String): List<PublicProfileDto>
    suspend fun follow(username: String): Boolean
    suspend fun unfollow(username: String): Boolean

    suspend fun listConversations(): List<ConversationSummaryDto>
    suspend fun getMessages(withUsername: String): Pair<List<MessageDto>, Int>
    suspend fun sendMessage(toUsername: String, text: String): Pair<MessageDto, Int>
}
