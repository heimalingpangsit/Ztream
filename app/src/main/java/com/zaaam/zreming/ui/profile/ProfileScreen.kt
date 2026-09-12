package com.zaaam.zreming.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.zreming.ui.auth.OwnerContactCard
import com.zaaam.zreming.ui.common.RoleBadge
import com.zaaam.zreming.ui.owner.OwnerPanelViewModel
import com.zaaam.zreming.ui.theme.CardDark
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight
import com.zaaam.zreming.util.OwnerContact
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onOpenOwnerPanel: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user
    val context = LocalContext.current

    var showUsernameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    if (user == null) {
        LaunchedEffect(Unit) { onLoggedOut() }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            Text("Profil", color = TextLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PrimaryRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        user.username.take(1).uppercase(),
                        color = PrimaryRed,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("@${user.username}", color = TextLight, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    RoleBadge(role = user.role, vip = user.vip)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---------- Kartu: Masa Aktif ----------
            ProfileCard(title = "Masa Aktif Akun") {
                if (user.role == "OWNER") {
                    Text("Aktif Selamanya", color = Color(0xFFFFC107), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    val expired = OwnerPanelViewModel.isExpired(user.expiresAt)
                    if (user.expiresAt == null) {
                        Text("Aktif Selamanya", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else if (expired) {
                        Text("Sudah Expired", color = PrimaryRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Hubungi owner untuk memperpanjang.", color = TextDim, fontSize = 11.sp)
                    } else {
                        Text(formatCountdown(user.expiresAt), color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("Aktif sampai ${OwnerPanelViewModel.formatReadable(user.expiresAt)}", color = TextDim, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---------- Kartu: Hubungi Kami ----------
            OwnerContactCard(
                title = "Hubungi Kami",
                subtitle = "Perpanjang akun / bantuan lainnya",
                onWhatsapp = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OwnerContact.whatsappUrl("Halo, saya mau tanya soal akun ZarStream saya"))))
                },
                onInstagram = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OwnerContact.instagramUrl())))
                },
            )

            Spacer(Modifier.height(14.dp))

            // ---------- Kartu: Pengaturan Akun ----------
            ProfileCard(title = "Pengaturan Akun", contentPadding = 0.dp) {
                SettingsRow(icon = Icons.Filled.Person, label = "Ganti Username", onClick = { showUsernameDialog = true })
                HorizontalDivider(color = Color(0xFF2A2A2A))
                SettingsRow(icon = Icons.Filled.Key, label = "Ganti Password", onClick = { showPasswordDialog = true })
                if (user.role == "OWNER" || user.role == "ADMIN") {
                    HorizontalDivider(color = Color(0xFF2A2A2A))
                    SettingsRow(icon = Icons.Filled.AdminPanelSettings, label = "Owner Panel", onClick = onOpenOwnerPanel)
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
                SettingsRow(icon = Icons.Filled.Logout, label = "Keluar", tint = PrimaryRed, onClick = {
                    viewModel.logout()
                    onLoggedOut()
                })
            }

            if (uiState.message != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    uiState.message!!,
                    color = if (uiState.isError) PrimaryRed else Color(0xFF4CAF50),
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showUsernameDialog) {
        ChangeUsernameDialog(
            currentUsername = user.username,
            isSaving = uiState.isSaving,
            onDismiss = { showUsernameDialog = false; viewModel.clearMessage() },
            onConfirm = { newUsername ->
                viewModel.changeUsername(newUsername) { showUsernameDialog = false }
            },
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            isSaving = uiState.isSaving,
            onDismiss = { showPasswordDialog = false; viewModel.clearMessage() },
            onConfirm = { old, new ->
                viewModel.changePassword(old, new) { showPasswordDialog = false }
            },
        )
    }
}

@Composable
private fun ProfileCard(
    title: String,
    contentPadding: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark),
    ) {
        Text(
            title,
            color = TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 6.dp),
        )
        Column(modifier = Modifier.padding(bottom = contentPadding, start = contentPadding, end = contentPadding)) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = TextLight,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = tint, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextDim, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ChangeUsernameDialog(
    currentUsername: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(currentUsername) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Ganti Username", color = TextLight) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Username baru") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value) },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            ) { Text(if (isSaving) "Menyimpan..." else "Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun ChangePasswordDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (old: String, new: String) -> Unit,
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Ganti Password", color = TextLight) },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Password lama") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Password baru (min 8 karakter)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(oldPassword, newPassword) },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            ) { Text(if (isSaving) "Menyimpan..." else "Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

/** "3 hari 5 jam lagi" / "8 jam lagi" / "Kurang dari 1 jam lagi" */
private fun formatCountdown(expiresAtIso: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val target = sdf.parse(expiresAtIso) ?: return expiresAtIso
        val diffMs = target.time - Calendar.getInstance().timeInMillis
        if (diffMs <= 0) return "Sudah Expired"
        val days = diffMs / 86_400_000L
        val hours = (diffMs % 86_400_000L) / 3_600_000L
        when {
            days > 0 -> "$days hari $hours jam lagi"
            hours > 0 -> "$hours jam lagi"
            else -> "Kurang dari 1 jam lagi"
        }
    } catch (_: Exception) {
        expiresAtIso
    }
}
