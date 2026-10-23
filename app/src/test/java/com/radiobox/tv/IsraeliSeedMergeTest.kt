package com.radiobox.tv

import com.radiobox.tv.core.Genre
import com.radiobox.tv.core.Genres
import com.radiobox.tv.core.IsraeliSeed
import com.radiobox.tv.core.StationRank
import com.radiobox.tv.data.api.RadioBrowserApi
import com.radiobox.tv.data.model.RBCountry
import com.radiobox.tv.data.model.RBStation
import com.radiobox.tv.data.repo.StationsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the curated Israeli seed list is merged correctly with API results. */
class IsraeliSeedMergeTest {

    private class FakeApi : RadioBrowserApi {
        var stations: List<RBStation> = emptyList()
        var clicks = 0
        override suspend fun countries(): List<RBCountry> = emptyList()
        override suspend fun searchStations(params: Map<String, String>): List<RBStation> = stations
        override suspend fun stationsByUuid(uuids: String): List<RBStation> = stations
        override suspend fun registerClick(uuid: String) { clicks++ }
    }

    private val news = Genres.byId("news")

    private fun apiStation(
        name: String,
        url: String,
        tags: String = "news",
        votes: Int = 100,
    ) = RBStation(
        uuid = "api-$name", name = name, urlResolved = url, tags = tags,
        votes = votes, bitrate = 128, countryCode = "IL",
    )

    @Test
    fun `seed stations lead the israeli genre`() = runTest {
        val api = FakeApi()
        val repo = StationsRepository(api)
        val result = repo.stationsForGenre(Genres.byId("israeli"), "IL", 0)
        assertTrue(result.isNotEmpty())
        assertTrue("first entry should be seed", result.first().uuid.startsWith("seed-"))
        // Canonical Israeli stations must be present thanks to the seed list.
        val names = result.map { it.name }
        assertTrue("גלגלצ in israeli", names.any { it.contains("גלגלצ") })
        assertTrue("רדיו צחוק in israeli", names.any { it.contains("צחוק") })
    }

    @Test
    fun `api twin of a seed station is dropped by streamKey`() = runTest {
        val api = FakeApi().apply {
            stations = listOf(
                apiStation("Kan Bet", "https://playerservices.streamtheworld.com/api/livestream-redirect/KAN_BET.mp3"),
                apiStation("Other", "https://example.com/other.mp3"),
            )
        }
        val result = StationsRepository(api).stationsForGenre(news, "IL", 0)
        assertEquals(1, result.count { it.name.contains("ב'") })
        assertTrue(result.any { it.name == "Other" })
        assertTrue(result.first().uuid.startsWith("seed-"))
    }

    @Test
    fun `api twin is dropped via alias map`() = runTest {
        val api = FakeApi().apply {
            // "88FM" normalizes to a registered alias of seed "kan-88".
            stations = listOf(apiStation("88FM", "https://example.com/88.mp3"))
        }
        val result = StationsRepository(api).stationsForGenre(Genres.byId("rock"), "IL", 0)
        assertTrue("no 88FM API duplicate", result.none { it.url.contains("example.com/88.mp3") })
        assertTrue(result.any { it.uuid == "seed-kan-88" })
    }

    @Test
    fun `seed is injected per genre only when tags overlap`() = runTest {
        val api = FakeApi()
        val classical = StationsRepository(api).stationsForGenre(Genres.byId("classical"), "IL", 0)
        assertTrue("קול המוסיקה seeded into classical",
            classical.any { it.uuid == "seed-kan-kol-hamusica" })
        assertTrue("no mizrahi stations in classical", classical.none { it.name.contains("דודי") })
    }

    @Test
    fun `seed skipped for non-IL country`() = runTest {
        val api = FakeApi()
        val result = StationsRepository(api).stationsForGenre(news, "US", 0)
        assertTrue(result.none { it.uuid.startsWith("seed-") })
    }

    @Test
    fun `blocked seed station is filtered out`() = runTest {
        val api = FakeApi()
        val repo = StationsRepository(api)
        val blocked = setOf(StationRank.nameKey("כאן רשת ב'"))
        val result = repo.stationsForGenre(news, "IL", 0, blocked = blocked)
        assertTrue(result.none { it.uuid == "seed-kan-bet" })
    }

    @Test
    fun `registerClick ignores seed uuids`() {
        val api = FakeApi()
        StationsRepository(api).registerClick("seed-lolfm")
        assertEquals(0, api.clicks)
        StationsRepository(api).registerClick("real-uuid")
        // fire-and-forget: poll instead of a fixed sleep so a cold worker JVM can't race it
        val deadline = System.currentTimeMillis() + 5000
        while (api.clicks == 0 && System.currentTimeMillis() < deadline) Thread.sleep(25)
        assertEquals(1, api.clicks)
    }

    @Test
    fun `breslev stations are hard-blocked in every path`() = runTest {
        val api = FakeApi().apply {
            stations = listOf(
                apiStation(
                    "ברסלב פוריו: דעת רבנו",
                    "https://cast.breslevforyou.co.il/listen/radiobreslev/radio.mp3",
                    tags = "jewish,music",
                ),
                apiStation(
                    "ברסלב פוריו",
                    "https://example.com/breslev-main.mp3",
                    tags = "jewish,music",
                ),
                apiStation("regular", "https://example.com/ok.mp3", tags = "jewish,music"),
            )
        }
        val repo = StationsRepository(api)
        for (genreId in listOf("israeli", "world", "regional")) {
            val res = repo.stationsForGenre(Genres.byId(genreId), "IL", 0)
            assertTrue("$genreId: no breslev host", res.none { it.url.contains("breslevforyou") })
            assertTrue("$genreId: no breslev name", res.none { it.name.contains("ברסלב") })
        }
        val home = repo.popularInCountry("IL", 0)
        assertTrue(home.none { it.name.contains("ברסלב") })
    }

    @Test
    fun `api twin is dropped by hebrew alias`() = runTest {
        val api = FakeApi().apply {
            stations = listOf(apiStation("גלגלץ", "https://example.com/glglz-twin.mp3", tags = "pop"))
        }
        val result = StationsRepository(api).stationsForGenre(Genres.byId("pop"), "IL", 0)
        assertTrue("twin dropped via alias", result.none { it.url.contains("glglz-twin") })
        assertTrue(result.any { it.uuid == "seed-glglz" })
    }

    @Test
    fun `seed catalog sanity`() {
        assertTrue("47 curated stations", IsraeliSeed.ALL.size >= 47)
        assertTrue(IsraeliSeed.ALL.all { it.stream.startsWith("http") })
        assertTrue(IsraeliSeed.ALL.all { it.tags.isNotEmpty() })
        // Every alias must normalize to exactly one registered seed slug.
        assertTrue(IsraeliSeed.ALIASES.keys.all { IsraeliSeed.bySlugOrNull(it) != null })
        // lolfm must not be aliased by a different slug.
        assertEquals("lolfm", IsraeliSeed.seedSlugFor(StationRank.nameKey("lolfm")))
    }
}
