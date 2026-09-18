package com.zaaam.zreming.data.repository

import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.data.model.ApiEnvelope
import com.zaaam.zreming.data.model.ConversationSummaryDto
import com.zaaam.zreming.data.model.MessageDto
import com.zaaam.zreming.data.model.PublicProfileDto
import com.zaaam.zreming.data.model.SendMessageRequest
import com.zaaam.zreming.data.remote.AuthApi
import com.zaaam.zreming.domain.repository.SocialRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val session: SessionManager,
    private val json: Json,
) : SocialRepository {

    override suspend fun discoverUsers(query: String): List<PublicProfileDto> =
        safeCall { api.discoverUsers(query.ifBlank { null }) }.users

    override suspend fun getProfile(username: String): PublicProfileDto =
        safeCall { api.getPublicProfile(bearerOrNull(), username) }

    override suspend fun getFollowers(username: String): List<PublicProfileDto> =
        safeCall { api.getFollowers(username) }.users

    override suspend fun getFollowing(username: String): List<PublicProfileDto> =
        safeCall { api.getFollowing(username) }.users

    override suspend fun follow(username: String): Boolean =
        safeCall { api.follow(bearer(), username) }.following

    override suspend fun unfollow(username: String): Boolean =
        safeCall { api.unfollow(bearer(), username) }.following

    override suspend fun listConversations(): List<ConversationSummaryDto> =
        safeCall { api.listConversations(bearer()) }.conversations

    override suspend fun getMessages(withUsername: String): Pair<List<MessageDto>, Int> {
        val result = safeCall { api.getConversationMessages(bearer(), withUsername) }
        return result.messages to result.streakCount
    }

    override suspend fun sendMessage(toUsername: String, text: String): Pair<MessageDto, Int> {
        val result = safeCall { api.sendMessage(bearer(), toUsername, SendMessageRequest(text)) }
        return result.message to result.streakCount
    }

    private fun bearer(): String {
        val token = session.getToken() ?: throw IllegalStateException("Belum login")
        return "Bearer $token"
    }

    private fun bearerOrNull(): String? = session.getToken()?.let { "Bearer $it" }

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
