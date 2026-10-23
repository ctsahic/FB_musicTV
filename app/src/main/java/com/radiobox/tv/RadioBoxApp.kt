package com.radiobox.tv

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import okhttp3.OkHttpClient
import com.radiobox.tv.data.api.ApiClient
import com.radiobox.tv.data.model.PodcastFav
import com.radiobox.tv.data.model.Station
import com.radiobox.tv.data.repo.FavoritesRepository
import com.radiobox.tv.data.repo.PodcastRepository
import com.radiobox.tv.data.repo.SettingsRepository
import com.radiobox.tv.data.repo.StationsRepository
import com.radiobox.tv.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RadioBoxApp : Application(), ImageLoaderFactory {

    lateinit var settings: SettingsRepository private set
    lateinit var favorites: FavoritesRepository private set
    lateinit var stations: StationsRepository private set
    lateinit var podcasts: PodcastRepository private set
    lateinit var player: PlayerConnection private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        favorites = FavoritesRepository(this)
        stations = StationsRepository(ApiClient.radioBrowser())
        podcasts = PodcastRepository(ApiClient.iTunes())
        player = PlayerConnection(this, settings, stations, favorites)
    }

    /**
     * Shared image loader: adds SVG decoding for station logos, plus an explicit
     * User-Agent — upload.wikimedia.org (logo source for KAN/Glglz/etc.) rejects
     * requests without one, so the default Coil client shows fallback initials.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .okHttpClient(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", "RadioBoxTV/1.0 (Android TV; personal sideload)")
                                .build()
                        )
                    }
                    .build()
            )
            .crossfade(true)
            .build()

    fun removeFavorite(uuid: String) {
        appScope.launch { favorites.removeStation(uuid) }
    }

    fun togglePodcastFavorite(fav: PodcastFav) {
        appScope.launch { favorites.togglePodcast(fav) }
    }

    fun toggleStationFavorite(station: Station) {
        appScope.launch { favorites.toggleStation(station) }
    }

    /** Hides the station from every category list (long-press on a card). */
    fun blockStation(station: Station) {
        appScope.launch {
            favorites.toggleBlocked(station, com.radiobox.tv.core.StationRank.nameKey(station.name))
        }
    }

    fun setCountry(code: String) {
        appScope.launch { settings.setCountry(code) }
    }

    fun setScreensaverSeconds(seconds: Int) {
        appScope.launch { settings.setScreensaverSeconds(seconds) }
    }

    fun setMinBitrate(kbps: Int) {
        appScope.launch { settings.setMinBitrate(kbps) }
    }

    fun setBufferSeconds(seconds: Int) {
        appScope.launch { settings.setBuffer(seconds, seconds * 2) }
    }
}
