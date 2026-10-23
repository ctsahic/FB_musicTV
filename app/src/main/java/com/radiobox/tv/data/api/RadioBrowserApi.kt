package com.radiobox.tv.data.api

import com.radiobox.tv.data.model.RBCountry
import com.radiobox.tv.data.model.RBStation
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.util.concurrent.TimeUnit

interface RadioBrowserApi {

    @GET("json/countries")
    suspend fun countries(): List<RBCountry>

    @GET("json/stations/search")
    suspend fun searchStations(@QueryMap params: Map<String, String>): List<RBStation>

    @GET("json/stations/byuuid")
    suspend fun stationsByUuid(@Query("uuids") uuids: String): List<RBStation>

    @GET("json/url/{uuid}")
    suspend fun registerClick(@Path("uuid") uuid: String)
}

interface ITunesApi {

    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("limit") limit: Int = 25,
        @Query("country") country: String = "IL",
    ): ITunesSearchResponseWire
}

/** Wire model kept separate so the converter can parse text/javascript responses. */
@kotlinx.serialization.Serializable
data class ITunesSearchResponseWire(
    val resultCount: Int = 0,
    val results: List<com.radiobox.tv.data.model.ITunesPodcast> = emptyList(),
)

object ApiClient {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun radioBrowser(baseUrl: String = "https://de1.api.radio-browser.info/"): RadioBrowserApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RadioBrowserApi::class.java)

    fun iTunes(): ITunesApi = Retrofit.Builder()
        .baseUrl("https://itunes.apple.com/")
        .client(okHttp())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ITunesApi::class.java)
}
