package com.radiobox.tv.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.data.model.Podcast
import com.radiobox.tv.data.model.PodcastFeed
import com.radiobox.tv.data.model.toFav
import com.radiobox.tv.ui.NavState
import com.radiobox.tv.ui.PlayerUi
import com.radiobox.tv.ui.components.EpisodeRow
import com.radiobox.tv.ui.components.ErrorBox
import com.radiobox.tv.ui.components.LoadingBox
import com.radiobox.tv.ui.components.LogoBox
import kotlinx.coroutines.launch

@Composable
fun PodcastDetailScreen(app: RadioBoxApp, nav: NavState, podcast: Podcast) {
    val feed by produceState<PodcastFeed?>(initialValue = null) {
        value = app.podcasts.episodes(podcast.feedUrl)
    }
    val favPodcasts by app.favorites.favoritePodcasts.collectAsState(initial = emptyList())
    val current by app.player.current.collectAsState()
    val isPlaying by app.player.isPlaying.collectAsState()
    val scope = rememberCoroutineScope()

    val f = feed
    val listFocus = androidx.compose.runtime.remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(f) {
        if (f != null && f!!.episodes.isNotEmpty()) runCatching { listFocus.requestFocus() }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(onClick = { nav.pop() }) {
                Box(Modifier.padding(10.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = "חזור",
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Text("פרקים", fontSize = 26.sp)
        }
        Spacer(Modifier.size(18.dp))

        // Header
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoBox(podcast.artworkUrl, Modifier.size(150.dp), fallbackText = podcast.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(podcast.name, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(podcast.artist, color = androidx.tv.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val isFav = favPodcasts.any { it.feedUrl == podcast.feedUrl }
                    Surface(onClick = { app.togglePodcastFavorite(podcast.toFav()) }) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite
                                else Icons.Rounded.FavoriteBorder,
                                contentDescription = "מועדפים",
                                tint = if (isFav) androidx.compose.ui.graphics.Color(0xFFF472B6)
                                else androidx.tv.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(if (isFav) "במועדפים" else "הוסף למועדפים")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.size(18.dp))

        when {
            f == null -> LoadingBox("טוען פרקים…")
            f != null && f!!.episodes.isEmpty() -> ErrorBox("לא נמצאו פרקים בפיד הזה")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.focusRequester(listFocus),
            ) {
                itemsIndexed(f!!.episodes, key = { i, ep -> "$i-${ep.audioUrl}" }) { index, ep ->
                    val isCurrent = current?.id == "podcast:${ep.audioUrl}"
                    EpisodeRow(
                        episode = ep,
                        isCurrent = isCurrent,
                        isPlayingThis = isCurrent && isPlaying,
                        onPlay = {
                            val eps = f!!.episodes
                            app.player.playEpisodes(eps, index, podcast.name, podcast.artworkUrl)
                            PlayerUi.show()
                        },
                    )
                }
            }
        }
    }
}
