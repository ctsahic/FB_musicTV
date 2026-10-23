package com.radiobox.tv.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.data.model.toPodcast
import com.radiobox.tv.data.model.toStation
import com.radiobox.tv.ui.NavState
import com.radiobox.tv.ui.PlayerUi
import com.radiobox.tv.ui.Screen
import com.radiobox.tv.ui.components.ErrorBox
import com.radiobox.tv.ui.components.PodcastCard
import com.radiobox.tv.ui.components.StationCard

@Composable
fun FavoritesScreen(app: RadioBoxApp, nav: NavState) {
    val favStations by app.favorites.favoriteStations.collectAsState(initial = emptyList())
    val favPodcasts by app.favorites.favoritePodcasts.collectAsState(initial = emptyList())
    var tab by remember { mutableIntStateOf(0) }
    val contentFocus = remember { FocusRequester() }

    androidx.compose.runtime.LaunchedEffect(tab, favStations.size, favPodcasts.size) {
        val hasContent = if (tab == 0) favStations.isNotEmpty() else favPodcasts.isNotEmpty()
        if (hasContent) runCatching { contentFocus.requestFocus() }
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
            Text("❤️  מועדפים", fontSize = 26.sp)
        }
        Spacer(Modifier.size(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TabChip("תחנות (${favStations.size})", tab == 0) { tab = 0 }
            TabChip("פודקאסטים (${favPodcasts.size})", tab == 1) { tab = 1 }
        }
        Spacer(Modifier.size(18.dp))

        if (tab == 0) {
            if (favStations.isEmpty()) {
                ErrorBox("אין תחנות מועדפות עדיין.\nלחץ על הלב בכרטיס תחנה כדי לשמור אותה כאן לנצח.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize().focusRequester(contentFocus),
                ) {
                    itemsIndexed(favStations, key = { _, f -> f.uuid }) { _, fav ->
                        StationCard(
                            station = fav.toStation(),
                            isFavorite = true,
                            onClick = { app.player.playStation(fav.toStation()); PlayerUi.show() },
                            onToggleFavorite = { app.removeFavorite(fav.uuid) },
                        )
                    }
                }
            }
        } else {
            if (favPodcasts.isEmpty()) {
                ErrorBox("אין פודקאסטים מועדפים עדיין.\nחפש פודקאסט ולחץ על הלב כדי לשמור אותו כאן.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize().focusRequester(contentFocus),
                ) {
                    itemsIndexed(favPodcasts, key = { _, f -> f.feedUrl }) { _, fav ->
                        PodcastCard(
                            podcast = fav.toPodcast(),
                            onClick = { nav.push(Screen.PodcastDetail(fav.toPodcast())) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) androidx.tv.material3.MaterialTheme.colorScheme.primary
            else androidx.tv.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
