package com.zaaam.zreming.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.zreming.data.model.ConversationSummaryDto
import com.zaaam.zreming.ui.social.UserAvatar
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onOpenChat: (String) -> Unit,
    onOpenDiscover: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Pesan",
                color = TextLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenDiscover) {
                Icon(Icons.Filled.PersonSearch, contentDescription = "Cari teman", tint = PrimaryRed)
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed)
            }
            uiState.conversations.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Belum ada obrolan.", color = TextDim)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenDiscover) { Text("Cari teman buat diajak chat", color = PrimaryRed) }
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.conversations) { convo ->
                    ConversationRow(convo = convo, onClick = { onOpenChat(convo.withUsername) })
                    HorizontalDivider(color = Color(0xFF222222))
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(convo: ConversationSummaryDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(username = convo.withUsername)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("@${convo.withUsername}", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (convo.streakCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text("🔥${convo.streakCount}", color = Color(0xFFFF9800), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                convo.lastMessage?.text ?: "",
                color = TextDim,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (convo.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryRed)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text("${convo.unreadCount}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
