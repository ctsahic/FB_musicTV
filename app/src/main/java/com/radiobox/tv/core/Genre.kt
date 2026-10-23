package com.radiobox.tv.core

/** A content category; each maps to one or more Radio Browser tags (OR-matched, merged client-side). */
data class Genre(
    val id: String,
    val label: String,
    val emoji: String,
    val tags: List<String> = emptyList(),
    val regional: Boolean = false,
)

object Genres {
    val ALL = listOf(
        Genre("electronic", "אלקטרונית", "🎛", listOf("electronic", "dance", "techno", "house", "trance", "edm")),
        Genre("classical", "קלאסית", "🎻", listOf("classical", "opera", "symphony")),
        Genre("news", "חדשות", "📰", listOf("news", "talk", "information")),
        Genre("israeli", "ישראלי", "🇮🇱", listOf("israeli", "hebrew", "mizrahi")),
        Genre("pop", "פופ", "🎤", listOf("pop", "top 40")),
        Genre("rock", "רוק", "🎸", listOf("rock", "alternative", "metal")),
        Genre("jazz", "ג'אז ובלוז", "🎷", listOf("jazz", "blues", "soul")),
        Genre("world", "מוזיקת עולם", "🌍", listOf("world", "folk", "ethnic")),
        Genre("regional", "אזוריות", "📍", regional = true),
    )

    fun byId(id: String): Genre = ALL.first { it.id == id }
}
