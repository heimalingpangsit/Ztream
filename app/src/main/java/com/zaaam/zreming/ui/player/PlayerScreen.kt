package com.zaaam.zreming.ui.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.zaaam.zreming.domain.model.AVAILABLE_CAPTION_LANGUAGES
import com.zaaam.zreming.ui.theme.AccentCyan
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Auto-hide controls state so top bar never blocks video view
    var controlsVisible by remember { mutableStateOf(true) }
    var showNobarChat by remember { mutableStateOf(false) }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Immersive fullscreen + Lock landscape during playback
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryRed)
        }
        return
    }

    val stream = uiState.streamSource

    if (stream == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Gagal memuat video", color = TextDim)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.resolveStream() }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)) {
                    Text("Coba Lagi")
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        if (stream.isWebEmbed) {
            // Web Player Sandbox (VidSrc, VidLink, SuperEmbed)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        }
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        loadUrl(stream.m3u8Url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val exoPlayer = remember(stream.m3u8Url) {
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .setDefaultRequestProperties(
                        mapOf(
                            "Referer" to stream.referer
                        )
                    )

                val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build().apply {
                        val mediaItem = MediaItem.Builder()
                            .setUri(stream.m3u8Url)
                            .setMimeType(MimeTypes.APPLICATION_M3U8)
                            .build()
                        setMediaItem(mediaItem)
                        if (viewModel.startPosSec > 0) {
                            seekTo(viewModel.startPosSec * 1000)
                        }
                        prepare()
                        playWhenReady = true
                    }
            }

            // Save playback position periodically (every 10s per PRD FR-4)
            LaunchedEffect(exoPlayer) {
                while (true) {
                    delay(10_000)
                    if (exoPlayer.isPlaying) {
                        viewModel.updatePlaybackPosition(exoPlayer.currentPosition, exoPlayer.duration)
                    }
                }
            }

            DisposableEffect(exoPlayer) {
                onDispose {
                    viewModel.updatePlaybackPosition(exoPlayer.currentPosition, exoPlayer.duration)
                    exoPlayer.release()
                }
            }

            // ---------- Nobar: broadcast aksi lokal (play/pause/seek) ke partisipan lain ----------
            var suppressLocalBroadcast by remember { mutableStateOf(false) }
            if (uiState.nobarRoomId != null) {
                DisposableEffect(exoPlayer) {
                    val listener = object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (suppressLocalBroadcast) return
                            viewModel.broadcastLocalPlaybackAction(
                                if (isPlaying) "PLAY" else "PAUSE",
                                exoPlayer.currentPosition
                            )
                        }
                        override fun onPositionDiscontinuity(
                            oldPosition: Player.PositionInfo,
                            newPosition: Player.PositionInfo,
                            reason: Int
                        ) {
                            if (suppressLocalBroadcast) return
                            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                                viewModel.broadcastLocalPlaybackAction("SEEK", newPosition.positionMs)
                            }
                        }
                    }
                    exoPlayer.addListener(listener)
                    onDispose { exoPlayer.removeListener(listener) }
                }

                // ---------- Nobar: terapin aksi dari partisipan LAIN ke player lokal ----------
                LaunchedEffect(exoPlayer) {
                    viewModel.remoteSyncEvents.collect { event ->
                        suppressLocalBroadcast = true
                        when (event.type) {
                            "PLAY" -> { exoPlayer.seekTo(event.positionMs); exoPlayer.play() }
                            "PAUSE" -> exoPlayer.pause()
                            "SEEK" -> exoPlayer.seekTo(event.positionMs)
                        }
                        delay(300) // beri jeda biar listener di atas nggak ikut broadcast balik
                        suppressLocalBroadcast = false
                    }
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = true
                        setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = (visibility == android.view.View.VISIBLE)
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay teks CC — DIISOLASI ke composable sendiri (lihat CaptionOverlay
            // di bawah) supaya polling posisi video buat sinkronisasi caption sama
            // sekali nggak bikin PlayerView di atas ikut ke-recompose. Sebelumnya
            // polling ini nempel di scope yang sama dengan PlayerView, itu yang
            // bikin video sempat "hilang" pas pause/seek/skip.
            if (!stream.isWebEmbed && uiState.captionLanguage != null) {
                CaptionOverlay(
                    exoPlayer = exoPlayer,
                    getCaptionText = { pos -> viewModel.currentCaptionText(pos) },
                )
            }
        }

        // Top bar overlay with auto-hide fade out
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = uiState.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stream.serverName,
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (!stream.isWebEmbed) {
                        CcButton(
                            selectedLanguage = uiState.captionLanguage,
                            isLoading = uiState.isLoadingCaptions,
                            onSelect = { lang -> viewModel.selectCaptionLanguage(lang) }
                        )
                    }

                    if (uiState.nobarRoomId != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (showNobarChat) PrimaryRed.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(onClick = { showNobarChat = !showNobarChat }) {
                                Icon(
                                    Icons.Filled.Groups,
                                    contentDescription = "Chat Nobar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (uiState.captionError != null) {
                    Text(
                        text = uiState.captionError!!,
                        color = PrimaryRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        if (uiState.nobarRoomId != null && showNobarChat) {
            NobarChatPanel(
                messages = uiState.nobarMessages,
                draft = uiState.nobarChatDraft,
                participants = uiState.nobarParticipants,
                onDraftChange = viewModel::onNobarChatDraftChange,
                onSend = viewModel::sendNobarChat,
                onClose = { showNobarChat = false },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun NobarChatPanel(
    messages: List<NobarChatLine>,
    draft: String,
    participants: Int,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.Black.copy(alpha = 0.75f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Nobar Chat", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$participants online", color = TextDim, fontSize = 10.sp)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(messages) { line ->
                Column(modifier = Modifier.padding(vertical = 3.dp)) {
                    if (line.isSystem) {
                        Text(line.text, color = TextDim, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text(line.username, color = PrimaryRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(line.text, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("Chat...", fontSize = 12.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                modifier = Modifier.weight(1f).height(48.dp),
            )
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                Icon(Icons.Filled.Send, contentDescription = "Kirim", tint = PrimaryRed)
            }
        }
    }
}

@Composable
private fun CaptionOverlay(
    exoPlayer: ExoPlayer,
    getCaptionText: (Long) -> String?,
) {
    var positionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(300)
            if (exoPlayer.isPlaying) {
                positionMs = exoPlayer.currentPosition
            }
        }
    }

    val captionText = getCaptionText(positionMs)
    if (!captionText.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp, start = 32.dp, end = 32.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                text = captionText,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun CcButton(
    selectedLanguage: String?,
    isLoading: Boolean,
    onSelect: (String?) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (selectedLanguage != null) PrimaryRed.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f),
            modifier = Modifier.size(38.dp)
        ) {
            IconButton(onClick = { menuExpanded = true }) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(
                        if (selectedLanguage != null) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionDisabled,
                        contentDescription = "CC",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Nonaktifkan CC") },
                onClick = { onSelect(null); menuExpanded = false }
            )
            HorizontalDivider()
            AVAILABLE_CAPTION_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.label + if (lang.code == selectedLanguage) "  ✓" else "") },
                    onClick = { onSelect(lang.code); menuExpanded = false }
                )
            }
        }
    }
}
