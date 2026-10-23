package com.radiobox.tv.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.core.Genre
import com.radiobox.tv.core.Utils
import com.radiobox.tv.data.model.Station
import com.radiobox.tv.ui.NavState
import com.radiobox.tv.ui.PlayerUi
import com.radiobox.tv.ui.components.ErrorBox
import com.radiobox.tv.ui.components.FocusableCard
import com.radiobox.tv.ui.components.LoadingBox
import com.radiobox.tv.ui.components.StationCard
import com.radiobox.tv.ui.theme.TextPrimary
import com.radiobox.tv.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private sealed interface StationsUi {
    data object Loading : StationsUi
    data object Empty : StationsUi
    data class Loaded(val stations: List<Station>) : StationsUi
    data class Error(val message: String) : StationsUi
}

@Composable
fun StationsScreen(app: RadioBoxApp, nav: NavState, genre: Genre) {
    val settings by app.settings.settings.collectAsState(initial = com.radiobox.tv.data.repo.Settings())
    val favStations by app.favorites.favoriteStations.collectAsState(initial = emptyList())
    val blocked by app.favorites.blockedStations.collectAsState(initial = emptySet())
    var ui by remember { mutableStateOf<StationsUi>(StationsUi.Loading) }
    var reloadKey by remember { mutableStateOf(0) }
    var blockTarget by remember { mutableStateOf<Station?>(null) }
    val scope = rememberCoroutineScope()
    val gridFocus = remember { FocusRequester() }

    LaunchedEffect(ui) {
        if (ui is StationsUi.Loaded) runCatching { gridFocus.requestFocus() }
    }

    fun load() {
        ui = StationsUi.Loading
        scope.launch {
            try {
                val list = app.stations.stationsForGenre(genre, settings.countryCode, settings.minBitrate, blocked)
                ui = if (list.isEmpty()) StationsUi.Empty else StationsUi.Loaded(list)
            } catch (e: Exception) {
                ui = StationsUi.Error(e.message ?: "שגיאת רשת")
            }
        }
    }

    LaunchedEffect(genre.id, settings.countryCode, settings.minBitrate, reloadKey, blocked) { load() }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FocusableCard(onClick = { nav.pop() }, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                Box(Modifier.padding(12.dp)) {
                    Text("→", fontSize = 22.sp, color = TextPrimary)
                }
            }
            Text(genre.emoji, fontSize = 28.sp)
            Column {
                Text(genre.label, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "${Utils.flagEmoji(settings.countryCode)} ${Utils.countryLabel(settings.countryCode)} · איכות ${settings.minBitrate}kbps+",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            Text("${Utils.flagEmoji(settings.countryCode)} ${settings.countryCode}", color = TextSecondary)
        }
        Spacer(Modifier.height(18.dp))

        when (val s = ui) {
            StationsUi.Loading -> LoadingBox("טוען תחנות…")
            StationsUi.Empty -> ErrorBox(
                "לא נמצאו תחנות עבור \"${genre.label}\" במדינה הנבחרת\nעם איכות מינימלית של ${settings.minBitrate}kbps.\nנסה מדינה אחרת או הורד את סף האיכות בהגדרות.",
                onRetry = { reloadKey++ },
            )
            is StationsUi.Error -> ErrorBox("שגיאת רשת: ${s.message}", onRetry = { reloadKey++ })
            is StationsUi.Loaded -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize().focusRequester(gridFocus),
            ) {
                itemsIndexed(s.stations, key = { _, st -> st.uuid.ifBlank { st.url } }) { _, station ->
                    val isFav = favStations.any { it.uuid == station.uuid }
                    StationCard(
                        station = station,
                        isFavorite = isFav,
                        onClick = {
                            app.player.playStation(station)
                            PlayerUi.show()
                        },
                        onToggleFavorite = { app.toggleStationFavorite(station) },
                        onBlock = { blockTarget = station },
                    )
                }
            }
        }
    }

    // Long-press confirm dialog: hide the station from every category list.
    blockTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { blockTarget = null },
            title = { Text("להסתיר את התחנה?") },
            text = {
                Text("\"${target.name}\" תוסר מכל רשימות הקטגוריות ומהמועדפים.\nאפשר לשחזר רק על-ידי ביטול החסימה בהגדרות.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    app.blockStation(target)
                    blockTarget = null
                }) { Text("הסתר") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { blockTarget = null }) { Text("ביטול") }
            },
            containerColor = androidx.compose.ui.graphics.Color(0xFF232637),
        )
    }
}
