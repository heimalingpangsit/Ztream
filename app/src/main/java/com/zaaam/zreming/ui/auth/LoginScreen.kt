package com.zaaam.zreming.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.zreming.R
import com.zaaam.zreming.ui.theme.CardDark
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight
import com.zaaam.zreming.util.OwnerContact

// Palet khusus layar login — disamakan dengan login.html (nuansa merah pekat,
// bukan abu-abu gelap generik milik tema utama app).
private val BloodBg = Color(0xFF0A0000)
private val BloodCard = Color(0xFF110303)
private val BloodBorder = Color(0x59B41414)
private val BloodBorderSoft = Color(0x33B41414)
private val BloodAccent = Color(0xFFCC1111)
private val BloodAccent2 = Color(0xFFEE2222)
private val BloodText = Color(0xFFFFDDDD)
private val BloodDim = Color(0xFFAA5555)
private val BloodFaint = Color(0xFF661111)
private val BloodFieldBg = Color(0x14CC1111)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onOpenBeliAccount: () -> Unit = {},
    onOpenPulihkanAccount: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onLoginSuccess()
    }

    Box(modifier = Modifier.fillMaxSize().background(BloodBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))

            Column(modifier = Modifier.widthIn(max = 440.dp)) {

                // ================= BANNER =================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .background(BloodBg)
                        .border(
                            BorderStroke(1.dp, BloodBorder),
                            RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                        ),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_app),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xB30A0000),
                                        Color(0xCC0A0000),
                                        Color(0xF20A0000),
                                    ),
                                ),
                            ),
                    )
                    // garis merah tipis di atas banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0x99CC1414),
                                        Color(0xCCFF1E1E),
                                        Color(0x99CC1414),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    ) {
                        Row {
                            Text(
                                "WELCOME",
                                color = BloodText,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                            )
                            Text(
                                "BACK",
                                color = BloodAccent,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "CREATED BY JEPIN666X",
                            color = BloodDim,
                            fontSize = 9.sp,
                            letterSpacing = 3.sp,
                        )
                    }
                }

                // ================= CARD =================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                        .background(BloodCard)
                        .border(
                            BorderStroke(1.dp, BloodBorder),
                            RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                        )
                        .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 22.dp),
                ) {
                    // ---------- Header + tombol simpan akun ----------
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Masuk ke akun kamu",
                                color = BloodText,
                                fontSize = 20.sp,
                                fontStyle = FontStyle.Italic,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "SECURE · ENCRYPTED · PRIVATE",
                                color = BloodDim,
                                fontSize = 9.sp,
                                letterSpacing = 2.sp,
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(2.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(BloodAccent, Color.Transparent),
                                        ),
                                    ),
                            )
                        }
                        SaveAccountBadge(
                            saved = uiState.accountSaved,
                            onClick = viewModel::toggleSaveAccount,
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    // ---------- Username ----------
                    FieldLabel("USERNAME")
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChange,
                        placeholder = { Text("username kamu", fontSize = 12.sp, fontStyle = FontStyle.Italic) },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = bloodFieldColors(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(13.dp))

                    // ---------- Password ----------
                    FieldLabel("PASSWORD")
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = { Text("password kamu", fontSize = 12.sp, fontStyle = FontStyle.Italic) },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                                Icon(
                                    if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Lihat password",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = bloodFieldColors(),
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))

                    // ---------- Ingat saya ----------
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.onToggleRememberMe(!uiState.rememberMe) },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (uiState.rememberMe) Color(0x26CC1111) else Color(0x0DCC1111))
                                .border(
                                    BorderStroke(1.5.dp, if (uiState.rememberMe) Color(0x80CC1111) else BloodBorderSoft),
                                    RoundedCornerShape(4.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uiState.rememberMe) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = BloodAccent2,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("INGAT SAYA", color = BloodDim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }

                    // ---------- Pesan error / info ----------
                    if (uiState.errorMessage != null || uiState.infoMessage != null) {
                        Spacer(Modifier.height(14.dp))
                        NoticeCard(
                            title = if (uiState.errorMessage != null) "GAGAL" else "INFO",
                            message = uiState.errorMessage ?: uiState.infoMessage.orEmpty(),
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // ---------- Tombol masuk ----------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0x33CC1111), Color(0x14CC1111)),
                                ),
                            )
                            .border(BorderStroke(1.dp, Color(0x66CC1111)), RoundedCornerShape(13.dp))
                            .clickable(enabled = !uiState.isLoading) { viewModel.login() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = BloodAccent2,
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    tint = BloodAccent2,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    "MASUK SEKARANG",
                                    color = BloodAccent2,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.5.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ================= BELI / PULIHKAN / KONTAK =================
                BloodOutlineButton(
                    label = "BELI ACCOUNT",
                    icon = { Icon(Icons.Filled.CreditCard, contentDescription = null, tint = BloodAccent2, modifier = Modifier.size(15.dp)) },
                    onClick = onOpenBeliAccount,
                )
                Spacer(Modifier.height(10.dp))
                BloodOutlineButton(
                    label = "PULIHKAN ACCOUNT",
                    icon = { Icon(Icons.Filled.Restore, contentDescription = null, tint = BloodAccent2, modifier = Modifier.size(15.dp)) },
                    onClick = onOpenPulihkanAccount,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BloodOutlineButton(
                        label = "WHATSAPP",
                        icon = { Icon(Icons.Filled.Chat, contentDescription = null, tint = BloodDim, modifier = Modifier.size(15.dp)) },
                        dim = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(OwnerContact.whatsappUrl())),
                            )
                        },
                    )
                    BloodOutlineButton(
                        label = "INSTAGRAM",
                        icon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = BloodDim, modifier = Modifier.size(15.dp)) },
                        dim = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(OwnerContact.instagramUrl())),
                            )
                        },
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FooterText("ZARSTREAM")
                    FooterDot()
                    FooterText("2026")
                    FooterDot()
                    FooterText("JEPIN666X")
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = BloodDim,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SaveAccountBadge(saved: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (saved) Color(0x26CC1111) else Color(0x14CC1111))
                .border(
                    BorderStroke(1.dp, if (saved) Color(0x99CC1111) else Color(0x4DCC1111)),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Save,
                contentDescription = "Simpan akun",
                tint = if (saved) BloodAccent else BloodDim,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (saved) "TERSIMPAN" else "SIMPAN",
            color = if (saved) BloodAccent else BloodDim,
            fontSize = 7.sp,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun NoticeCard(title: String, message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF130202))
            .border(BorderStroke(1.dp, BloodBorderSoft), RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(52.dp)
                .background(BloodAccent),
        )
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            Text(
                title,
                color = BloodAccent2,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(message, color = Color(0x99FFC8C8), fontSize = 11.sp)
        }
    }
}

