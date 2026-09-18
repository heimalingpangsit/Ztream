package com.zaaam.zreming.ui.nobar

/**
 * Format penanda undangan nobar yang dikirim lewat chat pribadi.
 *
 * Kenapa lewat chat? Karena undangan yang cuma lewat notifikasi backend
 * ternyata gak selalu kebaca di sisi yang diundang (teks notifikasinya gak
 * memuat kode room, jadi gak bisa dipakai buat gabung). Chat pribadi sudah
 * pasti jalan, jadi dipakai sebagai jalur cadangan yang bisa diandalkan:
 * penerima langsung lihat undangannya di tab Join Nobar DAN di chat.
 */
object NobarInviteMarker {

    const val PREFIX = "[NOBAR]"

    private val REGEX = Regex("""\[NOBAR]\s*([A-Za-z0-9_.\-]{3,})\s*(?:::\s*(.*))?""", RegexOption.IGNORE_CASE)

    fun build(roomId: String, contentTitle: String): String {
        val title = contentTitle.replace("\n", " ").trim().ifBlank { "Nobar" }
        return "$PREFIX $roomId :: $title\n" +
            "Aku ngajak kamu nobar. Buka tab Join Nobar, undangannya sudah ada di sana."
    }

    /** Mengembalikan pasangan roomId dan judul kalau teks ini undangan nobar. */
    fun parse(text: String?): Pair<String, String>? {
        if (text.isNullOrBlank()) return null
        val match = REGEX.find(text) ?: return null
        val roomId = match.groupValues[1].trim()
        if (roomId.isBlank()) return null
        val title = match.groupValues.getOrNull(2)?.trim()?.substringBefore('\n')?.trim().orEmpty()
        return roomId to title
    }
}
