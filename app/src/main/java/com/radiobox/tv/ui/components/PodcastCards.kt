package com.radiobox.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.radiobox.tv.core.Utils
import com.radiobox.tv.data.model.Podcast
import com.radiobox.tv.data.model.PodcastEpisode
import com.radiobox.tv.ui.theme.Purple
import com.radiobox.tv.ui.theme.TextSecondary

@Composable
fun PodcastCard(podcast: Podcast, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusableCard(onClick = onClick, modifier = modifier.width(200.dp), accent = Purple) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                LogoBox(podcast.artworkUrl, Modifier.matchParentSize(), fallbackText = podcast.name)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                podcast.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                podcast.artist,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EpisodeRow(
    episode: PodcastEpisode,
    isCurrent: Boolean,
    isPlayingThis: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (focused, focusMod) = rememberFocused()
    FocusableCard(
        onClick = onPlay,
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    ) {
        Row(
            focusMod
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (focused) FocusBar(height = 44.dp)
            Icon(
                imageVector = if (isPlayingThis) Icons.Rounded.Pause
                else Icons.Rounded.PlayArrow,
                contentDescription = "נגן",
                tint = if (isCurrent) Purple else TextSecondary,
                modifier = Modifier.size(30.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    episode.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    episode.pubDateLabel?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    episode.durationSec?.let {
                        Text(
                            Utils.formatDuration(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
            if (isCurrent) MiniEqualizer(playing = isPlayingThis)
        }
    }
}
