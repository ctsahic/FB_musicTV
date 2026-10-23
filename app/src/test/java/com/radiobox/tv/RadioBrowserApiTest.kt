package com.radiobox.tv

import com.radiobox.tv.data.api.ApiClient
import com.radiobox.tv.data.api.RadioBrowserApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RadioBrowserApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: RadioBrowserApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ApiClient.radioBrowser(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search stations sends params and parses stations`() = runBlocking {
        val body = """
            [
              {
                "changeuuid":"c1","stationuuid":"s1","serveruuid":null,
                "name":"88FM","url":"http://stream/88fm","url_resolved":"https://stream/88fm",
                "homepage":"http://88fm.co.il","favicon":"http://logo.png","tags":"jazz, news",
                "country":"ישראל","countrycode":"IL","state":"תל אביב","language":"hebrew",
                "votes":42,"lastchangetime":"","codec":"AAC","bitrate":128,"hls":0,
                "lastcheckok":1,"clickcount":7,"clicktrend":0,"geo_lat":null,"geo_long":null
              },
              {
                "stationuuid":"s2","name":"Kan","url":"http://kan","url_resolved":"http://kan",
                "tags":"news","country":"ישראל","countrycode":"IL","state":"","language":"hebrew",
                "votes":9,"codec":"MP3","bitrate":0,"hls":1
              }
            ]
        """.trimIndent()
        server.enqueue(MockResponse().setBody(body).addHeader("Content-Type", "application/json"))

        val stations = api.searchStations(
            mapOf(
                "countrycode" to "IL",
                "tag" to "jazz",
                "hidebroken" to "true",
                "order" to "votes",
                "reverse" to "true",
                "limit" to "40",
                "bitrateMin" to "128",
            )
        )

        val req = server.takeRequest()
        val path = req.path ?: ""
        assertTrue(path.startsWith("/json/stations/search"))
        assertTrue(path.contains("countrycode=IL"))
        assertTrue(path.contains("tag=jazz"))
        assertTrue(path.contains("bitrateMin=128"))

        assertEquals(2, stations.size)
        assertEquals("s1", stations[0].uuid)
        assertEquals("88FM", stations[0].name)
        assertEquals(128, stations[0].bitrate)
        assertEquals("תל אביב", stations[0].state)
        assertEquals("https://stream/88fm", stations[0].streamUrl)
        assertEquals("s2", stations[1].uuid)
        assertTrue(stations[1].isHls)
    }

    @Test
    fun `countries parses iso and counts`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[{"name":"ישראל","iso_3166_1":"IL","stationcount":340}]"""
            ).addHeader("Content-Type", "application/json")
        )
        val countries = api.countries()
        assertEquals(1, countries.size)
        assertEquals("IL", countries[0].iso)
        assertEquals(340, countries[0].stationcount)
        assertEquals("/json/countries", server.takeRequest().path)
    }
}
