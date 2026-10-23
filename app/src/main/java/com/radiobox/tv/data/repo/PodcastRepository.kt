package com.radiobox.tv.data.repo

import com.radiobox.tv.data.api.ITunesApi
import com.radiobox.tv.data.model.Podcast
import com.radiobox.tv.data.model.PodcastEpisode
import com.radiobox.tv.data.model.PodcastFeed
import com.radiobox.tv.data.model.toPodcast
import com.radiobox.tv.data.rss.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PodcastRepository(private val iTunes: ITunesApi) {

    private val parser = RssParser()
    private val feedCache = LinkedHashMap<String, PodcastFeed>()

    suspend fun search(term: String, limit: Int = 25): List<Podcast> = withContext(Dispatchers.IO) {
        runCatching { iTunes.searchPodcasts(term, limit = limit) }
            .getOrElse { com.radiobox.tv.data.api.ITunesSearchResponseWire() }
            .results
            .mapNotNull { it.toPodcast() }
            .filter { it.feedUrl.isNotBlank() }
    }

    /** Preset searches so the screen is never empty without typing. */
    suspend fun hebrewHighlights(): List<Podcast> = withContext(Dispatchers.IO) {
        val terms = listOf("פודקאסט", "ישראל", "היסטוריה ישראלית")
        terms.flatMap { t -> runCatching { iTunes.searchPodcasts(t, limit = 10) }.getOrDefault(com.radiobox.tv.data.api.ITunesSearchResponseWire()).results }
            .mapNotNull { it.toPodcast() }
            .distinctBy { it.feedUrl }
            .take(24)
    }

    suspend fun episodes(feedUrl: String): PodcastFeed = withContext(Dispatchers.IO) {
        feedCache[feedUrl]?.let { return@withContext it }
        val feed = runCatching {
            val request = okhttp3.Request.Builder().url(feedUrl).build()
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            client.newCall(request).execute().use { resp ->
                check(resp.isSuccessful) { "HTTP ${resp.code}" }
                parser.parse(resp.body!!.charStream())
            }
        }.getOrElse { PodcastFeed(title = "", episodes = emptyList()) }
        val deduped = feed.copy(episodes = feed.episodes.distinctBy { it.audioUrl })
        feedCache[feedUrl] = deduped
        deduped
    }

    fun currentEpisodes(feedUrl: String): List<PodcastEpisode> = feedCache[feedUrl]?.episodes ?: emptyList()
}
