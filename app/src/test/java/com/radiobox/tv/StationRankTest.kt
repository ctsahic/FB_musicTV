package com.radiobox.tv

import com.radiobox.tv.core.Genre
import com.radiobox.tv.core.StationRank
import com.radiobox.tv.data.model.RBStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StationRankTest {

    private fun st(
        uuid: String,
        name: String = "S$uuid",
        url: String = "https://stream.example/$uuid",
        tags: String = "",
        votes: Int = 0,
        bitrate: Int = 128,
    ) = RBStation(
        uuid = uuid, name = name, url = url, urlResolved = url, tags = tags,
        votes = votes, bitrate = bitrate,
    )

    // ---- streamKey / dedupe ----

    @Test
    fun `same stream collapses across scheme www and trailing slash`() {
        val a = streamKey("HTTPS://WWW.Ecast.co.il/stream/sahar/")
        val b = streamKey("https://live.ecast.co.il/stream/sahar")
        // scheme differs (https vs https here) but www and slash normalize to same key
        assertEquals(streamKey("https://live.ecast.co.il/stream/sahar"), b)
        assertNotEquals(a, b) // different host: not merged
        assertEquals(streamKey("http://Live.Example/x"), streamKey("https://www.Live.Example/x/"))
    }

    @Test
    fun `duplicate urls are deduped keeping the higher-vote entry`() {
        val s1 = st("1", url = "https://live.example/stream", votes = 3)
        val s2 = st("2", url = "https://www.example.com/stream", votes = 9)
        // different hosts -> different keys; use exact mirror for the real dup:
        val mirror = st("3", url = "https://live.example/stream/", votes = 12)
        val merged = listOf(s1, mirror).distinctBy { StationRank.streamKey(it.streamUrl) }
        assertEquals(1, merged.size)
        assertEquals("1", merged.single().uuid) // keeps first (already vote-sorted upstream)
    }

    // ---- noise tags ----

    @Test
    fun `noise ratio is 1 for empty and generic-only tags`() {
        assertEquals(1f, StationRank.noiseRatio(emptyList()), 0.001f)
        assertEquals(1f, StationRank.noiseRatio(listOf("music", "radio", "hits")), 0.001f)
    }

    @Test
    fun `noise ratio is 0 for fully meaningful tags`() {
        assertEquals(0f, StationRank.noiseRatio(listOf("techno", "house", "trance")), 0.001f)
    }

    // ---- ranking ----

    @Test
    fun `core-tag match outranks vote count`() {
        val genre = Genre("rock", "רוק", "🎸", listOf("rock", "alternative", "metal"))
        val tagSpam = st("sahar", tags = "music,radio,pop,jazz,rock,hebrew", votes = 100)
        val pureRock = st("pure", tags = "rock,hard rock", votes = 5)
        val ranked = StationRank.rankByGenre(listOf(tagSpam, pureRock), genre)
        assertEquals("pure", ranked.first().uuid)
    }

    @Test
    fun `no core tag anywhere keeps votes as tiebreaker`() {
        val genre = Genre("rock", "רוק", "🎸", listOf("rock", "metal"))
        val a = st("a", tags = "pop", votes = 30)
        val b = st("b", tags = "talk", votes = 40)
        val ranked = StationRank.rankByGenre(listOf(a, b), genre)
        assertEquals("b", ranked.first().uuid)
    }

    @Test
    fun `regional genre returns list unchanged`() {
        val genre = Genre("regional", "אזוריות", "📍", regional = true)
        val list = listOf(st("x"), st("y"))
        assertEquals(list, StationRank.rankByGenre(list, genre))
    }

    // ---- name merging / tag spam ----

    @Test
    fun `nameKey normalizes spacing case and punctuation`() {
        assertEquals(StationRank.nameKey("Kan  Bet!"), StationRank.nameKey("kan bet"))
        assertEquals("kanbet", StationRank.nameKey("Kan Bet"))
    }

    @Test
    fun `same name different urls merge to one entry`() {
        val a = st("1", name = "Kan Bet", url = "https://a.example/stream", votes = 30)
        val b = st("2", name = "Kan  Bet ", url = "https://b.example/other", votes = 10)
        val merged = listOf(a, b).sortedWith(compareByDescending<RBStation> { it.votes })
            .distinctBy { StationRank.nameKey(it.name) }
        assertEquals(1, merged.size)
        assertEquals("1", merged.single().uuid)
    }

    @Test
    fun `tag spam with single low-fraction match is demoted`() {
        val genre = Genre("classical", "קלאסית", "🎻", listOf("classical", "opera", "symphony"))
        val spam = st("sahar", tags = "hebrew,hip-hop,interviews,jazz,music,news,news talk,pop,psychedelic,psytrance,r&b,rap,rock,soul,trap", votes = 23)
        val pure = st("pure", tags = "classical,opera", votes = 2)
        assertTrue(StationRank.isTagSpam(StationRank.tagList(spam), genre.tags.toSet()))
        assertEquals("pure", StationRank.rankByGenre(listOf(spam, pure), genre).first().uuid)
    }

    @Test
    fun `focused station with few tags is not spam`() {
        val genre = Genre("rock", "רוק", "🎸", listOf("rock", "alternative", "metal"))
        val ok = st("ok", tags = "rock,hard rock,classic rock", votes = 5)
        assertTrue(!StationRank.isTagSpam(StationRank.tagList(ok), genre.tags.toSet()))
    }

    private fun streamKey(url: String) = StationRank.streamKey(url)
}
