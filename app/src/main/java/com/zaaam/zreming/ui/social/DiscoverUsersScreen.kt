package com.zaaam.zreming.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight

@Composable
fun DiscoverUsersScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
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
            Text("Cari Teman", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Cari username") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = viewModel::search,
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            ) { Text("Cari") }
        }

        if (uiState.errorMessage != null) {
            Text(
                uiState.errorMessage!!,
                color = PrimaryRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed)
            }
        } else if (uiState.users.isEmpty()) {
            Text(
                "Tidak ada user ditemukan.",
                color = TextDim,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.users) { user ->
                    UserListRow(
                        user = user,
                        onClick = { onOpenProfile(user.username) },
                        trailing = {
                            OutlinedButton(
                                onClick = { viewModel.toggleFollow(user) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (user.isFollowing) TextDim else PrimaryRed,
                                ),
                            ) {
                                Text(if (user.isFollowing) "Mengikuti" else "Follow", fontSize = 12.sp)
                            }
                        },
                    )
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFF222222))
                }
            }
        }
    }
}
