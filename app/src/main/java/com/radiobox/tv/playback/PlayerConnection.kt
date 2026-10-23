package com.radiobox.tv.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.radiobox.tv.data.model.PodcastEpisode
import com.radiobox.tv.data.model.Station
import com.radiobox.tv.data.repo.FavoritesRepository
import com.radiobox.tv.data.repo.SettingsRepository
import com.radiobox.tv.data.repo.StationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** What the UI needs to know about the thing that is currently playing. */
data class NowPlaying(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val artwork: String? = null,
    val url: String? = null,
    val isLive: Boolean = true,
    val isPodcast: Boolean = false,
    val isHls: Boolean = false,
)

/**
 * App-side handle to the background playback session.
 * All commands and state go through a [MediaController] bound to [PlaybackService].
 */
class PlayerConnection(
    context: Context,
    private val settings: SettingsRepository,
    private val stationsRepo: StationsRepository? = null,
    private val favorites: FavoritesRepository? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var ticker: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _current = MutableStateFlow<NowPlaying?>(null)
    val current: StateFlow<NowPlaying?> = _current

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(C.TIME_UNSET)
    val durationMs: StateFlow<Long> = _durationMs

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        scope.launch {
            val c = MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, PlaybackService::class.java)),
            ).buildAsync().await()
            controller = c
            attach(c)
        }
    }

    private fun attach(c: MediaController) {
        c.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startTicker() else stopTicker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) _error.value = null
                refreshTimeline()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                SharedPlaybackState.resetForNewMedia()
                refreshCurrent()
                refreshTimeline()
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                refreshCurrent()
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                _speed.value = playbackParameters.speed
            }

            override fun onPlayerError(error: PlaybackException) {
                _error.value = error.errorCodeName.substringAfterLast('_')
                    .replace('_', ' ')
                    .lowercase()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                refreshTimeline()
            }

            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val entry = metadata[i]
                    if (entry is IcyInfo) SharedPlaybackState.setStreamTitle(entry.title)
                }
            }
        })
        refreshCurrent()
        refreshTimeline()
        _isPlaying.value = c.isPlaying
    }

    private fun refreshCurrent() {
        val c = controller ?: return
        val item = c.currentMediaItem ?: run {
            _current.value = null
            return
        }
        val isPodcast = item.mediaId.startsWith("podcast:")
        val live = item.localConfiguration != null && !isPodcast
        _current.value = NowPlaying(
            id = item.mediaId,
            title = c.mediaMetadata.title?.toString() ?: "",
            subtitle = c.mediaMetadata.artist?.toString(),
            artwork = c.mediaMetadata.artworkUri?.toString(),
            url = item.localConfiguration?.uri?.toString(),
            isLive = live,
            isPodcast = isPodcast,
            isHls = item.localConfiguration?.mimeType == MimeTypes.APPLICATION_M3U8,
        )
    }

    private fun refreshTimeline() {
        val c = controller ?: return
        _durationMs.value = c.duration
        _positionMs.value = if (c.duration == C.TIME_UNSET) 0L else c.currentPosition.coerceAtLeast(0)
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val c = controller ?: break
                if (c.duration != C.TIME_UNSET) {
                    _positionMs.value = c.currentPosition.coerceAtLeast(0)
                }
                delay(500)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    // ---- Commands ----

    fun playStation(station: Station) {
        val c = controller ?: return
        if (station.url.isBlank()) return
        val item = MediaItem.Builder()
            .setUri(station.url)
            .setMediaId("station:${station.uuid}")
            .setMimeType(if (station.isHls) MimeTypes.APPLICATION_M3U8 else null)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.subtitle.ifBlank { null })
                    .apply { station.logo?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                    .build()
            )
            .build()
        SharedPlaybackState.resetForNewMedia()
        c.setMediaItem(item)
        c.playbackParameters = PlaybackParameters(1f)
        c.prepare()
        c.play()
        _error.value = null
        stationsRepo?.registerClick(station.uuid)
        scope.launch { runCatching { favorites?.rememberLastStation(station) } }
    }

    fun playEpisodes(episodes: List<PodcastEpisode>, startIndex: Int, podcastName: String, podcastArt: String?) {
        val c = controller ?: return
        if (episodes.isEmpty()) return
        val items = episodes.map { ep ->
            MediaItem.Builder()
                .setUri(ep.audioUrl)
                .setMediaId("podcast:${ep.audioUrl}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(ep.title)
                        .setArtist(podcastName)
                        .apply { (ep.artworkUrl ?: podcastArt)?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                        .build()
                )
                .build()
        }
        SharedPlaybackState.resetForNewMedia()
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        scope.launch {
            val s = settings.settings.first()
            c.playbackParameters = PlaybackParameters(s.podcastSpeed)
            c.prepare()
            c.play()
        }
        _error.value = null
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE && c.mediaItemCount > 0) c.prepare()
            c.play()
        }
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        SharedPlaybackState.resetForNewMedia()
        _current.value = null
    }

    fun seekTo(ms: Long) {
        val c = controller ?: return
        if (c.duration != C.TIME_UNSET) c.seekTo(ms.coerceIn(0, c.duration.coerceAtLeast(0)))
    }

    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        if (c.duration == C.TIME_UNSET) return
        val target = (c.currentPosition + deltaMs).coerceIn(0, c.duration)
        c.seekTo(target)
    }

    fun setSpeed(speed: Float) {
        val c = controller ?: return
        c.playbackParameters = PlaybackParameters(speed)
        scope.launch { settings.setPodcastSpeed(speed) }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }
}
