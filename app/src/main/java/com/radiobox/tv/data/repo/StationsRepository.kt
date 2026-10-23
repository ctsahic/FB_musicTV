package com.radiobox.tv.data.repo

import com.radiobox.tv.core.Genre
import com.radiobox.tv.core.IsraeliSeed
import com.radiobox.tv.core.StationRank
import com.radiobox.tv.data.api.RadioBrowserApi
import com.radiobox.tv.data.model.RBCountry
import com.radiobox.tv.data.model.RBStation
import com.radiobox.tv.data.model.Station
import com.radiobox.tv.data.model.toStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StationsRepository(private val api: RadioBrowserApi) {

    suspend fun countries(): List<RBCountry> = withContext(Dispatchers.IO) {
        runCatching { api.countries() }.getOrElse { emptyList() }
    }

    /**
     * Stations for a genre in a country, honoring the minimum-bitrate quality filter.
     * Multi-tag genres are OR-merged across parallel requests, deduplicated by UUID *and*
     * by stream URL (the directory often lists the same stream twice), then ranked by
     * genre relevance via [StationRank] so tag-spam stations don't dominate every category.
     */
    suspend fun stationsForGenre(
        genre: Genre,
        countryCode: String,
        minBitrate: Int,
        blocked: Set<String> = emptySet(),
    ): List<Station> =
        withContext(Dispatchers.IO) {
            if (genre.regional) {
                regionalStations(countryCode, blocked)
            } else {
                coroutineScope {
                    val deferred = genre.tags.map { tag ->
                        async {
                            runCatching {
                                api.searchStations(
                                    baseParams(countryCode, limit = 40) +
                                        mapOf("tag" to tag) +
                                        if (minBitrate > 0) mapOf("bitrateMin" to minBitrate.toString()) else emptyMap()
                                )
                            }.getOrElse { emptyList() }
                        }
                    }
                    val api = deferred.awaitAll()
                        .flatten()
                        .filter { it.streamUrl.isNotBlank() }
                        .distinctBy { it.uuid }
                        .sortedWith(compareByDescending<RBStation> { it.votes }.thenByDescending { it.bitrate })
                        .distinctBy { StationRank.streamKey(it.streamUrl) }
                        .distinctBy { StationRank.nameKey(it.name) }

                    // Curated seed entries (verified streams, our own genre tags)
                    // come first; API twins of the same station are dropped.
                    val seed = seedForGenre(genre, countryCode)
                    val seedKeys = seed.map { StationRank.streamKey(it.streamUrl) }.toSet()
                    val seedNameKeys = seed.map { StationRank.nameKey(it.name) }.toSet()
                    val merged = (seed + api.filter { st ->
                        val nk = StationRank.nameKey(st.name)
                        StationRank.streamKey(st.streamUrl) !in seedKeys &&
                            nk !in seedNameKeys &&
                            IsraeliSeed.seedSlugFor(nk) == null
                    })
                        .filter { it.uuid !in blocked && StationRank.nameKey(it.name) !in blocked }
                        .filter { !IsraeliSeed.isHostBlocked(it.streamUrl) }
                        .filter { !IsraeliSeed.isNameBlocked(StationRank.nameKey(it.name)) }
                        .distinctBy { StationRank.streamKey(it.streamUrl) }
                        .distinctBy { StationRank.nameKey(it.name) }

                    StationRank.rankByGenre(merged, genre)
                        // Stable sort keeps StationRank order inside each group.
                        .sortedBy { if (it.uuid.startsWith("seed-")) 0 else 1 }
                        .take(60)
                        .map { it.toStation() }
                }
            }
        }

    /** Regional stations = stations in the country that report a state/region. */
    private suspend fun regionalStations(countryCode: String, blocked: Set<String>): List<Station> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.searchStations(baseParams(countryCode, limit = 200))
            }.getOrElse { emptyList() }
                .filter { !it.state.isNullOrBlank() && it.streamUrl.isNotBlank() }
                .filter { !IsraeliSeed.isHostBlocked(it.streamUrl) }
                .filter { !IsraeliSeed.isNameBlocked(StationRank.nameKey(it.name)) }
                .distinctBy { it.uuid }
                .sortedByDescending { it.votes }
                .distinctBy { StationRank.streamKey(it.streamUrl) }
                .distinctBy { StationRank.nameKey(it.name) }
                .filter { it.uuid !in blocked && StationRank.nameKey(it.name) !in blocked }
                .take(60)
                .map { it.toStation() }
        }

    /** Popular stations in a country (home screen fallback). */
    suspend fun popularInCountry(
        countryCode: String,
        minBitrate: Int,
        limit: Int = 20,
        blocked: Set<String> = emptySet(),
    ): List<Station> =
        withContext(Dispatchers.IO) {
            val api = runCatching {
                api.searchStations(
                    baseParams(countryCode, limit = limit) +
                        if (minBitrate > 0) mapOf("bitrateMin" to minBitrate.toString()) else emptyMap()
                )
            }.getOrElse { emptyList() }
                .filter { it.streamUrl.isNotBlank() }

            val seed = seedForGenre(null, countryCode)
            val seedKeys = seed.map { StationRank.streamKey(it.streamUrl) }.toSet()
            val seedNameKeys = seed.map { StationRank.nameKey(it.name) }.toSet()
            (seed + api.filter { st ->
                val nk = StationRank.nameKey(st.name)
                StationRank.streamKey(st.streamUrl) !in seedKeys &&
                    nk !in seedNameKeys &&
                    IsraeliSeed.seedSlugFor(nk) == null
            })
                .filter { it.uuid !in blocked && StationRank.nameKey(it.name) !in blocked }
                .filter { !IsraeliSeed.isHostBlocked(it.streamUrl) }
                .filter { !IsraeliSeed.isNameBlocked(StationRank.nameKey(it.name)) }
                .distinctBy { StationRank.streamKey(it.streamUrl) }
                .distinctBy { StationRank.nameKey(it.name) }
                .sortedBy { if (it.uuid.startsWith("seed-")) 0 else 1 }
                .take(limit)
                .map { it.toStation() }
        }

    /** Seed stations matching the genre (all seeds for the home screen); IL only. */
    private fun seedForGenre(genre: Genre?, countryCode: String): List<RBStation> {
        if (countryCode != "IL") return emptyList()
        return IsraeliSeed.ALL
            .filter { seed -> genre == null || seed.tags.intersect(genre.tags).isNotEmpty() }
            .map { s ->
                RBStation(
                    uuid = "seed-${s.slug}",
                    name = s.name,
                    urlResolved = s.stream,
                    homepage = "",
                    favicon = s.logo,
                    tags = s.tags.joinToString(","),
                    country = "Israel",
                    countryCode = "IL",
                    hls = if (s.hls) 1 else 0,
                )
            }
    }

    /** Fire-and-forget click counter that helps the community rank stations. */
    fun registerClick(uuid: String) {
        if (uuid.startsWith("seed-")) return
        CoroutineScope(Dispatchers.IO).launch { runCatching { api.registerClick(uuid) } }
    }

    private fun baseParams(countryCode: String, limit: Int) = mapOf(
        "countrycode" to countryCode,
        "hidebroken" to "true",
        "order" to "votes",
        "reverse" to "true",
        "limit" to limit.toString(),
    )
}
