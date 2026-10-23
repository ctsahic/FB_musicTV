package com.radiobox.tv.core

import com.radiobox.tv.data.model.RBStation

/**
 * Pure (unit-testable) logic for cleaning up station lists:
 * 1. Collapsing duplicate directory entries that point at the same stream
 *    or carry the same normalized name ("Kan Bet" listed twice with 2 URLs).
 * 2. Ranking stations by how relevant they really are to the chosen genre —
 *    a station that spams dozens of tags (e.g. "music, news, rock, pop, jazz…")
 *    gets demoted instead of floating to the top of every category.
 */
object StationRank {

    /** Tags so generic that matching them says nothing about the station's actual genre. */
    val NOISE_TAGS = setOf(
        "music", "musica", "musik", "muzika", "various", "various artists", "radio", "web radio",
        "hits", "hit music", "charts", "top 100", "mix", "mixed", "party", "live",
        "stream", "audio", "fm", "am", "dab", "internet", "online", "web",
        "local", "community", "student", "public", "commercial", "news talk",
        "interviews", "schedule", "entertainment", "culture", "lifestyle",
        "israel", "israeli music", "hebrew music", "80s", "90s", "70s", "60s", "2000s",
    )

    /** The station's comma-separated tag list, lowercased and trimmed. */
    fun tagList(station: RBStation): List<String> =
        station.tags.orEmpty().split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    /** 0.0 (every tag is meaningful) … 1.0 (every tag is generic filler). */
    fun noiseRatio(tagList: List<String>): Float =
        if (tagList.isEmpty()) 1f else tagList.count { it in NOISE_TAGS }.toFloat() / tagList.size

    /**
     * Normalizes a stream URL so mirrored directory entries of the same station
     * collapse into one card (scheme / www. / trailing slash are ignored).
     */
    fun streamKey(url: String): String =
        url.trim().lowercase()
            .removePrefix("https://").removePrefix("http://")
            .removePrefix("www.")
            .trimEnd('/')

    /** Normalized station name for merging directory duplicates ("Kan  Bet!" == "kanbet"). */
    fun nameKey(name: String): String =
        name.trim().lowercase().filter { it.isLetterOrDigit() }
            .ifBlank { name.trim().lowercase() }

    /**
     * Tag-spam detector: many tags but only a tiny fraction relevant to the genre
     * (e.g. 15 tags where exactly 1 matches) means the station floats in every
     * category — demote it below genuinely focused stations.
     */
    fun isTagSpam(tags: List<String>, genreTagSet: Set<String>): Boolean {
        if (tags.size < 8) return false
        val meaningful = tags.filter { it !in NOISE_TAGS }
        val frac = if (meaningful.isEmpty()) 0f
        else meaningful.count { it in genreTagSet }.toFloat() / meaningful.size
        return frac < 0.3f
    }

    /**
     * Rank = not tag-spam first, then match on the genre's core tag, then the
     * highest relevant-tag fraction, then community votes, then bitrate.
     */
    fun rankByGenre(stations: List<RBStation>, genre: Genre): List<RBStation> {
        if (genre.regional) return stations
        val core = genre.tags.first()
        val genreTags = genre.tags.toSet()
        return stations
            .map { st ->
                val ts = tagList(st)
                Ranked(
                    st,
                    coreMatch = if (core in ts.toSet()) 1 else 0,
                    frac = relevanceFraction(ts, genreTags),
                    spam = isTagSpam(ts, genreTags),
                )
            }
            .sortedWith(
                compareBy<Ranked> { it.spam }
                    .thenByDescending { it.coreMatch }
                    .thenByDescending { it.frac }
                    .thenByDescending { it.st.votes }
                    .thenByDescending { it.st.bitrate }
            )
            .map { it.st }
    }

    /** Fraction of meaningful tags that match the genre (0..1). */
    private fun relevanceFraction(tags: List<String>, genreTags: Set<String>): Float {
        val meaningful = tags.filter { it !in NOISE_TAGS }
        if (meaningful.isEmpty()) return 0f
        return meaningful.count { it in genreTags }.toFloat() / meaningful.size
    }

    private data class Ranked(val st: RBStation, val coreMatch: Int, val frac: Float, val spam: Boolean)
}
