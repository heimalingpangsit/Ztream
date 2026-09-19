package com.zaaam.zreming.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

/**
 * Menghasilkan HWID (fingerprint device) yang STABIL selama app belum
 * di-uninstall / HP belum di-factory-reset — ini yang dikirim ke backend
 * lewat LoginRequest.deviceId supaya server bisa hitung "device keberapa"
 * buat limit member (1 HP) / member+ (5 HP).
 *
 * Dibangun dari ANDROID_ID (unik per app+device+user profile di Android,
 * berubah kalau di-factory-reset) digabung sedikit info model HP, di-hash
 * SHA-256 biar bentuknya rapi & gak nyingkap ANDROID_ID mentahan.
 *
 * CATATAN JUJUR: ANDROID_ID bisa berubah kalau app di-uninstall lalu
 * install ulang di beberapa versi Android/OEM (jarang, tapi ada). Kalau
 * butuh yang lebih tahan banting, alternatifnya App Set ID / Play
 * Integrity API dari Google — tapi itu butuh Google Play Services & lebih
 * ribet setupnya. ANDROID_ID ini sudah cukup buat kasus "batasi jumlah HP
 * per akun" seperti yang kamu mau.
 */
object DeviceIdProvider {

    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ) ?: "unknown"

        val raw = "$androidId|${Build.MANUFACTURER}|${Build.MODEL}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** Nama device yang enak dibaca manusia, buat ditampilin di daftar HWID owner/user. */
    fun getDeviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}
