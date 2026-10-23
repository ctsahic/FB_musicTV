package com.radiobox.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.radiobox.tv.core.StationRank
import com.radiobox.tv.core.Utils
import com.radiobox.tv.data.model.Station
import com.radiobox.tv.ui.theme.Purple
import com.radiobox.tv.ui.theme.TextSecondary

@Composable
fun StationCard(
    station: Station,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Int = 210,
    onBlock: (() -> Unit)? = null,
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.width(cardWidth.dp),
        onLongClick = onBlock,
        onMenu = onBlock,
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.55f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                LogoBox(station.logo, Modifier.fillMaxSize(), fallbackText = station.name)

                // Bitrate + codec, bottom-left on the artwork
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (station.bitrate >= 320) Badge("320k ✦", color = Color(0xFF2DD4BF))
                    else if (station.bitrate > 0) Badge("${station.bitrate}k")
                    station.codec?.takeIf { it.isNotBlank() }?.let {
                        Badge(it.uppercase(), color = Color(0xFF9CA0B3))
                    }
                }
                if (station.isHls) {
                    Badge(
                        "HLS",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                    )
                }

                // Favorite heart, top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite
                        else Icons.Rounded.FavoriteBorder,
                        contentDescription = "מועדפים",
                        tint = if (isFavorite) Color(0xFFF472B6) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                station.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    Utils.flagEmoji(station.countryCode),
                    station.subtitle,
                ).joinToString("  "),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Real genre tags (noise words filtered out) — small chips under the name
            val tagChips = station.tagList.filter { it !in StationRank.NOISE_TAGS }.take(3)
            if (tagChips.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tagChips.forEach { tag ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF2A2D40))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Compact variant for horizontal shelves (favorites on Home). */
@Composable
fun StationCardCompact(
    station: Station,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) = StationCard(
    station = station,
    isFavorite = isFavorite,
    onClick = onClick,
    onToggleFavorite = onToggleFavorite,
    modifier = modifier,
    cardWidth = 190,
)
