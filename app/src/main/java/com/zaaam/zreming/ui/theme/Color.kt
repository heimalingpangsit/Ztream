package com.zaaam.zreming.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DarkBg = Color(0xFF07080C)
val SurfaceDark = Color(0xFF0E121C)
val CardDark = Color(0xFF161B26)
val GlassSurface = Color(0x73161B26) // rgba(22, 27, 38, 0.45)
val GlassBorder = Color(0x26FFFFFF)  // rgba(255, 255, 255, 0.15)
val GlassBorderBright = Color(0x40FFFFFF) // rgba(255, 255, 255, 0.25)
val PrimaryRed = Color(0xFFFF2D55)
val AccentCyan = Color(0xFF00E5FF)
val TextLight = Color(0xFFFFFFFF)
val TextDim = Color(0xFF8E99AA)
val TextSubtle = Color(0xFF525D6E)
val GoldRating = Color(0xFFFFB800)

val ZtreamColorScheme = darkColorScheme(
    primary = PrimaryRed,
    onPrimary = Color.White,
    background = DarkBg,
    onBackground = TextLight,
    surface = SurfaceDark,
    onSurface = TextLight,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextDim,
    secondary = AccentCyan
)
