package com.radiobox.tv.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.core.Utils
import com.radiobox.tv.data.model.Station
import com.radiobox.tv.playback.SharedPlaybackState
import com.radiobox.tv.ui.ScreensaverController
import com.radiobox.tv.ui.components.Badge
import com.radiobox.tv.ui.components.LogoBox
import com.radiobox.tv.ui.theme.Purple
import com.radiobox.tv.ui.theme.Teal
import com.radiobox.tv.ui.theme.TextSecondary

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    sizeDp: Int = 64,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Surface(onClick = onClick, modifier = modifier.size(sizeDp.dp)) {
        Box(
            Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size((sizeDp * 0.5f).dp))
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.15f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Purple)
        )
    }
}

@Composable
fun PlayerScreen(app: RadioBoxApp, onClose: () -> Unit) {
    val current by app.player.current.collectAsState()
    val isPlaying by app.player.isPlaying.collectAsState()
    val isBuffering by app.player.isBuffering.collectAsState()
    val positionMs by app.player.positionMs.collectAsState()
    val durationMs by app.player.durationMs.collectAsState()
    val speed by app.player.speed.collectAsState()
    val error by app.player.error.collectAsState()
    val reconnecting by SharedPlaybackState.reconnecting.collectAsState()
    val streamTitle by SharedPlaybackState.streamTitle.collectAsState()
    val favStations by app.favorites.favoriteStations.collectAsState(initial = emptyList())

    val cur = current ?: return
    val focusRequester = remember { FocusRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val stationUuid = cur.id.removePrefix("station:")
    val isFavStation = favStations.any { it.uuid == stationUuid }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF191227), Color(0xFF0E0F16))))
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 60.dp, vertical = 32.dp)) {

            // ---- Header ----
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundButton(
                    icon = Icons.Rounded.Close,
                    description = "חזור",
                    onClick = onClose,
                    sizeDp = 48,
                    tint = TextSecondary,
                )
                Text("מנגן כעת", style = androidx.tv.material3.MaterialTheme.typography.titleLarge, color = TextSecondary)
                Spacer(Modifier.weight(1f))
                if (reconnecting) Badge("מתחבר מחדש…", color = Color(0xFFFBBF24))
                if (error != null) Badge("שגיאה", color = Color(0xFFF87171))
                if (cur.isLive) Badge("שידור חי", color = Teal)
            }

            Spacer(Modifier.weight(1f))

            // ---- Center: artwork + info ----
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                LogoBox(cur.artwork, Modifier.size(230.dp), fallbackText = cur.title)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        cur.title,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (cur.isLive && !streamTitle.isNullOrBlank()) {
                        Text("🎤 $streamTitle", fontSize = 20.sp, color = Teal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    } else {
                        cur.subtitle?.let {
                            Text(it, fontSize = 18.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    when {
                        isBuffering -> Text("טוען באפר…", color = TextSecondary, fontSize = 16.sp)
                        reconnecting -> Text("מתחבר מחדש…", color = Color(0xFFFBBF24), fontSize = 16.sp)
                        error != null -> Text("שגיאה: $error", color = Color(0xFFF87171), fontSize = 16.sp)
                    }

                    if (!cur.isLive && durationMs > 0) {
                        Spacer(Modifier.height(6.dp))
                        ProgressBar(positionMs.toFloat() / durationMs.toFloat())
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Utils.formatMs(positionMs), color = TextSecondary, style = androidx.tv.material3.MaterialTheme.typography.labelSmall)
                            Text(Utils.formatMs(durationMs), color = TextSecondary, style = androidx.tv.material3.MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ---- Controls ----
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {

                if (!cur.isPodcast) {
                    RoundButton(
                        icon = if (isFavStation) Icons.Rounded.Favorite
                        else Icons.Rounded.FavoriteBorder,
                        description = "מועדפים",
                        onClick = {
                            if (isFavStation) {
                                app.removeFavorite(stationUuid)
                            } else {
                                val st = Station(
                                    uuid = stationUuid,
                                    name = cur.title,
                                    url = cur.url.orEmpty(),
                                    logo = cur.artwork,
                                    isHls = cur.isHls,
                                )
                                if (st.url.isNotBlank()) {
                                    scope.launch { app.favorites.toggleStation(st) }
                                }
                            }
                        },
                        tint = if (isFavStation) Color(0xFFF472B6) else Color.White,
                    )

                    // Ambient screensaver, right next to the heart button.
                    RoundButton(
                        icon = Icons.Rounded.NightsStay,
                        description = "שומר מסך",
                        onClick = { ScreensaverController.show() },
                        tint = Color(0xFF93C5FD),
                    )
                }

                if (!cur.isLive) {
                    RoundButton(
                        icon = Icons.Rounded.FastRewind,
                        description = "אחורה 15 שניות",
                        onClick = { app.player.seekBy(-15_000) },
                    )
                }

                Surface(
                    onClick = { app.player.togglePlayPause() },
                    modifier = Modifier.size(96.dp).focusRequester(focusRequester),
                ) {
                    Box(
                        Modifier.fillMaxSize().background(Purple, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause
                            else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "השהה" else "נגן",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp),
                        )
                    }
                }

                if (!cur.isLive) {
                    RoundButton(
                        icon = Icons.Rounded.FastForward,
                        description = "קדימה 15 שניות",
                        onClick = { app.player.seekBy(15_000) },
                    )
                }

                RoundButton(
                    icon = Icons.Rounded.Stop,
                    description = "עצור",
                    onClick = { app.player.stop(); onClose() },
                    tint = Color(0xFFF87171),
                )

                if (!cur.isLive) {
                    Spacer(Modifier.width(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                            val selected = kotlin.math.abs(speed - s) < 0.01f
                            Surface(onClick = { app.player.setSpeed(s) }) {
                                Box(
                                    Modifier
                                        .background(if (selected) Teal else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        if (s == 1f) "1x" else "${s}x",
                                        color = if (selected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
