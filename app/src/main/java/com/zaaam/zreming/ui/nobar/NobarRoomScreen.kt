package com.zaaam.zreming.ui.nobar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight

@Composable
fun NobarRoomScreen(
    viewModel: NobarRoomViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenRoom: (roomId: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = TextLight,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Gabung Nobar",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Masukkan kode room yang kamu terima dari temanmu.",
            color = TextDim,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { code = it.trim().take(40) },
            label = { Text("Kode Room") },
            placeholder = { Text("ZaaamStream-xxxxxx", color = TextDim) },
            singleLine = true,
            enabled = !uiState.isJoining,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Go,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.join(code) { roomId -> onOpenRoom(roomId) }
            },
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
                Icon(
                    Icons.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Gabung Sekarang")
            }
        }

        if (uiState.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(uiState.errorMessage!!, color = PrimaryRed, fontSize = 12.sp)
        }
    }
}