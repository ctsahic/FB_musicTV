package com.radiobox.tv.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.radiobox.tv.data.model.PodcastFav
import com.radiobox.tv.data.model.Station
import com.radiobox.tv.data.model.StationFav
import com.radiobox.tv.data.model.toFav
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.favoritesStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

/** JSON (de)serialization of favorite payloads - extracted for unit testing. */
object FavoritesCodec {
    val json = Json { ignoreUnknownKeys = true }
    private val stationList = ListSerializer(StationFav.serializer())
    private val podcastList = ListSerializer(PodcastFav.serializer())

    fun encodeStations(favs: List<StationFav>): String = json.encodeToString(stationList, favs)

    fun decodeStations(raw: String?): List<StationFav> = runCatching {
        raw?.takeIf { it.isNotBlank() }?.let { json.decodeFromString(stationList, it) }
    }.getOrNull() ?: emptyList()

    fun encodePodcasts(favs: List<PodcastFav>): String = json.encodeToString(podcastList, favs)

    fun decodePodcasts(raw: String?): List<PodcastFav> = runCatching {
        raw?.takeIf { it.isNotBlank() }?.let { json.decodeFromString(podcastList, it) }
    }.getOrNull() ?: emptyList()

    fun encodeLastStation(fav: StationFav): String = json.encodeToString(StationFav.serializer(), fav)

    fun decodeLastStation(raw: String?): StationFav? = runCatching {
        raw?.takeIf { it.isNotBlank() }?.let { json.decodeFromString(StationFav.serializer(), it) }
    }.getOrNull()
}

class FavoritesRepository(private val context: Context) {

    private object K {
        val STATIONS = stringPreferencesKey("fav_stations_json")
        val PODCASTS = stringPreferencesKey("fav_podcasts_json")
        val LAST = stringPreferencesKey("last_station_json")
        val BLOCKED = stringSetPreferencesKey("blocked_stations")
    }

    /**
     * Blocked stations, stored as both UUIDs and normalized names so every
     * directory mirror of the station disappears from all category lists.
     */
    val blockedStations: Flow<Set<String>> =
        context.favoritesStore.data.map { it[K.BLOCKED] ?: emptySet() }.distinctUntilChanged()

    val favoriteStations: Flow<List<StationFav>> =
        context.favoritesStore.data.map { FavoritesCodec.decodeStations(it[K.STATIONS]) }.distinctUntilChanged()

    val favoritePodcasts: Flow<List<PodcastFav>> =
        context.favoritesStore.data.map { FavoritesCodec.decodePodcasts(it[K.PODCASTS]) }.distinctUntilChanged()

    val lastStation: Flow<StationFav?> =
        context.favoritesStore.data.map { FavoritesCodec.decodeLastStation(it[K.LAST]) }.distinctUntilChanged()

    fun isFavorite(uuid: String): Flow<Boolean> =
        favoriteStations.map { list -> list.any { it.uuid == uuid } }.distinctUntilChanged()

    /** Toggles and returns the new state. */
    suspend fun toggleStation(station: Station): Boolean {
        var added = false
        context.favoritesStore.edit { p ->
            val current = FavoritesCodec.decodeStations(p[K.STATIONS]).toMutableList()
            val existing = current.indexOfFirst { it.uuid == station.uuid }
            if (existing >= 0) current.removeAt(existing) else { current.add(0, station.toFav()); added = true }
            p[K.STATIONS] = FavoritesCodec.encodeStations(current)
        }
        return added
    }

    suspend fun removeStation(uuid: String) = context.favoritesStore.edit { p ->
        val current = FavoritesCodec.decodeStations(p[K.STATIONS]).filterNot { it.uuid == uuid }
        p[K.STATIONS] = FavoritesCodec.encodeStations(current)
    }

    /** Toggles a podcast favorite and returns the new state. */
    suspend fun togglePodcast(fav: PodcastFav): Boolean {
        var added = false
        context.favoritesStore.edit { p ->
            val current = FavoritesCodec.decodePodcasts(p[K.PODCASTS]).toMutableList()
            val existing = current.indexOfFirst { it.feedUrl == fav.feedUrl }
            if (existing >= 0) current.removeAt(existing) else { current.add(0, fav); added = true }
            p[K.PODCASTS] = FavoritesCodec.encodePodcasts(current)
        }
        return added
    }

    suspend fun rememberLastStation(station: Station) = context.favoritesStore.edit { p ->
        p[K.LAST] = FavoritesCodec.encodeLastStation(station.toFav())
    }

    /** Hides a station everywhere (all category lists) and returns true when now blocked. */
    suspend fun toggleBlocked(station: Station, nameKey: String): Boolean {
        var blocked = false
        context.favoritesStore.edit { p ->
            val current = (p[K.BLOCKED] ?: emptySet()).toMutableSet()
            if (current.contains(station.uuid) || current.contains(nameKey)) {
                current.remove(station.uuid); current.remove(nameKey)
            } else {
                current.add(station.uuid); current.add(nameKey); blocked = true
            }
            p[K.BLOCKED] = current
            // Blocking also removes it from favorites / last-station.
            val favs = FavoritesCodec.decodeStations(p[K.STATIONS]).filterNot {
                it.uuid == station.uuid || com.radiobox.tv.core.StationRank.nameKey(it.name) == nameKey
            }
            p[K.STATIONS] = FavoritesCodec.encodeStations(favs)
            if (FavoritesCodec.decodeLastStation(p[K.LAST])?.uuid == station.uuid) {
                p.remove(K.LAST)
            }
        }
        return blocked
    }
}
