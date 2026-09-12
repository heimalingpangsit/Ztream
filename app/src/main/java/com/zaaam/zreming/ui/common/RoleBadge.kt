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
 * OWNER  -> emas
 * VIP+   -> merah (role USER/ADMIN dengan vip == true)
 * MEMBER -> abu-abu (default)
 */
@Composable
fun RoleBadge(role: String, vip: Boolean, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when {
        role == "OWNER" -> Triple("\uD83D\uDC51 OWNER", Color(0xFFFFC107), Color(0xFF3D2E00))
        role == "ADMIN" -> Triple("\uD83D\uDEE1 ADMIN", Color(0xFF42A5F5), Color.White)
        vip -> Triple("\u2726 VIP+", Color(0xFFE50914), Color.White)
        else -> Triple("MEMBER", Color(0xFFB0BEC5), Color(0xFF263238))
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
