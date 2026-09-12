package com.zaaam.zreming.domain.model

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

data class CaptionLanguage(
    val code: String,
    val label: String,
)

val AVAILABLE_CAPTION_LANGUAGES = listOf(
    CaptionLanguage("id", "Indonesia"),
    CaptionLanguage("en", "English"),
    CaptionLanguage("es", "Español"),
    CaptionLanguage("fr", "Français"),
    CaptionLanguage("de", "Deutsch"),
    CaptionLanguage("ja", "日本語"),
    CaptionLanguage("ko", "한국어"),
    CaptionLanguage("zh-CN", "中文"),
    CaptionLanguage("ar", "العربية"),
    CaptionLanguage("hi", "हिन्दी"),
)
