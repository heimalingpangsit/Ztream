package com.zaaam.zreming.ui.nobar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.domain.repository.AuthRepository
import com.zaaam.zreming.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NobarInviteSource { CHAT, NOTIFICATION }

data class NobarInviteItem(
    val id: String,
    val roomCode: String,
    val fromUsername: String,
    val title: String,
    val body: String,
    val source: NobarInviteSource,
) {
    val canJoin: Boolean get() = roomCode.isNotBlank()
}

data class NobarHubUiState(
    val isJoining: Boolean = false,
    val isLoadingInvites: Boolean = true,
    val invites: List<NobarInviteItem> = emptyList(),
    val errorMessage: String? = null,
)

private val ROOM_CODE_REGEX = Regex("(?:ZarStream|ZarNobar|Nobar|Room)[-_][A-Za-z0-9]{4,}", RegexOption.IGNORE_CASE)
private val LOOSE_CODE_REGEX = Regex("\\b[A-Za-z0-9]{3,}-[A-Za-z0-9]{4,}\\b")
private val NOBAR_KEYWORDS = listOf("nobar", "nonton bareng", "watch party", "undang", "invite")

@HiltViewModel
class NobarHubViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val socialRepository: SocialRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NobarHubUiState())
    val uiState: StateFlow<NobarHubUiState> = _uiState.asStateFlow()

    init {
        loadInvites()
    }

    fun loadInvites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInvites = true, errorMessage = null) }
            val me = sessionManager.getUser()?.username.orEmpty()
            val collected = LinkedHashMap<String, NobarInviteItem>()

            fun put(item: NobarInviteItem) {
                val key = item.roomCode.lowercase().ifBlank { "raw:" + item.id }
                if (!collected.containsKey(key)) collected[key] = item
            }

            runCatching { loadInvitesFromChat(me) }.getOrNull()?.forEach(::put)

            runCatching { authRepository.getNotifications() }.getOrNull()?.first
                ?.filter(::looksLikeNobarNotification)
                ?.forEach { n ->
                    put(
                        NobarInviteItem(
                            id = n.id.ifBlank { n.createdAt },
                            roomCode = extractRoomCode(n.body) ?: extractRoomCode(n.title).orEmpty(),
                            fromUsername = "",
                            title = n.title.ifBlank { "Undangan Nobar" },
                            body = n.body,
                            source = NobarInviteSource.NOTIFICATION,
                        )
                    )
                }

            _uiState.update {
                it.copy(isLoadingInvites = false, invites = collected.values.toList())
            }
        }
    }

    private suspend fun loadInvitesFromChat(me: String): List<NobarInviteItem> = coroutineScope {
        val conversations = runCatching { socialRepository.listConversations() }.getOrNull().orEmpty()
        val results = mutableListOf<NobarInviteItem>()

        conversations.forEach { conv ->
            val last = conv.lastMessage
            if (last != null && !last.senderUsername.equals(me, ignoreCase = true)) {
                NobarInviteMarker.parse(last.text)?.let { (roomId, title) ->
                    results += chatInvite(conv.withUsername, roomId, title, last.id)
                }
            }
        }

        val deep = conversations.take(8).map { conv ->
            async {
                runCatching { socialRepository.getMessages(conv.withUsername) }.getOrNull()
                    ?.first
                    ?.takeLast(40)
                    ?.filter { !it.senderUsername.equals(me, ignoreCase = true) }
                    ?.mapNotNull { msg ->
                        NobarInviteMarker.parse(msg.text)?.let { (roomId, title) ->
                            chatInvite(conv.withUsername, roomId, title, msg.id)
                        }
                    }
                    .orEmpty()
            }
        }
        deep.forEach { results += it.await() }
        results.reversed()
    }

    private fun chatInvite(from: String, roomId: String, title: String, messageId: String) =
        NobarInviteItem(
            id = messageId.ifBlank { "$from-$roomId" },
            roomCode = roomId,
            fromUsername = from,
            title = title.ifBlank { "Undangan Nobar" },
            body = "@$from ngajak kamu nobar",
            source = NobarInviteSource.CHAT,
        )

    private fun looksLikeNobarNotification(n: com.zaaam.zreming.data.model.NotificationDto): Boolean {
        val haystack = (n.type + " " + n.title + " " + n.body).lowercase()
        return NOBAR_KEYWORDS.any { haystack.contains(it) } || ROOM_CODE_REGEX.containsMatchIn(haystack)
    }

    private fun extractRoomCode(text: String?): String? {
        if (text.isNullOrBlank()) return null
        ROOM_CODE_REGEX.find(text)?.let { return it.value }
        NobarInviteMarker.parse(text)?.let { return it.first }
        return LOOSE_CODE_REGEX.find(text)?.value
    }

    fun join(rawCode: String, onSuccess: (roomId: String) -> Unit) {
        val code = normalizeRoomCode(rawCode)
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorMessage = null) }
            try {
                val room = authRepository.joinNobarRoom(code)
                _uiState.update { it.copy(isJoining = false) }
                onSuccess(room.id.ifBlank { code })
            } catch (e: Exception) {
                if (rawCode.isNotBlank() && rawCode.contains('-')) {
                    _uiState.update { it.copy(isJoining = false) }
                    onSuccess(code)
                } else {
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            errorMessage = e.message ?: "Kode room tidak valid atau sudah berakhir",
                        )
                    }
                }
            }
        }
    }

    private fun normalizeRoomCode(input: String): String {
        val cleaned = input.trim()
        if (cleaned.isBlank()) return ""
        if (cleaned.contains('-')) return cleaned
        return "ZarStream-$cleaned"
    }
}