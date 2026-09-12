package com.zaaam.zreming.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.zreming.R
import com.zaaam.zreming.ui.theme.CardDark
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight
import com.zaaam.zreming.util.OwnerContact

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // ---------- Banner atas rounded, hitam-merah, pakai logo ZarStream ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A0508), DarkBg, Color(0xFF0A0A0A)),
                    ),
                ),
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_mark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(150.dp)
                    .alpha(0.9f),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, DarkBg))),
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Row {
                    Text("WELCOME", color = TextLight, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("BACK", color = PrimaryRed, fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "ZARSTREAM",
                    color = TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // ---------- Header form + badge "Tersimpan" ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Masuk ke akun kamu",
                        color = TextLight,
                        fontSize = 20.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "SECURE · ENCRYPTED · PRIVATE",
                        color = TextDim,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PrimaryRed),
                    )
                }
                if (uiState.rememberMe && uiState.username.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1B3D24)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("TERSIMPAN", color = Color(0xFF4CAF50), fontSize = 8.sp, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ---------- Username ----------
            Text("USERNAME", color = TextDim, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                placeholder = { Text("username kamu") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = zFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))

            // ---------- Password ----------
            Text("PASSWORD", color = TextDim, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = { Text("password kamu") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                        Icon(
                            if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle password",
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = zFieldColors(),
                visualTransformation = if (uiState.isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.rememberMe,
                    onCheckedChange = viewModel::onToggleRememberMe,
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryRed),
                )
                Text("INGAT SAYA", color = TextDim, fontSize = 12.sp, letterSpacing = 1.sp)
            }

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(uiState.errorMessage!!, color = PrimaryRed, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = viewModel::login,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("→  MASUK SEKARANG", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OwnerContact.whatsappUrl())))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("WHATSAPP", fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OwnerContact.instagramUrl())))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("INSTAGRAM", fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "ZARSTREAM • 2026",
                color = Color(0xFF555555),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun zFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CardDark,
    unfocusedContainerColor = CardDark,
    focusedTextColor = TextLight,
    unfocusedTextColor = TextLight,
    focusedBorderColor = PrimaryRed,
    unfocusedBorderColor = Color(0xFF3A3A3A),
    focusedLeadingIconColor = PrimaryRed,
    unfocusedLeadingIconColor = TextDim,
    focusedTrailingIconColor = PrimaryRed,
    unfocusedTrailingIconColor = TextDim,
    cursorColor = PrimaryRed,
    focusedPlaceholderColor = TextDim,
    unfocusedPlaceholderColor = Color(0xFF666666),
)

/** Dipakai juga di ProfileScreen — kartu kontak WA/IG owner. */
@Composable
fun OwnerContactCard(
    title: String,
    subtitle: String,
    onWhatsapp: () -> Unit,
    onInstagram: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onWhatsapp,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("WhatsApp", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onInstagram,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Instagram", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "WA: 0${OwnerContact.WHATSAPP_NUMBER.removePrefix("62")}   •   IG: @${OwnerContact.INSTAGRAM_HANDLE}",
            color = TextDim,
            fontSize = 10.sp,
        )
    }
}
