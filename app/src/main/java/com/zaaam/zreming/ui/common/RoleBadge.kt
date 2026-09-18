package com.zaaam.zreming.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * OWNER    -> emas
 * ADMIN    -> biru
 * MEMBER+  -> merah (vip == true, maxDevices > 1 — paket 5 HP)
 * MEMBER   -> merah muda (vip == true, maxDevices <= 1 — paket 1 HP)
 * (belum langganan / expired) -> abu-abu
 *
 * Dibedain dari `maxDevices` yang udah ada di UserDto, BUKAN dari string role
 * baru — jadi gak perlu ubah apa pun di backend buat badge ini kepasang benar,
 * asal owner set maxDevices=1 buat akun member dan maxDevices=5 (atau lebih
 * dari 1) buat akun member+ pas jual akunnya.
 */
@Composable
fun RoleBadge(role: String, vip: Boolean, maxDevices: Int = 1, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when {
        role == "OWNER" -> Triple("\uD83D\uDC51 OWNER", Color(0xFFFFC107), Color(0xFF3D2E00))
        role == "ADMIN" -> Triple("\uD83D\uDEE1 ADMIN", Color(0xFF42A5F5), Color.White)
        vip && maxDevices > 1 -> Triple("\u2726 MEMBER+", Color(0xFFE50914), Color.White)
        vip -> Triple("MEMBER", Color(0xFFB33A3A), Color.White)
        else -> Triple("BELUM MEMBER", Color(0xFFB0BEC5), Color(0xFF263238))
    }

    Text(
        text = label,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
