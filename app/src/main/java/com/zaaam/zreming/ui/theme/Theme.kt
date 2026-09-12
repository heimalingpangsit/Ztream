package com.zaaam.zreming.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ZtreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZtreamColorScheme,
        content = content
    )
}
