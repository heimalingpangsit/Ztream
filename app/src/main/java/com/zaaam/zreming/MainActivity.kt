package com.zaaam.zreming

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.ui.navigation.AppNavigation
import com.zaaam.zreming.ui.theme.ZtreamTheme
import com.zaaam.zreming.util.BroadcastSyncManager
import com.zaaam.zreming.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var broadcastSyncManager: BroadcastSyncManager

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* hasil diabaikan; kalau ditolak, notif cuma nggak muncul */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        requestNotificationPermissionIfNeeded()

        setContent {
            ZtreamTheme {
                AppNavigation(sessionManager = sessionManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Cek notifikasi baru (termasuk broadcast owner) tiap kali app dibuka
        // atau kembali ke foreground — lihat catatan arsitektur di
        // BroadcastSyncManager soal kenapa ini bukan FCM.
        lifecycleScope.launch {
            broadcastSyncManager.checkAndNotify(this@MainActivity)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
