package com.radiobox.tv

import com.radiobox.tv.data.rss.RssParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xmlpull.v1.XmlPullParserFactory

class RssParserTest {

    private val feedXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" version="2.0">
          <channel>
            <title>הפודקאסט של רדיו</title>
            <description><![CDATA[<p>תיאור עם <b>HTML</b> ומילים רבות</p>]]></description>
            <language>he</language>
            <itunes:image href="https://example.com/cover600.jpg" />
            <image><url>https://example.com/cover.jpg</url></image>
            <item>
              <title>פרק 12 - התחלה</title>
              <pubDate>Wed, 02 Oct 2019 13:00:00 +0000</pubDate>
              <itunes:duration>1:02:30</itunes:duration>
              <itunes:summary>תקציר ראשון</itunes:summary>
              <description>תיאור ארוך יותר</description>
              <enclosure url="https://cdn.example.com/ep12.mp3" length="73800000" type="audio/mpeg" />
            </item>
            <item>
              <title>פרק 11</title>
              <pubDate>Wed, 25 Sep 2019 13:00:00 +0000</pubDate>
              <itunes:duration>2745</itunes:duration>
              <enclosure url="https://cdn.example.com/ep11.m4a" length="44000000" type="audio/mp4" />
              <itunes:image href="https://example.com/ep11-art.jpg" />
            </item>
            <item>
              <title>רק טקסט בלי מדיה</title>
              <description>אין enclosure כאן</description>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    private fun parse(xml: String) = RssParser(XmlPullParserFactory.newInstance()).parse(xml.reader())

    @Test
    fun `parses channel title and artwork`() {
        val feed = parse(feedXml)
        assertEquals("הפודקאסט של רדיו", feed.title)
        assertEquals("https://example.com/cover600.jpg", feed.artworkUrl)
    }

    @Test
    fun `parses episodes with durations`() {
        val feed = parse(feedXml)
        assertEquals(2, feed.episodes.size)
        val ep1 = feed.episodes[0]
        assertEquals("פרק 12 - התחלה", ep1.title)
        assertEquals(3750L, ep1.durationSec) // 1:02:30
        assertEquals("https://cdn.example.com/ep12.mp3", ep1.audioUrl)
        assertNotNull(ep1.pubDateLabel)
        // summary wins over description
        assertEquals("תקציר ראשון", ep1.description)

        val ep2 = feed.episodes[1]
        assertEquals(2745L, ep2.durationSec) // plain seconds
        assertEquals("https://example.com/ep11-art.jpg", ep2.artworkUrl)
    }

    @Test
    fun `skips items without audio enclosure`() {
        val feed = parse(feedXml)
        assertTrue(feed.episodes.none { it.title == "רק טקסט בלי מדיה" })
    }

    @Test
    fun `handles empty feed`() {
        val feed = parse("<rss version=\"2.0\"><channel><title>x</title></channel></rss>")
        assertEquals("x", feed.title)
        assertEquals(0, feed.episodes.size)
        assertNull(feed.artworkUrl)
    }
}
