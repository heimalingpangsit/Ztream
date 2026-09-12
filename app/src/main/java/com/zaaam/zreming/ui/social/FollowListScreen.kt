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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight

@Composable
fun FollowListScreen(
    viewModel: FollowListViewModel = hiltViewModel(),
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
            Text(viewModel.title, color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed)
            }
            uiState.users.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Belum ada.", color = TextDim)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.users) { user ->
                    UserListRow(
                        user = user,
                        onClick = { onOpenProfile(user.username) },
                        trailing = {},
                    )
                    HorizontalDivider(color = Color(0xFF222222))
                }
            }
        }
    }
}
