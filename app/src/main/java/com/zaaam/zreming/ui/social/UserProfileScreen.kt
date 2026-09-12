package com.zaaam.zreming.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.zreming.ui.common.RoleBadge
import com.zaaam.zreming.ui.theme.CardDark
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight

@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenFollowers: (String) -> Unit,
    onOpenFollowing: (String) -> Unit,
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
            Text("Profil", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed)
            }
            uiState.profile == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "User tidak ditemukan", color = TextDim)
            }
            else -> {
                val profile = uiState.profile!!
                Column(modifier = Modifier.padding(20.dp)) {
                    UserAvatar(username = profile.username, size = 72)
                    Spacer(Modifier.height(12.dp))
                    UsernameWithBadge(profile.username, profile.verified)
                    Spacer(Modifier.height(6.dp))
                    RoleBadge(role = profile.role, vip = profile.vip)

                    if (profile.streakCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 16.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${profile.streakCount} hari streak bareng kamu",
                                color = androidx.compose.ui.graphics.Color(0xFFFF9800),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (!profile.bio.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(profile.bio, color = TextDim, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatBox(
                            label = "Pengikut",
                            value = profile.followersCount,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenFollowers(profile.username) },
                        )
                        Spacer(Modifier.width(10.dp))
                        StatBox(
                            label = "Mengikuti",
                            value = profile.followingCount,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenFollowing(profile.username) },
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Row {
                        Button(
                            onClick = viewModel::toggleFollow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (profile.isFollowing) CardDark else PrimaryRed,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (profile.isFollowing) "Mengikuti" else "Follow")
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { onOpenChat(profile.username) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pesan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(CardDark)
            .clickableSafe(onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextDim, fontSize = 11.sp)
    }
}

private fun Modifier.clickableSafe(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