@Composable
private fun BloodOutlineButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    dim: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (dim) Color(0x0DCC1111) else Color(0x1ACC1111))
            .border(
                BorderStroke(1.dp, if (dim) Color(0x26CC1111) else Color(0x4DCC1111)),
                RoundedCornerShape(13.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = if (dim) BloodDim else BloodAccent2,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
        }
    }
}

@Composable
private fun FooterText(text: String) {
    Text(text, color = BloodFaint, fontSize = 8.sp, letterSpacing = 2.5.sp)
}

@Composable
private fun FooterDot() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(3.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x4DCC1414)),
    )
}

@Composable
private fun bloodFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0x1FCC1111),
    unfocusedContainerColor = BloodFieldBg,
    disabledContainerColor = BloodFieldBg,
    focusedTextColor = BloodText,
    unfocusedTextColor = BloodText,
    disabledTextColor = BloodDim,
    focusedBorderColor = Color(0x80CC1111),
    unfocusedBorderColor = Color(0x26CC1111),
    disabledBorderColor = Color(0x1ACC1111),
    focusedLeadingIconColor = BloodAccent2,
    unfocusedLeadingIconColor = BloodFaint,
    focusedTrailingIconColor = BloodAccent2,
    unfocusedTrailingIconColor = BloodDim,
    cursorColor = BloodAccent2,
    focusedPlaceholderColor = BloodFaint,
    unfocusedPlaceholderColor = BloodFaint,
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
