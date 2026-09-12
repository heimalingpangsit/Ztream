package com.zaaam.zreming.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.zreming.data.model.PublicProfileDto
import com.zaaam.zreming.ui.common.RoleBadge
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim
import com.zaaam.zreming.ui.theme.TextLight

@Composable
fun UserAvatar(username: String, size: Int = 44) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(PrimaryRed.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            username.take(1).uppercase(),
            color = PrimaryRed,
            fontSize = (size / 2.2).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun UsernameWithBadge(username: String, verified: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("@$username", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (verified) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Verified",
                tint = androidx.compose.ui.graphics.Color(0xFF3897F0),
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
fun UserListRow(
    user: PublicProfileDto,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(username = user.username)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            UsernameWithBadge(user.username, user.verified)
            Spacer(Modifier.height(3.dp))
            Row {
                RoleBadge(role = user.role, vip = user.vip)
                Spacer(Modifier.width(6.dp))
                Text("${user.followersCount} pengikut", color = TextDim, fontSize = 11.sp)
            }
        }
        trailing()
    }
}
