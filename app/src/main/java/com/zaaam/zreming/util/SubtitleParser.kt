package com.zaaam.zreming.util

import com.zaaam.zreming.domain.model.SubtitleCue

/**
 * Parser sederhana untuk format .srt dan .vtt. Tidak menangani semua edge
 * case format subtitle (styling tag, posisi, dsb) — cukup untuk menampilkan
 * teks + timing, yang paling penting untuk fitur CC ini.
 */
object SubtitleParser {

    private val srtTimeRegex = Regex(
        """(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})"""
    )

    fun parse(raw: String): List<SubtitleCue> {
        val cleaned = raw.replace("\r\n", "\n").replace("\r", "\n")
        val blocks = cleaned.split(Regex("\n\\s*\n"))
        val cues = mutableListOf<SubtitleCue>()

        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.isEmpty()) continue

            val timeLineIndex = lines.indexOfFirst { srtTimeRegex.containsMatchIn(it) }
            if (timeLineIndex == -1) continue

            val match = srtTimeRegex.find(lines[timeLineIndex]) ?: continue
            val g = match.groupValues // g[0] = whole match, g[1..8] = the 8 captured numbers

            val start = toMs(g[1], g[2], g[3], g[4])
            val end = toMs(g[5], g[6], g[7], g[8])

            val text = lines.drop(timeLineIndex + 1)
                .joinToString(" ")
                .replace(Regex("<[^>]*>"), "") // buang tag styling html-ish
                .trim()

            if (text.isNotBlank() && end > start) {
                cues.add(SubtitleCue(start, end, text))
            }
        }
        return cues.sortedBy { it.startMs }
    }

    private fun toMs(h: String, m: String, s: String, ms: String): Long {
        return h.toLong() * 3_600_000L + m.toLong() * 60_000L + s.toLong() * 1000L + ms.toLong()
    }
}
