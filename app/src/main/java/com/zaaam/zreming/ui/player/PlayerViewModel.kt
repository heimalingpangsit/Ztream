package com.zaaam.zreming.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.data.nobar.NobarPlaybackState
import com.zaaam.zreming.data.nobar.NobarSyncRepository
import com.zaaam.zreming.data.remote.SubtitleClient
import com.zaaam.zreming.domain.model.ContinueWatchingItem
import com.zaaam.zreming.domain.model.StreamSource
import com.zaaam.zreming.domain.model.SubtitleCue
import com.zaaam.zreming.domain.repository.AuthRepository
import com.zaaam.zreming.domain.usecase.ContinueWatchingUseCase
import com.zaaam.zreming.domain.usecase.GetStreamUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NobarChatLine(
    val id: String,
    val username: String,
    val text: String,
    val isSystem: Boolean = false,
)

data class PlayerUiState(
    val title: String = "",
    val isLoading: Boolean = true,
    val streamSource: StreamSource? = null,
    val errorMessage: String? = null,
    val captionLanguage: String? = null,
    val captionCues: List<SubtitleCue> = emptyList(),
    val isLoadingCaptions: Boolean = false,
    val captionError: String? = null,
    val nobarRoomId: String? = null,
    val nobarIsHost: Boolean = true,
    val nobarConnected: Boolean = false,
    val nobarViaWebSocket: Boolean = false,
    val nobarMessages: List<NobarChatLine> = emptyList(),
    val nobarParticipants: Int = 1,
    val nobarChatDraft: String = "",
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getStreamUrlUseCase: GetStreamUrlUseCase,
    private val continueWatchingUseCase: ContinueWatchingUseCase,
    private val subtitleClient: SubtitleClient,
    private val nobarSyncRepository: NobarSyncRepository,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val title: String get() = _uiState.value.title
    val startPosSec: Long = savedStateHandle["startPosSec"] ?: 0L
    private var contentId: String = savedStateHandle["contentId"] ?: "0"
    private var isTv: Boolean = savedStateHandle["isTv"] ?: false
    private var season: Int = savedStateHandle["season"] ?: 1
    private var episode: Int = savedStateHandle["episode"] ?: 1
    private var posterUrl: String = savedStateHandle["posterUrl"] ?: ""
    private val roomId: String? = (savedStateHandle["roomId"] as? String)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            title = savedStateHandle["title"] ?: "Video Player",
            nobarRoomId = roomId,
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _remoteSyncEvents = MutableSharedFlow<NobarPlaybackState>(extraBufferCapacity = 4)
    val remoteSyncEvents: SharedFlow<NobarPlaybackState> = _remoteSyncEvents

    private var sourceCues: List<SubtitleCue>? = null
    private var nobarJob: Job? = null

    init {
        if (roomId != null) {
            resolveNobarRoomThenPlay(roomId)
        } else {
            resolveStream()
        }
    }

    private fun resolveNobarRoomThenPlay(roomId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val room = authRepository.getNobarRoom(roomId)
                val myUsername = sessionManager.getUser()?.username
                val isHost = myUsername != null && myUsername.equals(room.hostUsername, ignoreCase = true)

                contentId = room.contentId
                isTv = room.isTv
                season = room.season
                episode = room.episode
                posterUrl = room.posterUrl

                _uiState.update {
                    it.copy(
                        title = room.contentTitle.ifBlank { it.title },
                        nobarIsHost = isHost,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Gagal memuat data room Nobar")
                }
            }
            resolveStream()
            connectToNobar(roomId)
        }
    }

    fun resolveStream() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val source = getStreamUrlUseCase(contentId, isTv, season, episode)
                _uiState.update { it.copy(isLoading = false, streamSource = source) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Gagal memuat URL stream"
                    )
                }
            }
        }
    }

    private fun connectToNobar(roomId: String) {
        val myUsername = sessionManager.getUser()?.username ?: "Kamu"

        nobarSyncRepository.markOnline(roomId, myUsername)

        nobarJob?.cancel()
        nobarJob = viewModelScope.launch {
            launch {
                nobarSyncRepository.observePlayback(roomId).collect { playback ->
                    if (playback.updatedBy.isNotBlank() && playback.updatedBy != myUsername) {
                        _remoteSyncEvents.tryEmit(playback)
                    }
                }
            }

            launch {
                nobarSyncRepository.observeChat(roomId).collect { messages ->
                    val chatLines = messages.map { msg ->
                        NobarChatLine(
                            id = msg.id,
                            username = msg.username,
                            text = msg.text,
                        )
                    }
                    _uiState.update { it.copy(nobarMessages = chatLines, nobarConnected = true) }
                }
            }

            launch {
                nobarSyncRepository.observePresence(roomId).collect { participants ->
                    _uiState.update { it.copy(nobarParticipants = participants.size.coerceAtLeast(1)) }
                }
            }
        }
    }

    fun onNobarChatDraftChange(value: String) {
        _uiState.update { it.copy(nobarChatDraft = value) }
    }

    fun sendNobarChat() {
        val text = _uiState.value.nobarChatDraft.trim()
        if (text.isEmpty()) return
        val roomIdValue = _uiState.value.nobarRoomId ?: return
        val myUsername = sessionManager.getUser()?.username ?: "Kamu"
        _uiState.update { it.copy(nobarChatDraft = "") }
        viewModelScope.launch {
            runCatching {
                nobarSyncRepository.sendChat(roomIdValue, myUsername, text)
            }
        }
    }

    fun broadcastLocalPlaybackAction(type: String, positionMs: Long) {
        val roomIdValue = _uiState.value.nobarRoomId ?: return
        if (!_uiState.value.nobarIsHost) return
        val myUsername = sessionManager.getUser()?.username ?: ""
        viewModelScope.launch {
            runCatching {
                nobarSyncRepository.pushPlayback(
                    roomIdValue,
                    NobarPlaybackState(
                        isPlaying = type == "PLAY",
                        positionMs = positionMs,
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = myUsername,
                    ),
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nobarJob?.cancel()
    }

    fun updatePlaybackPosition(currentPosMs: Long, totalDurationMs: Long) {
        if (totalDurationMs <= 0) return
        val currentPosSec = currentPosMs / 1000
        val totalSec = totalDurationMs / 1000

        viewModelScope.launch {
            continueWatchingUseCase.save(
                ContinueWatchingItem(
                    contentId = contentId,
                    slug = if (isTv) "tv-$contentId" else "movie-$contentId",
                    title = title,
                    posterUrl = posterUrl,
                    episodeId = if (isTv) "$season-$episode" else null,
                    positionSec = currentPosSec,
                    durationSec = totalSec,
                    lastWatchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun selectCaptionLanguage(languageCode: String?) {
        if (languageCode == null) {
            _uiState.update { it.copy(captionLanguage = null, captionCues = emptyList(), captionError = null) }
            return
        }
        _uiState.update { it.copy(captionLanguage = languageCode, isLoadingCaptions = true, captionError = null) }
        viewModelScope.launch {
            try {
                val source = sourceCues ?: subtitleClient
                    .fetchSourceCues(contentId, isTv, season, episode)
                    .also { sourceCues = it }

                if (source.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingCaptions = false,
                            captionCues = emptyList(),
                            captionError = "Subtitle tidak ditemukan untuk judul ini"
                        )
                    }
                    return@launch
                }

                val finalCues = if (languageCode == "en") {
                    source
                } else {
                    val translatedTexts = subtitleClient.translateBatch(source.map { it.text }, languageCode)
                    source.mapIndexed { i, cue -> cue.copy(text = translatedTexts.getOrElse(i) { cue.text }) }
                }

                _uiState.update { it.copy(isLoadingCaptions = false, captionCues = finalCues, captionError = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCaptions = false,
                        captionError = e.localizedMessage ?: "Gagal memuat subtitle"
                    )
                }
            }
        }
    }

    fun currentCaptionText(positionMs: Long): String? {
        val cues = _uiState.value.captionCues
        return cues.firstOrNull { positionMs in it.startMs..it.endMs }?.text
    }
}