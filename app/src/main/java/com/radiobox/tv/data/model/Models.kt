package com.radiobox.tv.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Raw station record from the Radio Browser API. */
@Serializable
data class RBStation(
    @SerialName("stationuuid") val uuid: String = "",
    val name: String = "",
    val url: String? = null,
    @SerialName("url_resolved") val urlResolved: String? = null,
    val homepage: String? = null,
    val favicon: String? = null,
    val tags: String? = null,
    val country: String? = null,
    @SerialName("countrycode") val countryCode: String? = null,
    val state: String? = null,
    val language: String? = null,
    val votes: Int = 0,
    val codec: String? = null,
    val bitrate: Int = 0,
    val hls: Int = 0,
    @SerialName("lastcheckok") val lastCheckOk: Int = 1,
    val clickcount: Int = 0,
) {
    /** Best playback URL (resolved redirect when available). */
    val streamUrl: String get() = urlResolved.orEmpty().ifBlank { url.orEmpty() }
    val isHls: Boolean get() = hls == 1
}

/** Country entry from /json/countries. */
@Serializable
data class RBCountry(
    val name: String = "",
    @SerialName("iso_3166_1") val iso: String = "",
    val stationcount: Int = 0,
)

/** Minimal UI-facing station used across screens (from API or favorites). */
data class Station(
    val uuid: String,
    val name: String,
    val url: String,
    val logo: String? = null,
    val bitrate: Int = 0,
    val codec: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val state: String? = null,
    val isHls: Boolean = false,
    val tags: String? = null,
) {
    val subtitle: String
        get() = listOfNotNull(country, state?.takeIf { it.isNotBlank() }).joinToString(" · ")

    /** Comma-separated tag list from the directory, lowercased. */
    val tagList: List<String>
        get() = tags.orEmpty().split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
}

fun RBStation.toStation() = Station(
    uuid = uuid,
    name = name.trim(),
    url = streamUrl,
    logo = favicon?.takeIf { it.isNotBlank() },
    bitrate = bitrate,
    codec = codec,
    country = country,
    countryCode = countryCode,
    state = state,
    isHls = isHls,
    tags = tags,
)

/** A podcast found via the iTunes search API. */
@Serializable
data class ITunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ITunesPodcast> = emptyList(),
)

@Serializable
data class ITunesPodcast(
    @SerialName("collectionName") val name: String = "",
    @SerialName("artistName") val artist: String = "",
    @SerialName("feedUrl") val feedUrl: String? = null,
    @SerialName("artworkUrl600") val artworkUrl: String? = null,
    @SerialName("primaryGenreName") val genre: String? = null,
    @SerialName("trackCount") val trackCount: Int = 0,
)

data class Podcast(
    val name: String,
    val artist: String,
    val feedUrl: String,
    val artworkUrl: String? = null,
    val genre: String? = null,
)

fun ITunesPodcast.toPodcast() = feedUrl?.takeIf { it.isNotBlank() }?.let {
    Podcast(
        name = name,
        artist = artist,
        feedUrl = it,
        artworkUrl = artworkUrl,
        genre = genre,
    )
}

/** A single podcast episode parsed from the RSS feed. */
data class PodcastEpisode(
    val title: String,
    val audioUrl: String,
    val pubDateLabel: String? = null,
    val durationSec: Long? = null,
    val description: String? = null,
    val artworkUrl: String? = null,
)

data class PodcastFeed(
    val title: String,
    val artworkUrl: String? = null,
    val episodes: List<PodcastEpisode> = emptyList(),
)

/** Persisted favorite station (self-contained so it can play without a network lookup). */
@Serializable
data class StationFav(
    val uuid: String,
    val name: String,
    val url: String,
    val logo: String? = null,
    val bitrate: Int = 0,
    val codec: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val state: String? = null,
    val isHls: Boolean = false,
)

fun StationFav.toStation() = Station(
    uuid = uuid, name = name, url = url, logo = logo, bitrate = bitrate,
    codec = codec, country = country, countryCode = countryCode, state = state, isHls = isHls,
)

fun Station.toFav() = StationFav(
    uuid = uuid, name = name, url = url, logo = logo, bitrate = bitrate,
    codec = codec, country = country, countryCode = countryCode, state = state, isHls = isHls,
)

/** Persisted favorite podcast. */
@Serializable
data class PodcastFav(
    val name: String,
    val artist: String,
    val feedUrl: String,
    val artworkUrl: String? = null,
    val genre: String? = null,
)

fun Podcast.toFav() = PodcastFav(name, artist, feedUrl, artworkUrl, genre)
fun PodcastFav.toPodcast() = Podcast(name, artist, feedUrl, artworkUrl, genre)
