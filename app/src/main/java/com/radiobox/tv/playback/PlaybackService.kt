package com.radiobox.tv.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background playback service. Runs as a foreground service (mediaPlayback type),
 * so audio continues while the user is on the TV home screen or the screensaver is showing.
 * All control (play/pause/seek/speed) flows through the MediaSession, which the system
 * exposes on the TV home screen and to the remote's media keys.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var retryJob: Job? = null
    private var retries = 0

    override fun onCreate() {
        super.onCreate()

        val app = application as com.radiobox.tv.RadioBoxApp
        val settings = app.settings.snapshotBlocking()

        val minBuf = (settings.bufferMinSeconds * 1000).coerceAtLeast(1_000)
        val maxBuf = (settings.bufferMaxSeconds * 1000).coerceAtLeast(minBuf + 1_000)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ minBuf,
                /* maxBufferMs = */ maxBuf,
                /* bufferForPlaybackMs = */ 5_000,
                /* bufferForPlaybackAfterRebufferMs = */ 10_000,
            )
            .setBackBuffer(180_000, true)
            .build()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("RadioBoxTV/1.0")
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(8_000)
            .setAllowCrossProtocolRedirects(true)

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                scheduleRetry()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    retries = 0
                    SharedPlaybackState.setReconnecting(false)
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleIntent(i: Intent) {
        if (i.action != ACTION_PLAY) return
        val url = i.getStringExtra(EXTRA_URL) ?: return
        val name = i.getStringExtra(EXTRA_NAME) ?: ""
        val isPodcast = i.getBooleanExtra(EXTRA_IS_PODCAST, false)
        val artwork = i.getStringExtra(EXTRA_ARTWORK)
        val speed = i.getFloatExtra(EXTRA_SPEED, 1f)
        val startMs = i.getLongExtra(EXTRA_START_MS, 0L)

        val player = mediaSession?.player ?: return
        SharedPlaybackState.resetForNewMedia()

        val metadata = MediaMetadata.Builder()
            .setTitle(name)
            .setIsBrowsable(false)
            .setMediaType(
                if (isPodcast) MediaMetadata.MEDIA_TYPE_MUSIC
                else MediaMetadata.MEDIA_TYPE_RADIO_STATION
            )
            .apply { artwork?.let { setArtworkUri(android.net.Uri.parse(it)) } }
            .build()

        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaId(if (isPodcast) "podcast:$url" else "station:$url")
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(item, startMs)
        player.playbackParameters = androidx.media3.common.PlaybackParameters(speed)
        player.prepare()
        player.play()
    }

    private fun scheduleRetry() {
        if (retryJob?.isActive == true) return
        retries++
        if (retries > MAX_RETRIES) {
            SharedPlaybackState.setReconnecting(false)
            return
        }
        SharedPlaybackState.setReconnecting(true)
        retryJob = serviceScope.launch {
            delay(backoffMs(retries))
            val player = mediaSession?.player ?: return@launch
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                player.prepare()
                player.play()
            }
        }
    }

    private fun backoffMs(attempt: Int): Long = (2_000L * attempt).coerceAtMost(20_000L)

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        retryJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.radiobox.tv.action.PLAY"
        const val EXTRA_URL = "url"
        const val EXTRA_NAME = "name"
        const val EXTRA_IS_PODCAST = "is_podcast"
        const val EXTRA_ARTWORK = "artwork"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_START_MS = "start_ms"
        const val MAX_RETRIES = 8
    }
}
