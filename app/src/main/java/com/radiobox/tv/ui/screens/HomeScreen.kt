package com.radiobox.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.core.Genre
import com.radiobox.tv.core.Genres
import com.radiobox.tv.core.Utils
import com.radiobox.tv.data.model.toPodcast
import com.radiobox.tv.data.model.toStation
import com.radiobox.tv.playback.SharedPlaybackState
import com.radiobox.tv.ui.NavState
import com.radiobox.tv.ui.PlayerUi
import com.radiobox.tv.ui.Screen
import com.radiobox.tv.ui.ScreensaverController
import com.radiobox.tv.ui.components.EqualizerBars
import com.radiobox.tv.ui.components.FocusableCard
import com.radiobox.tv.ui.components.LogoBox
import com.radiobox.tv.ui.components.MiniEqualizer
import com.radiobox.tv.ui.components.PodcastCard
import com.radiobox.tv.ui.components.SectionTitle
import com.radiobox.tv.ui.components.StationCard
import com.radiobox.tv.ui.components.gradientFor
import com.radiobox.tv.ui.theme.Purple
import com.radiobox.tv.ui.theme.TextPrimary
import com.radiobox.tv.ui.theme.TextSecondary

@Composable
fun HomeScreen(app: RadioBoxApp, nav: NavState) {
    val settings by app.settings.settings.collectAsState(initial = com.radiobox.tv.data.repo.Settings())
    val current by app.player.current.collectAsState()
    val isPlaying by app.player.isPlaying.collectAsState()
    val streamTitle by SharedPlaybackState.streamTitle.collectAsState()
    val lastStation by app.favorites.lastStation.collectAsState(initial = null)
    val favStations by app.favorites.favoriteStations.collectAsState(initial = emptyList())
    val favPodcasts by app.favorites.favoritePodcasts.collectAsState(initial = emptyList())

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // ---- Header ----
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Purple, Color(0xFFDB2777)))),
                contentAlignment = Alignment.Center,
            ) {
                Text("📻", fontSize = 22.sp)
            }
            Text("רדיו־בוקס TV", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            FocusableCard(
                onClick = { nav.push(Screen.CountryPicker) },
                shape = RoundedCornerShape(12.dp),
                accent = Color(0xFF2DD4BF),
            ) {
                Text(
                    "${Utils.flagEmoji(settings.countryCode)}  ${Utils.countryLabel(settings.countryCode)}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }
            HeaderIconButton(Icons.Rounded.DarkMode, "שומר מסך") {
                ScreensaverController.show()
            }
            HeaderIconButton(Icons.Rounded.Settings, "הגדרות") {
                nav.push(Screen.Settings)
            }
        }

        // ---- Now playing bar ----
        val cur = current
        if (cur != null) {
            FocusableCard(
                onClick = { PlayerUi.show() },
                modifier = Modifier.fillMaxWidth(),
                accent = Purple,
            ) {
                Row(
                    Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(Purple.copy(alpha = 0.16f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    LogoBox(cur.artwork, Modifier.size(64.dp), fallbackText = cur.title)
                    Column(Modifier.weight(1f)) {
                        Text("מנגן כעת", color = Purple, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(cur.title, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val sub = streamTitle ?: cur.subtitle
                        if (!sub.isNullOrBlank()) {
                            Text(sub, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    EqualizerBars(playing = isPlaying)
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause
                        else Icons.Rounded.PlayArrow,
                        contentDescription = "נגן/השהה",
                        tint = Purple,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        } else {
            val last = lastStation
            if (last != null) {
                FocusableCard(
                    onClick = { app.player.playStation(last.toStation()); PlayerUi.show() },
                    modifier = Modifier.fillMaxWidth(),
                    accent = Color(0xFF2DD4BF),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LogoBox(last.logo, Modifier.size(56.dp), fallbackText = last.name)
                        Column {
                            Text("המשך להאזין", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(last.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "נגן",
                            tint = Purple,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }
        }

        // ---- Genres ----
        SectionTitle("סגנונות")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Genres.ALL.forEach { genre -> GenreTile(genre) { nav.push(Screen.Stations(genre)) } }
        }

        // ---- Favorites ----
        if (favStations.isNotEmpty()) {
            SectionTitle("המועדפים שלי (${favStations.size})")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                favStations.take(12).forEach { fav ->
                    StationCard(
                        station = fav.toStation(),
                        isFavorite = true,
                        onClick = { app.player.playStation(fav.toStation()); PlayerUi.show() },
                        onToggleFavorite = { app.removeFavorite(fav.uuid) },
                    )
                }
            }
        }

        // ---- Podcasts ----
        SectionTitle("פודקאסטים")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FocusableCard(
                onClick = { nav.push(Screen.Podcasts) },
                modifier = Modifier.width(200.dp),
                accent = Color(0xFF2DD4BF),
            ) {
                Column(
                    Modifier.padding(vertical = 40.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color(0xFF2DD4BF),
                        modifier = Modifier.size(34.dp),
                    )
                    Text("חפש פודקאסטים", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
            favPodcasts.take(8).forEach { p ->
                PodcastCard(podcast = p.toPodcast(), onClick = { nav.push(Screen.PodcastDetail(p.toPodcast())) })
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

/** Colorful gradient genre tile. */
@Composable
private fun GenreTile(genre: Genre, onClick: () -> Unit) {
    FocusableCard(onClick = onClick, modifier = Modifier.width(150.dp), accent = Color.White.copy(alpha = 0.6f)) {
        Column(
            Modifier
                .background(gradientFor(genre.id))
                .padding(vertical = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(genre.emoji, fontSize = 36.sp)
            Text(
                genre.label,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isPlayingGenre(genre.id)) MiniEqualizer(playing = true)
        }
    }
}

@Composable
private fun isPlayingGenre(genreId: String): Boolean = false

@Composable
private fun HeaderIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    com.radiobox.tv.ui.components.HeaderIconButton(icon = icon, desc = desc, onClick = onClick)
}
