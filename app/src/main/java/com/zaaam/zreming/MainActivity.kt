package com.zaaam.zreming

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.ui.maintenance.MaintenanceScreen
import com.zaaam.zreming.ui.maintenance.MaintenanceViewModel
import com.zaaam.zreming.ui.navigation.AppNavigation
import com.zaaam.zreming.ui.theme.DarkBg
import com.zaaam.zreming.ui.theme.PrimaryRed
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
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        requestNotificationPermissionIfNeeded()

        setContent {
            ZtreamTheme {
                val maintenanceViewModel: MaintenanceViewModel = hiltViewModel()
                val maintenanceState by maintenanceViewModel.uiState.collectAsState()

                when {
                    maintenanceState.isChecking -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = PrimaryRed)
                        }
                    }

                    maintenanceState.isMaintenance -> {
                        MaintenanceScreen(
                            onRetry = { maintenanceViewModel.checkMaintenance() },
                        )
                    }

                    else -> {
                        AppNavigation(sessionManager = sessionManager)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
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