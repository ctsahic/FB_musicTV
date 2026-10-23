package com.radiobox.tv.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val countryCode: String = "IL",
    val screensaverEnabled: Boolean = true,
    val screensaverSeconds: Int = 300,          // 5 minutes
    val minBitrate: Int = 128,
    val bufferMinSeconds: Int = 60,
    val bufferMaxSeconds: Int = 120,
    val podcastSpeed: Float = 1.0f,
)

class SettingsRepository(private val context: Context) {

    private object K {
        val COUNTRY = stringPreferencesKey("country_code")
        val SAVER_ENABLED = booleanPreferencesKey("screensaver_enabled")
        val SAVER_SECONDS = intPreferencesKey("screensaver_seconds")
        val MIN_BITRATE = intPreferencesKey("min_bitrate")
        val BUFFER_MIN = intPreferencesKey("buffer_min_seconds")
        val BUFFER_MAX = intPreferencesKey("buffer_max_seconds")
        val SPEED = floatPreferencesKey("podcast_speed")
    }

    val settings: Flow<Settings> = context.settingsStore.data.map { p ->
        Settings(
            countryCode = p[K.COUNTRY] ?: "IL",
            screensaverEnabled = p[K.SAVER_ENABLED] ?: true,
            screensaverSeconds = p[K.SAVER_SECONDS] ?: 300,
            minBitrate = p[K.MIN_BITRATE] ?: 128,
            bufferMinSeconds = p[K.BUFFER_MIN] ?: 60,
            bufferMaxSeconds = p[K.BUFFER_MAX] ?: 120,
            podcastSpeed = p[K.SPEED] ?: 1.0f,
        )
    }

    /** Blocking read used once when the playback service starts. */
    fun snapshotBlocking(): Settings = kotlinx.coroutines.runBlocking {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { settings.first() }
    }

    suspend fun setCountry(code: String) = context.settingsStore.edit { it[K.COUNTRY] = code }
    suspend fun setScreensaverEnabled(enabled: Boolean) = context.settingsStore.edit { it[K.SAVER_ENABLED] = enabled }
    suspend fun setScreensaverSeconds(seconds: Int) = context.settingsStore.edit { it[K.SAVER_SECONDS] = seconds }
    suspend fun setMinBitrate(kbps: Int) = context.settingsStore.edit { it[K.MIN_BITRATE] = kbps }
    suspend fun setBuffer(minSec: Int, maxSec: Int) = context.settingsStore.edit {
        it[K.BUFFER_MIN] = minSec
        it[K.BUFFER_MAX] = maxOf(minSec, maxSec)
    }
    suspend fun setPodcastSpeed(speed: Float) = context.settingsStore.edit { it[K.SPEED] = speed }
}
