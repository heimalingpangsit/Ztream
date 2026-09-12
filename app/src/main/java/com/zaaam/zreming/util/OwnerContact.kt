package com.zaaam.zreming.util

object OwnerContact {
    const val WHATSAPP_NUMBER = "6285166139064"
    const val INSTAGRAM_HANDLE = "jrkiilagi06__"

    fun whatsappUrl(message: String = "Halo, saya mau minta dibuatkan akun Ztream"): String {
        val encoded = java.net.URLEncoder.encode(message, "UTF-8")
        return "https://wa.me/$WHATSAPP_NUMBER?text=$encoded"
    }

    fun instagramUrl(): String = "https://instagram.com/$INSTAGRAM_HANDLE"
}
