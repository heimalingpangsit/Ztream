package com.zaaam.zreming.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaaam.zreming.domain.model.ContentItem
import com.zaaam.zreming.domain.model.ContentSection
import com.zaaam.zreming.domain.model.ContinueWatchingItem
import com.zaaam.zreming.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onContentClick: (String) -> Unit,
    onPlayClick: (String, String, Boolean, Int, Int, Long, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(DarkBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryRed)
        }
        return
    }

    if (uiState.errorMessage != null && uiState.sections.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(DarkBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = uiState.errorMessage ?: "Terjadi kesalahan", color = TextDim)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.loadHome() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Coba Lagi")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Hero Banner Slider
        if (uiState.heroItems.isNotEmpty()) {
            item {
                HeroBannerSlider(
                    items = uiState.heroItems,
                    onPlayClick = { hero ->
                        val isTv = hero.type == com.zaaam.zreming.domain.model.ContentType.SERIES
                        onPlayClick(hero.title, hero.id, isTv, 1, 1, 0L, hero.posterUrl)
                    },
                    onDetailClick = { hero -> onContentClick(hero.slug) }
                )
            }
        }

        // Continue Watching
        if (uiState.continueWatching.isNotEmpty()) {
            item {
                SectionHeader(title = "Lanjutkan Menonton")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.continueWatching,
                        key = { it.contentId }
                    ) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = {
                                val isTv = item.episodeId != null
                                val parts = item.episodeId?.split("-")
                                val s = parts?.getOrNull(0)?.toIntOrNull() ?: 1
                                val e = parts?.getOrNull(1)?.toIntOrNull() ?: 1
                                onPlayClick(item.title, item.contentId, isTv, s, e, item.positionSec, item.posterUrl)
                            }
                        )
                    }
                }
            }
        }

        // Horizontal Sections
        items(
            items = uiState.sections,
            key = { it.title }
        ) { section ->
            SectionHeader(title = section.title)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = section.items,
                    key = { it.id }
                ) { item ->
                    MoviePosterCard(
                        item = item,
                        onClick = { onContentClick(item.slug) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun HeroBannerSlider(
    items: List<ContentItem>,
    onPlayClick: (ContentItem) -> Unit,
    onDetailClick: (ContentItem) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    // auto-advance tiap 5 detik, berhenti sebentar kalau user lagi swipe manual
    LaunchedEffect(items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val item = items[page]
            HeroBannerContent(
                item = item,
                rank = page + 1,
                onPlayClick = { onPlayClick(item) },
                onDetailClick = { onDetailClick(item) }
            )
        }

        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (selected) 20.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) PrimaryRed else Color.White.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

@Composable
fun HeroBannerContent(
    item: ContentItem,
    rank: Int,
    onPlayClick: () -> Unit,
    onDetailClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(440.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onDetailClick() }
    ) {
        AsyncImage(
            model = item.backdropUrl.ifEmpty { item.posterUrl },
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DarkBg.copy(alpha = 0.4f),
                            DarkBg.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "★ ${item.rating}",
                        color = GoldRating,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• TRENDING #$rank",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                color = TextLight,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.overview,
                color = TextDim,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Putar Sekarang", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDetailClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                ) {
                    Text("Detail")
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextLight,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp)
    )
}

@Composable
fun MoviePosterCard(
    item: ContentItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(128.dp)
                    .height(185.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDark),
                contentScale = ContentScale.Crop
            )
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
            ) {
                Text(
                    text = "★ ${item.rating}",
                    color = GoldRating,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            color = TextLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${item.releaseDate.take(4)} • ${if (item.type == com.zaaam.zreming.domain.model.ContentType.SERIES) "Series" else "Film"}",
            color = TextDim,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit
) {
    val progress = if (item.durationSec > 0) item.positionSec.toFloat() / item.durationSec.toFloat() else 0.5f

    Column(
        modifier = Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(105.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CardDark)
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp)
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter),
                color = PrimaryRed,
                trackColor = Color.DarkGray
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            color = TextLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${(progress * 100).toInt()}% ditonton",
            color = AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
