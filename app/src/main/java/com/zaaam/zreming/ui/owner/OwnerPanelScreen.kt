package com.zaaam.zreming.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.zreming.data.model.UserDto
import com.zaaam.zreming.ui.common.RoleBadge
import com.zaaam.zreming.ui.theme.CardDark
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight

@Composable
fun OwnerPanelScreen(
    viewModel: OwnerPanelViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var expiryTargetUser by remember { mutableStateOf<UserDto?>(null) }
    var showAddDramaDialog by remember { mutableStateOf(false) }
    var usernameTargetUser by remember { mutableStateOf<UserDto?>(null) }
    var passwordTargetUser by remember { mutableStateOf<UserDto?>(null) }
    var deleteTargetUser by remember { mutableStateOf<UserDto?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = TextLight)
            }
            Text(
                "Owner Panel",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { showAddDramaDialog = true }) {
                Text("+ Film Baru", fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = viewModel::openCreateDialog,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            ) { Text("+ User Baru", fontSize = 12.sp) }
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
            Button(onClick = viewModel::search, enabled = !uiState.isLoading) { Text("Cari") }
        }

        if (uiState.message != null) {
            Text(
                uiState.message!!,
                color = if (uiState.isError) PrimaryRed else Color(0xFF4CAF50),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.users) { user ->
                    UserRow(
                        user = user,
                        onToggleVip = { vip -> viewModel.setVip(user.id, vip) },
                        onEditExpiry = { expiryTargetUser = user },
                        onToggleVerified = { verified -> viewModel.setVerified(user.id, verified) },
                        onResetHwid = { viewModel.resetHwid(user.id, user.username) },
                        onChangeUsername = { usernameTargetUser = user },
                        onChangePassword = { passwordTargetUser = user },
                        onDeleteUser = { deleteTargetUser = user },
                    )
                    HorizontalDivider(color = Color(0xFF222222))
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateUserDialog(
            onDismiss = viewModel::dismissCreateDialog,
            onCreate = { username, password, role, vip, expiryDays ->
                viewModel.createUser(username, password, role, vip, expiryDays)
            },
        )
    }

    if (showAddDramaDialog) {
        AddDramaDialog(
            isSaving = uiState.isSavingDrama,
            message = uiState.dramaMessage,
            isError = uiState.dramaIsError,
            onDismiss = { showAddDramaDialog = false; viewModel.clearDramaMessage() },
            onSave = { title, description, poster, genresCsv, year, totalEpisodes, streamUrl ->
                viewModel.saveDrama(title, description, poster, genresCsv, year, totalEpisodes, streamUrl) {
                    showAddDramaDialog = false
                }
            },
        )
    }

    expiryTargetUser?.let { user ->
        ExpiryDialog(
            user = user,
            onDismiss = { expiryTargetUser = null },
            onSetDays = { days ->
                viewModel.setExpiryDays(user.id, days)
                expiryTargetUser = null
            },
        )
    }

    usernameTargetUser?.let { user ->
        SimpleTextInputDialog(
            title = "Ganti Username @${user.username}",
            label = "Username baru",
            initialValue = user.username,
            confirmLabel = "Simpan",
            onDismiss = { usernameTargetUser = null },
            onConfirm = { newUsername ->
                viewModel.changeUsername(user.id, newUsername) { usernameTargetUser = null }
            },
        )
    }

    passwordTargetUser?.let { user ->
        SimpleTextInputDialog(
            title = "Ganti Password @${user.username}",
            label = "Password baru (min 8 karakter)",
            initialValue = "",
            confirmLabel = "Simpan",
            isPassword = true,
            onDismiss = { passwordTargetUser = null },
            onConfirm = { newPassword ->
                viewModel.changePassword(user.id, newPassword) { passwordTargetUser = null }
            },
        )
    }

    deleteTargetUser?.let { user ->
        AlertDialog(
            onDismissRequest = { deleteTargetUser = null },
            containerColor = CardDark,
            title = { Text("Hapus akun @${user.username}?", color = TextLight, fontSize = 16.sp) },
            text = {
                Text(
                    "Akun ini akan dihapus PERMANEN dari database. Tindakan ini tidak bisa dibatalkan.",
                    color = TextDim,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteUser(user.id) { deleteTargetUser = null } },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                ) { Text("Ya, Hapus Permanen") }
            },
            dismissButton = { TextButton(onClick = { deleteTargetUser = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun UserRow(
    user: UserDto,
    onToggleVip: (Boolean) -> Unit,
    onEditExpiry: () -> Unit,
    onToggleVerified: (Boolean) -> Unit,
    onResetHwid: () -> Unit,
    onChangeUsername: () -> Unit,
    onChangePassword: () -> Unit,
    onDeleteUser: () -> Unit,
) {
    val expired = OwnerPanelViewModel.isExpired(user.expiresAt)
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("@${user.username}", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (user.verified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF3897F0),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            RoleBadge(role = user.role, vip = user.vip)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (expired) "Expired: ${OwnerPanelViewModel.formatReadable(user.expiresAt)} (lewat)" else "Aktif sampai: ${OwnerPanelViewModel.formatReadable(user.expiresAt)}",
                color = if (expired) PrimaryRed else TextDim,
                fontSize = 11.sp,
            )
            Text(
                text = "Perangkat: ${user.deviceCount}/${user.maxDevices}",
                color = TextDim,
                fontSize = 10.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (user.role == "USER") {
                    TextButton(onClick = { onToggleVip(!user.vip) }) {
                        Text(if (user.vip) "Cabut VIP+" else "Jadikan VIP+", fontSize = 11.sp)
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Aksi lainnya", tint = TextDim)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Atur Expiry") },
                            onClick = { menuExpanded = false; onEditExpiry() },
                        )
                        DropdownMenuItem(
                            text = { Text(if (user.verified) "Cabut Centang Biru" else "Kasih Centang Biru") },
                            onClick = { menuExpanded = false; onToggleVerified(!user.verified) },
                        )
                        DropdownMenuItem(
                            text = { Text("Reset HWID") },
                            onClick = { menuExpanded = false; onResetHwid() },
                        )
                        DropdownMenuItem(
                            text = { Text("Ganti Username") },
                            onClick = { menuExpanded = false; onChangeUsername() },
                        )
                        DropdownMenuItem(
                            text = { Text("Ganti Password") },
                            onClick = { menuExpanded = false; onChangePassword() },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Hapus Akun", color = PrimaryRed) },
                            onClick = { menuExpanded = false; onDeleteUser() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (username: String, password: String, role: String, vip: Boolean, expiryDays: Int?) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("USER") }
    var vip by remember { mutableStateOf(false) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var expiryDays by remember { mutableStateOf<Int?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Buat User Baru", color = TextLight) },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; localError = null },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; localError = null },
                    label = { Text("Password (min 8 karakter)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                Box {
                    OutlinedButton(onClick = { roleMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Role: $role")
                    }
                    DropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("USER") }, onClick = { role = "USER"; roleMenuExpanded = false })
                        DropdownMenuItem(text = { Text("ADMIN") }, onClick = { role = "ADMIN"; roleMenuExpanded = false })
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Masa aktif akun", color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                ExpiryChoiceRow(selected = expiryDays, onSelect = { expiryDays = it })

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vip, onCheckedChange = { vip = it })
                    Text("Aktifkan VIP+ sejak awal", color = TextLight, fontSize = 12.sp)
                }

                if (localError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(localError!!, color = PrimaryRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isBlank() || password.length < 8) {
                        localError = "Username wajib diisi & password minimal 8 karakter"
                        return@Button
                    }
                    onCreate(username.trim(), password, role, vip, expiryDays)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            ) { Text("Buat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun ExpiryDialog(user: UserDto, onDismiss: () -> Unit, onSetDays: (Int?) -> Unit) {
    var selected by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Atur Masa Aktif @${user.username}", color = TextLight, fontSize = 15.sp) },
        text = {
            Column {
                Text(
                    "Sekarang: ${OwnerPanelViewModel.formatReadable(user.expiresAt)}",
                    color = TextDim,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                ExpiryChoiceRow(selected = selected, onSelect = { selected = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSetDays(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun ExpiryChoiceRow(selected: Int?, onSelect: (Int?) -> Unit) {
    val options = listOf(
        "7 hari" to 7,
        "30 hari" to 30,
        "90 hari" to 90,
        "365 hari" to 365,
        "Selamanya" to null,
    )
    Column {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (label, days) ->
                    val isSelected = selected == days
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(days) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryRed,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AddDramaDialog(
    isSaving: Boolean,
    message: String?,
    isError: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        poster: String,
        genresCsv: String,
        year: String,
        totalEpisodes: String,
        streamUrl: String,
    ) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var poster by remember { mutableStateOf("") }
    var genresCsv by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var totalEpisodes by remember { mutableStateOf("1") }
    var streamUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("Tambah Film / Drama Manual", color = TextLight, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Sinopsis") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = poster,
                    onValueChange = { poster = it },
                    label = { Text("Poster URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = genresCsv,
                    onValueChange = { genresCsv = it },
                    label = { Text("Genre (pisah koma: Action, Drama)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter { c -> c.isDigit() } },
                        label = { Text("Tahun") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = totalEpisodes,
                        onValueChange = { totalEpisodes = it.filter { c -> c.isDigit() } },
                        label = { Text("Jumlah Episode") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    label = { Text("Link Streaming *") },
                    placeholder = { Text("https://tv10.lk21official.cc/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        message,
                        color = if (isError) PrimaryRed else Color(0xFF4CAF50),
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description, poster, genresCsv, year, totalEpisodes, streamUrl) },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            ) { Text(if (isSaving) "Menyimpan..." else "Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}


@Composable
private fun SimpleTextInputDialog(
    title: String,
    label: String,
    initialValue: String,
    confirmLabel: String,
    isPassword: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text(title, color = TextLight, fontSize = 15.sp) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
