package com.radiobox.tv

import com.radiobox.tv.data.model.PodcastFav
import com.radiobox.tv.data.model.StationFav
import com.radiobox.tv.data.repo.FavoritesCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesCodecTest {

    @Test
    fun `station round trip`() {
        val favs = listOf(
            StationFav(
                uuid = "abc-123",
                name = "Galgalatz",
                url = "http://stream.example.com/ggl",
                logo = "http://logo.example.com/ggl.png",
                bitrate = 128,
                codec = "MP3",
                country = "ישראל",
                countryCode = "IL",
                state = null,
                isHls = false,
            )
        )
        val decoded = FavoritesCodec.decodeStations(FavoritesCodec.encodeStations(favs))
        assertEquals(1, decoded.size)
        assertEquals("abc-123", decoded[0].uuid)
        assertEquals("Galgalatz", decoded[0].name)
        assertEquals(128, decoded[0].bitrate)
        assertEquals("IL", decoded[0].countryCode)
    }

    @Test
    fun `podcast round trip`() {
        val favs = listOf(
            PodcastFav(name = "עו\"ל", artist = "כאן", feedUrl = "https://feeds.example.com/pod.xml", artworkUrl = null)
        )
        val decoded = FavoritesCodec.decodePodcasts(FavoritesCodec.encodePodcasts(favs))
        assertEquals(1, decoded.size)
        assertEquals("https://feeds.example.com/pod.xml", decoded[0].feedUrl)
        assertEquals("עו\"ל", decoded[0].name)
    }

    @Test
    fun `decode garbage returns empty and never throws`() {
        assertTrue(FavoritesCodec.decodeStations(null).isEmpty())
        assertTrue(FavoritesCodec.decodeStations("").isEmpty())
        assertTrue(FavoritesCodec.decodeStations("{not json").isEmpty())
        assertTrue(FavoritesCodec.decodePodcasts("][oops").isEmpty())
        assertNull(FavoritesCodec.decodeLastStation("garbage"))
    }

    @Test
    fun `last station round trip`() {
        val fav = StationFav(uuid = "u1", name = "Kan Bet", url = "http://s/x.mp3")
        val decoded = FavoritesCodec.decodeLastStation(FavoritesCodec.encodeLastStation(fav))
        assertEquals("u1", decoded?.uuid)
        assertEquals("Kan Bet", decoded?.name)
    }
}
