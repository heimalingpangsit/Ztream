package com.zaaam.zreming.ui.nobar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.zreming.ui.social.UserAvatar
import com.zaaam.zreming.ui.social.UsernameWithBadge
import com.zaaam.zreming.ui.theme.CardDark
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight

@Composable
fun NobarInviteScreen(
    viewModel: NobarInviteViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenRoom: (roomId: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = TextLight)
            }
            Text("Mulai Nobar", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        when {
            uiState.isCreatingRoom -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed)
            }
            uiState.room == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Gagal membuat room", color = TextDim, textAlign = TextAlign.Center)
            }
            else -> {
                val room = uiState.room!!
                Column(modifier = Modifier.padding(horizontal = 20.dp).weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardDark)
                            .padding(16.dp),
                    ) {
                        Column {
                            Text("Room: ${room.contentTitle}", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Kode room: ${room.id}", color = TextDim, fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = TextDim, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Undang teman (mutual follow)", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))

                    if (uiState.errorMessage != null) {
                        Text(uiState.errorMessage!!, color = PrimaryRed, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (uiState.friends.isEmpty()) {
                        Text(
                            "Belum ada teman mutual buat diundang. Follow-follow-an dulu sama temanmu.",
                            color = TextDim,
                            fontSize = 12.sp,
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.friends) { friend ->
                                val invited = uiState.invitedUsernames.contains(friend.username)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    UserAvatar(username = friend.username, size = 38)
                                    Spacer(Modifier.width(10.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        UsernameWithBadge(friend.username, friend.verified)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.invite(friend.username) },
                                        enabled = !invited,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (invited) TextDim else PrimaryRed,
                                        ),
                                    ) {
                                        Text(if (invited) "Terkirim" else "Undang", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { onOpenRoom(room.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Room & Mulai Nonton")
                }
            }
        }
    }
}
