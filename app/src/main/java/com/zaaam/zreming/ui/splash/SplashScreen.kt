package com.zaaam.zreming.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zaaam.zreming.R
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
import kotlinx.coroutines.delay

/**
 * Intro sinematik pakai logo asli ZarStream:
 *  1) mark "Z" muncul elegan (fade + scale halus, tanpa overshoot alay)
 *     dengan glow merah yang "bernapas" pelan di belakangnya
 *  2) crossfade mulus dari mark-only ke lockup penuh "Z + ZarStream"
 *     sambil sedikit membesar, terasa seperti mark "mekar" jadi wordmark
 *  3) tahan sejenak, lalu fade-out seluruh splash
 *
 * Total durasi ±2.4 detik.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val markScale = remember { Animatable(0.92f) }
    val markAlpha = remember { Animatable(0f) }

    val lockupAlpha = remember { Animatable(0f) }
    val lockupScale = remember { Animatable(0.94f) }

    val containerAlpha = remember { Animatable(1f) }

    // glow yang berdenyut pelan & terus berputar lembut selama splash tampil —
    // memberi kesan "hidup"/premium tanpa terasa ramai
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
        ),
        label = "glowRotation",
    )

    LaunchedEffect(Unit) {
        // 1) mark fade + scale-in halus, elegan (bukan overshoot kaya mainan)
        markAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        markScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))

        // 2) tahan sebentar sebagai mark saja
        delay(500)

        // 3) crossfade ke lockup penuh (Z + ZarStream) sambil sedikit membesar
        markAlpha.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        lockupScale.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        lockupAlpha.animateTo(1f, tween(550, easing = FastOutSlowInEasing))

        // 4) tahan wordmark penuh
        delay(700)

        // 5) fade-out seluruh splash
        containerAlpha.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(containerAlpha.value)
            .background(DarkBg),
        contentAlignment = Alignment.Center,
    ) {
        // glow ambient di belakang logo — berdenyut pelan & berotasi sangat lambat
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(1f + (glowPulse - 0.55f) * 0.15f)
                .rotate(glowRotation)
                .alpha(0.35f * glowPulse * (markAlpha.value + lockupAlpha.value).coerceAtMost(1f))
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            PrimaryRed.copy(alpha = 0.5f),
                            Color.Transparent,
                            PrimaryRed.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                )
                .scale(1.4f),
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .alpha(0.5f * glowPulse)
                .background(Brush.radialGradient(listOf(PrimaryRed.copy(alpha = 0.4f), Color.Transparent))),
        )

        // logo mark saja (fase 1)
        Image(
            painter = painterResource(id = R.drawable.logo_mark),
            contentDescription = "ZarStream",
            modifier = Modifier
                .size(150.dp)
                .scale(markScale.value)
                .alpha(markAlpha.value),
        )

        // lockup penuh: Z + wordmark "ZarStream" (fase 2)
        Image(
            painter = painterResource(id = R.drawable.logo_app),
            contentDescription = "ZarStream",
            modifier = Modifier
                .size(240.dp)
                .scale(lockupScale.value)
                .alpha(lockupAlpha.value),
        )
    }
}
