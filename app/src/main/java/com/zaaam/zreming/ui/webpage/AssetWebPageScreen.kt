package com.zaaam.zreming.ui.webpage

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Box
import com.zaaam.zreming.ui.theme.DarkBg
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import com.zaaam.zreming.ui.theme.TextLight
import com.zaaam.zreming.ui.theme.PrimaryRed

/**
 * Base URL backend asli — dipakai sebagai "origin" palsu buat halaman HTML
 * yang di-bundle di assets, supaya panggilan fetch("/api/...") RELATIF di
 * dalam order.html/helper.html tetap nyasar ke worker asli, bukan gagal
 * karena origin-nya file:///android_asset/.
 *
 * Kalau nanti domain worker-nya ganti, cukup ubah ini SATU baris.
 */
private const val API_ORIGIN = "https://jepverse.pokaycore.workers.dev/"

/**
 * Menampilkan file HTML statis yang di-bundle di app/src/main/assets/
 * (order.html buat beli akun, helper.html buat pulihkan akun) di dalam
 * WebView in-app, dengan base URL di-set ke domain backend asli supaya
 * semua fetch("/api/...") relatif di halaman itu jalan normal.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AssetWebPageScreen(
    assetFileName: String,
    title: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    val backHandler = rememberUpdatedState(onBack)

    Column(Modifier.fillMaxSize().background(DarkBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextLight)
            }
            Text(
                title,
                color = TextLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp),
            )
        }

        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        // Jembatan buat tombol "back" yang ada DI DALAM halaman
                        // HTML-nya. Halaman aslinya dibikin buat versi web, jadi
                        // tombolnya nunjuk ke login.html/dashboard.html yang gak
                        // ada di app — sekarang tombol itu manggil ZarApp.close()
                        // dan yang nutup layarnya navigasi native.
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun close() {
                                    post { backHandler.value() }
                                }
                            },
                            "ZarApp",
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }
                        val html = context.assets.open(assetFileName)
                            .bufferedReader()
                            .use { it.readText() }
                        loadDataWithBaseURL(API_ORIGIN, html, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryRed)
                }
            }
        }
    }
}
