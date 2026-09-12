package com.zaaam.zreming.util

import android.content.Context
import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dipanggil tiap kali app dibuka/kembali ke foreground (lihat pemanggilan
 * di MainActivity). Ambil daftar notifikasi terbaru dari server, dan untuk
 * yang belum pernah ditampilkan sebagai system notification (dilacak lewat
 * `lastSeenNotificationId` di SessionManager), tampilkan lewat
 * NotificationHelper. Broadcast dari owner dapat channel prioritas tinggi
 * tersendiri; jenis lain (follower baru, pesan chat, dst) pakai channel umum.
 */
@Singleton
class BroadcastSyncManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) {
    suspend fun checkAndNotify(context: Context) {
        if (!authRepository.isLoggedIn()) return

        val (notifications, _) = try {
            authRepository.getNotifications()
        } catch (_: Exception) {
            return
        }
        if (notifications.isEmpty()) return

        val lastSeenId = sessionManager.getLastSeenNotificationId()
        val lastSeenIndex = notifications.indexOfFirst { it.id == lastSeenId }

        // kalau belum pernah ada catatan lastSeenId (baru pertama kali install/login),
        // jangan langsung banjirin notifikasi lama — cukup catat yang paling baru
        // sebagai baseline dan tampilkan mulai dari yang terjadi SETELAH ini.
        val newOnes = if (lastSeenIndex == -1) emptyList() else notifications.subList(0, lastSeenIndex)

        newOnes.reversed().forEach { notif ->
            val channel = if (notif.type == "BROADCAST") {
                NotificationHelper.CHANNEL_ID_BROADCAST
            } else {
                NotificationHelper.CHANNEL_ID_GENERAL
            }
            NotificationHelper.show(
                context = context,
                notificationId = notif.id.hashCode(),
                channelId = channel,
                title = notif.title,
                body = notif.body,
            )
        }

        notifications.firstOrNull()?.let { sessionManager.saveLastSeenNotificationId(it.id) }
    }
}
