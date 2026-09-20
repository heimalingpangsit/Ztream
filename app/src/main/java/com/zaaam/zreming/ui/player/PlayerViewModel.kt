package com.zaaam.zreming.ui.player

import android.util.Log
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
                Log.d(TAG, "resolveNobarRoom: fetching room $roomId")
                val room = authRepository.getNobarRoom(roomId)
                val myUsername = sessionManager.getUser()?.username
                val isHost = myUsername != null && myUsername.equals(room.hostUsername, ignoreCase = true)

                contentId = room.contentId
                isTv = room.isTv
                season = room.season
                episode = room.episode
                posterUrl = room.posterUrl

                Log.d(TAG, "resolveNobarRoom: room loaded, updating UI")
                _uiState.update {
                    it.copy(
                        title = room.contentTitle.ifBlank { it.title },
                        nobarIsHost = isHost,
                    )
                }
                
                Log.d(TAG, "resolveNobarRoom: resolving stream")
                resolveStream()
                
                Log.d(TAG, "resolveNobarRoom: connecting to nobar")
                connectToNobar(roomId)
                
            } catch (e: Exception) {
                Log.e(TAG, "resolveNobarRoom GAGAL: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Gagal memuat data room Nobar"
                    )
                }
            }
        }
    }

    fun resolveStream() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                Log.d(TAG, "resolveStream: fetching $contentId")
                val source = getStreamUrlUseCase(contentId, isTv, season, episode)
                Log.d(TAG, "resolveStream: SUCCESS")
                _uiState.update { it.copy(isLoading = false, streamSource = source) }
            } catch (e: Exception) {
                Log.e(TAG, "resolveStream GAGAL: ${e.message}", e)
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

        Log.d(TAG, "connectToNobar: starting for room $roomId")

        try {
            nobarSyncRepository.markOnline(roomId, myUsername)
        } catch (e: Exception) {
            Log.w(TAG, "markOnline failed: ${e.message}")
        }

        nobarJob?.cancel()
        nobarJob = null

        nobarJob = viewModelScope.launch {
            try {
                launch {
                    try {
                        Log.d(TAG, "Setting up playback observer")
                        nobarSyncRepository.observePlayback(roomId).collect { playback ->
                            if (playback.updatedBy.isNotBlank() && playback.updatedBy != myUsername) {
                                Log.d(TAG, "Playback update: ${playback.isPlaying}")
                                _remoteSyncEvents.tryEmit(playback)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "playbackObserver error: ${e.message}", e)
                    }
                }

                launch {
                    try {
                        Log.d(TAG, "Setting up chat observer")
                        nobarSyncRepository.observeChat(roomId).collect { messages ->
                            val chatLines = messages.map { msg ->
                                NobarChatLine(
                                    id = msg.id,
                                    username = msg.username,
                                    text = msg.text,
                                )
                            }
                            Log.d(TAG, "Chat update: ${messages.size} messages")
                            _uiState.update { it.copy(nobarMessages = chatLines, nobarConnected = true) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "chatObserver error: ${e.message}", e)
                    }
                }

                launch {
                    try {
                        Log.d(TAG, "Setting up presence observer")
                        nobarSyncRepository.observePresence(roomId).collect { participants ->
                            Log.d(TAG, "Presence: ${participants.size} online")
                            _uiState.update { it.copy(nobarParticipants = participants.size.coerceAtLeast(1)) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "presenceObserver error: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "connectToNobar scope error: ${e.message}", e)
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
            try {
                Log.d(TAG, "sendChat: sending")
                nobarSyncRepository.sendChat(roomIdValue, myUsername, text)
                Log.d(TAG, "sendChat: SUCCESS")
            } catch (e: Exception) {
                Log.e(TAG, "sendChat GAGAL: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Gagal kirim chat") }
            }
        }
    }

    fun broadcastLocalPlaybackAction(type: String, positionMs: Long) {
        val roomIdValue = _uiState.value.nobarRoomId ?: return
        if (!_uiState.value.nobarIsHost) return
        val myUsername = sessionManager.getUser()?.username ?: ""
        viewModelScope.launch {
            try {
                Log.d(TAG, "broadcastPlayback: $type")
                nobarSyncRepository.pushPlayback(
                    roomIdValue,
                    NobarPlaybackState(
                        isPlaying = type == "PLAY",
                        positionMs = positionMs,
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = myUsername,
                    ),
                )
                Log.d(TAG, "broadcastPlayback: SUCCESS")
            } catch (e: Exception) {
                Log.e(TAG, "broadcastPlayback GAGAL: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: cleaning up")
        nobarJob?.cancel()
        nobarJob = null
    }

    fun updatePlaybackPosition(currentPosMs: Long, totalDurationMs: Long) {
        if (totalDurationMs <= 0) return
        val currentPosSec = currentPosMs / 1000
        val totalSec = totalDurationMs / 1000

        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                Log.w(TAG, "updatePlaybackPosition failed: ${e.message}")
            }
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
                            captionError = "Subtitle tidak ditemukan"
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
                Log.e(TAG, "selectCaptionLanguage error: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoadingCaptions = false,
                        captionError = "Gagal memuat subtitle"
                    )
                }
            }
        }
    }

    fun currentCaptionText(positionMs: Long): String? {
        val cues = _uiState.value.captionCues
        return cues.firstOrNull { positionMs in it.startMs..it.endMs }?.text
    }

    companion object {
        private const val TAG = "PLAYER_NOBAR"
    }
}
