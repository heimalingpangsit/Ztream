package com.zaaam.zreming.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zaaam.zreming.R

/**
 * CATATAN ARSITEKTUR — kenapa ini bukan Firebase Cloud Messaging (FCM):
 *
 * Notifikasi "beneran" yang muncul di status bar walau app tertutup/di-kill
 * butuh push service (FCM di Android). Itu butuh setup project Firebase
 * terpisah (google-services.json + server key), yang belum ada di sini.
 *
 * Yang diimplementasikan di sini: app CEK notifikasi baru (termasuk
 * broadcast dari owner) tiap kali app dibuka/foreground, lalu nampilin
 * local notification lewat NotificationManager biasa untuk yang belum
 * pernah ditampilkan. Ini jalan selama app pernah dibuka user dalam waktu
 * dekat — bukan instant push ke HP yang appnya lagi ke-close total.
 * Kalau nanti mau upgrade ke FCM asli, tinggal ganti bagian "trigger"-nya
 * (BroadcastSyncManager) jadi FirebaseMessagingService, helper channel dan
 * builder notifikasi di bawah ini tetap dipakai sama persis.
 */
object NotificationHelper {

    const val CHANNEL_ID_GENERAL = "zarstream_general"
    const val CHANNEL_ID_BROADCAST = "zarstream_broadcast"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_GENERAL,
                "Aktivitas Umum",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Follower baru, pesan, dan aktivitas sosial lainnya"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_BROADCAST,
                "Pengumuman ZarStream",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengumuman & promo resmi dari owner ZarStream"
            }
        )
    }

    fun show(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
    ) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo_mark)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Butuh izin POST_NOTIFICATIONS runtime di Android 13+. Kalau belum
        // diizinkan user, ini akan silently gagal — pastikan sudah minta izin
        // itu di layar lain (mis. Home/Profile) via ActivityResultContracts.
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // izin belum diberikan, abaikan
        }
    }
}
