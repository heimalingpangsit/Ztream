package com.zaaam.zreming.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaaam.zreming.domain.model.ContentType
import com.zaaam.zreming.ui.theme.*

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onPlayClick: (String, String, Boolean, Int, Int, Long, String) -> Unit,
    onNobarClick: (String, String, Boolean, Int, Int, String) -> Unit = { _, _, _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryRed)
        }
        return
    }

    val detail = uiState.detail
    if (detail == null) {
        Box(modifier = Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.errorMessage ?: "Detail tidak ditemukan", color = TextDim)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.loadDetail() }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)) {
                    Text("Coba Lagi")
                }
            }
        }
        return
    }

    val isSeries = detail.type == ContentType.SERIES

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Backdrop & Back Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                AsyncImage(
                    model = detail.backdropUrl.ifEmpty { detail.posterUrl },
                    contentDescription = detail.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    DarkBg
                                )
                            )
                        )
                )
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
            }
        }

        // Details Content
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = detail.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("★ ${detail.rating}", color = GoldRating, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(detail.releaseDate.take(4), color = TextDim, fontSize = 13.sp)
                    if (detail.runtime > 0) {
                        Text("${detail.runtime} menit", color = TextDim, fontSize = 13.sp)
                    }
                    Surface(
                        color = CardDark,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            if (isSeries) "Series" else "Movie",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Play and Watchlist buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onPlayClick(detail.title, detail.id, isSeries, 1, 1, 0L, detail.posterUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Putar", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.toggleWatchlist() },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (uiState.isWatchlist) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tersimpan")
                        } else {
                            Text("＋ Simpan")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        onNobarClick(detail.id, detail.title, isSeries, 1, 1, detail.posterUrl)
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mulai Nobar", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = detail.overview.ifEmpty { "Tidak ada sinopsis tersedia." },
                    color = TextDim,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Series Episode List
        if (isSeries && detail.episodes.isNotEmpty()) {
            item {
                Text(
                    text = "Daftar Episode Season 1",
                    color = TextLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(detail.episodes) { ep ->
                EpisodeItemRow(
                    episode = ep,
                    onClick = {
                        onPlayClick("${detail.title} - E${ep.episodeNumber}", detail.id, true, ep.seasonNumber, ep.episodeNumber, 0L, ep.stillUrl.ifEmpty { detail.posterUrl })
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun EpisodeItemRow(
    episode: com.zaaam.zreming.domain.model.Episode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(CardDark, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (episode.stillUrl.isNotEmpty()) {
                AsyncImage(model = episode.stillUrl, contentDescription = null, contentScale = ContentScale.Crop)
            } else {
                Text("E${episode.episodeNumber}", color = TextLight, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.title}",
                color = TextLight,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            if (episode.runtime > 0) {
                Text("${episode.runtime} menit", color = TextDim, fontSize = 11.sp)
            }
        }
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentCyan)
    }
}
