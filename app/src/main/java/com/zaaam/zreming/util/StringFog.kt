package com.zaaam.zreming.util

import android.util.Base64

/**
 * Native String Obfuscator (StringFog equivalent)
 * Obfuscates sensitive strings at compile-time / runtime using XOR key + Base64 masking
 * preventing plaintext string extraction via reverse-engineering / decompilation.
 */
object StringFog {
    private const val FOG_KEY: Byte = 0x5A

    fun decrypt(encoded: String): String {
        return try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            val decrypted = ByteArray(bytes.size)
            for (i in bytes.indices) {
                decrypted[i] = (bytes[i].toInt() xor FOG_KEY.toInt()).toByte()
            }
            String(decrypted, Charsets.UTF_8)
        } catch (_: Exception) {
            encoded
        }
    }

    fun encrypt(raw: String): String {
        val bytes = raw.toByteArray(Charsets.UTF_8)
        val encrypted = ByteArray(bytes.size)
        for (i in bytes.indices) {
            encrypted[i] = (bytes[i].toInt() xor FOG_KEY.toInt()).toByte()
        }
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }
}
