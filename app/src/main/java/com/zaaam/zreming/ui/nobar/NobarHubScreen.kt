package com.zaaam.zreming.ui.nobar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.zreming.ui.theme.CardDark
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight
import kotlinx.coroutines.delay

@Composable
fun NobarHubScreen(
    viewModel: NobarHubViewModel = hiltViewModel(),
    onOpenRoom: (roomId: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            viewModel.loadInvites()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Join Nobar", color = TextLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Masukin kode room dari temenmu, atau gabung langsung dari undangan di bawah.",
            color = TextDim,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.trim().take(48) },
                    label = { Text("Kode Room") },
                    placeholder = { Text("ZarStream-xxxxxx", color = TextDim) },
                    singleLine = true,
                    enabled = !uiState.isJoining,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Go,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.join(code) { roomId -> onOpenRoom(roomId) } },
                    enabled = code.length >= 4 && !uiState.isJoining,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = TextLight,
                        )
                    } else {
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gabung Sekarang")
                    }
                }
                if (uiState.errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(uiState.errorMessage!!, color = PrimaryRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = TextLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Undangan Masuk",
                color = TextLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { viewModel.loadInvites() },
                enabled = !uiState.isLoadingInvites,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Muat ulang undangan",
                    tint = if (uiState.isLoadingInvites) TextDim else PrimaryRed,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Undangan dari host muncul otomatis di sini, dan pesannya juga masuk ke tab Chat.",
            color = TextDim,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))

        when {
            uiState.isLoadingInvites && uiState.invites.isEmpty() -> {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryRed, modifier = Modifier.size(28.dp))
                }
            }

            uiState.invites.isEmpty() -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardDark,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.MailOutline,
                            contentDescription = null,
                            tint = TextDim,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Belum ada undangan nobar", color = TextDim, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Kalau temenmu udah ngundang tapi belum kelihatan, tekan tombol muat ulang.",
                            color = TextDim,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(uiState.invites, key = { index, item -> "$index-${item.id}-${item.roomCode}" }) { _, invite ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CardDark,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        invite.title.ifBlank { "Undangan Nobar" },
                                        color = TextLight,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        if (invite.fromUsername.isNotBlank()) {
                                            "Dari @${invite.fromUsername}"
                                        } else {
                                            invite.body
                                        },
                                        color = TextDim,
                                        fontSize = 12.sp,
                                    )
                                    if (invite.canJoin) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(invite.roomCode, color = TextDim, fontSize = 10.sp)
                                    } else {
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            "Kode room gak kebaca — minta kodenya ke host.",
                                            color = PrimaryRed,
                                            fontSize = 10.sp,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.join(invite.roomCode) { roomId -> onOpenRoom(roomId) }
                                    },
                                    enabled = invite.canJoin && !uiState.isJoining,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text("Gabung", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}