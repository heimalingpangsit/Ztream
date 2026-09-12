package com.zaaam.zreming.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.data.remote.NobarEvent
import com.zaaam.zreming.data.remote.NobarSocketClient
import com.zaaam.zreming.data.remote.SubtitleClient
import com.zaaam.zreming.domain.model.ContinueWatchingItem
import com.zaaam.zreming.domain.model.StreamSource
import com.zaaam.zreming.domain.model.SubtitleCue
import com.zaaam.zreming.domain.usecase.ContinueWatchingUseCase
import com.zaaam.zreming.domain.usecase.GetStreamUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NobarChatLine(val username: String, val text: String, val isSystem: Boolean = false)

data class PlayerUiState(
    val title: String = "",
    val isLoading: Boolean = true,
    val streamSource: StreamSource? = null,
    val errorMessage: String? = null,
    val captionLanguage: String? = null, // null = CC off
    val captionCues: List<SubtitleCue> = emptyList(),
    val isLoadingCaptions: Boolean = false,
    val captionError: String? = null,
    // ---- Nobar (Watch Party) ----
    val nobarRoomId: String? = null,
    val nobarConnected: Boolean = false,
    val nobarMessages: List<NobarChatLine> = emptyList(),
    val nobarParticipants: Int = 1,
    val nobarChatDraft: String = "",
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getStreamUrlUseCase: GetStreamUrlUseCase,
    private val continueWatchingUseCase: ContinueWatchingUseCase,
    private val subtitleClient: SubtitleClient,
    private val nobarSocketClient: NobarSocketClient,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val title: String = savedStateHandle["title"] ?: "Video Player"
    val startPosSec: Long = savedStateHandle["startPosSec"] ?: 0L
    private val contentId: String = savedStateHandle["contentId"] ?: "0"
    private val isTv: Boolean = savedStateHandle["isTv"] ?: false
    private val season: Int = savedStateHandle["season"] ?: 1
    private val episode: Int = savedStateHandle["episode"] ?: 1
    private val posterUrl: String = savedStateHandle["posterUrl"] ?: ""
    private val roomId: String? = (savedStateHandle["roomId"] as? String)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(PlayerUiState(title = title, nobarRoomId = roomId))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Event sinkronisasi dari partisipan LAIN — PlayerScreen yang subscribe
    // ini buat gerakin ExoPlayer (play/pause/seek). Dipisah dari uiState
    // supaya nggak ke-apply berkali-kali tiap recomposition.
    private val _remoteSyncEvents = MutableSharedFlow<NobarEvent.Sync>(extraBufferCapacity = 4)
    val remoteSyncEvents: SharedFlow<NobarEvent.Sync> = _remoteSyncEvents

    // cache subtitle sumber (bahasa asli/Inggris) biar ganti-ganti bahasa CC
    // nggak perlu fetch ulang ke Wyzie tiap kali, cukup terjemahkan ulang
    private var sourceCues: List<SubtitleCue>? = null

    init {
        resolveStream()
        if (roomId != null) connectToNobar(roomId)
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
        viewModelScope.launch {
            nobarSocketClient.connect(roomId, myUsername).collect { event ->
                when (event) {
                    is NobarEvent.System -> {
                        _uiState.update {
                            it.copy(
                                nobarConnected = true,
                                nobarParticipants = event.participants,
                                nobarMessages = it.nobarMessages + NobarChatLine("Sistem", event.text, isSystem = true),
                            )
                        }
                    }
                    is NobarEvent.Chat -> {
                        _uiState.update {
                            it.copy(nobarMessages = it.nobarMessages + NobarChatLine(event.username, event.text))
                        }
                    }
                    is NobarEvent.Sync -> {
                        _remoteSyncEvents.tryEmit(event)
                    }
                    is NobarEvent.ConnectionClosed -> {
                        _uiState.update { it.copy(nobarConnected = false) }
                    }
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
        nobarSocketClient.sendChat(text)
        val myUsername = sessionManager.getUser()?.username ?: "Kamu"
        _uiState.update {
            it.copy(
                nobarChatDraft = "",
                nobarMessages = it.nobarMessages + NobarChatLine(myUsername, text),
            )
        }
    }

    /** Dipanggil PlayerScreen tiap user LOKAL play/pause/seek manual (bukan
     *  hasil dari remote sync), supaya event itu di-broadcast ke partisipan lain. */
    fun broadcastLocalPlaybackAction(type: String, positionMs: Long) {
        if (roomId == null) return
        nobarSocketClient.sendSync(type, positionMs)
    }

    override fun onCleared() {
        super.onCleared()
        if (roomId != null) nobarSocketClient.disconnect()
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

    /** Panggil dengan null untuk matiin CC. */
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
