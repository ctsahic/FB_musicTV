package com.radiobox.tv.data.rss

import com.radiobox.tv.core.Utils
import com.radiobox.tv.data.model.PodcastEpisode
import com.radiobox.tv.data.model.PodcastFeed
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader

/**
 * Minimal podcast RSS parser (RSS 2.0 + itunes namespace).
 * Uses local attribute names so it tolerates any namespace URI / prefix.
 */
class RssParser(private val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()) {

    fun parse(reader: Reader): PodcastFeed {
        val parser = factory.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(reader)
        }

        var channelTitle = ""
        var channelArt: String? = null
        val episodes = mutableListOf<PodcastEpisode>()

        var inItem = false
        var inImage = false

        // Current item accumulators
        var title = ""
        var enclosureUrl = ""
        var pubDate: String? = null
        var durationRaw: String? = null
        var description: String? = null
        var itemArt: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.lowercase().substringAfterLast(':')
                    when {
                        name == "item" -> {
                            inItem = true
                            title = ""; enclosureUrl = ""; pubDate = null
                            durationRaw = null; description = null; itemArt = null
                        }
                        name == "title" -> {
                            if (inItem) title = clean(parser.nextTextSafe())
                            else if (inImage) { /* <image><title> of channel, ignore */ }
                            else channelTitle = clean(parser.nextTextSafe())
                        }
                        name == "enclosure" && inItem -> {
                            val type = attr(parser, "type")
                            val url = attr(parser, "url")
                            if (url != null && (type == null || type.startsWith("audio") || type.isEmpty())) {
                                if (enclosureUrl.isEmpty()) enclosureUrl = url
                            } else if (url != null && enclosureUrl.isEmpty()) {
                                enclosureUrl = url
                            }
                        }
                        name == "pubdate" && inItem -> pubDate = parser.nextTextSafe()
                        name == "duration" && inItem -> durationRaw = parser.nextTextSafe()
                        name == "summary" && inItem -> if (description.isNullOrBlank()) description = parser.nextTextSafe()
                        name == "description" && inItem -> if (description.isNullOrBlank()) description = parser.nextTextSafe()
                        name == "image" && !inItem -> {
                            // itunes:image uses an href attribute; RSS <image> wraps a <url> child
                            val href = attr(parser, "href")
                            if (!href.isNullOrBlank()) channelArt = href else inImage = true
                        }
                        name == "image" && inItem -> attr(parser, "href")?.let { itemArt = it }
                        name == "url" && inImage -> if (channelArt.isNullOrBlank()) channelArt = parser.nextTextSafe()
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name.lowercase().substringAfterLast(':')
                    when {
                        name == "item" -> {
                            inItem = false
                            if (enclosureUrl.isNotBlank()) {
                                episodes += PodcastEpisode(
                                    title = title.ifBlank { "ללא כותרת" },
                                    audioUrl = enclosureUrl,
                                    pubDateLabel = Utils.formatPubDate(pubDate),
                                    durationSec = Utils.parseDurationSec(durationRaw),
                                    description = description?.let { clean(it) },
                                    artworkUrl = itemArt,
                                )
                            }
                        }
                        name == "image" && !inItem -> inImage = false
                    }
                }
            }
            event = parser.next()
        }

        return PodcastFeed(
            title = channelTitle,
            artworkUrl = channelArt ?: episodes.firstNotNullOfOrNull { it.artworkUrl },
            episodes = episodes,
        )
    }

    private fun attr(p: XmlPullParser, localName: String): String? {
        for (i in 0 until p.attributeCount) {
            if (p.getAttributeName(i).equals(localName, ignoreCase = true)) return p.getAttributeValue(i)
        }
        return null
    }

    private fun XmlPullParser.nextTextSafe(): String = try {
        nextText()
    } catch (_: Exception) {
        ""
    }

    /** Strips HTML tags + entities and collapses whitespace. */
    private fun clean(s: String): String = s
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(600)
}
