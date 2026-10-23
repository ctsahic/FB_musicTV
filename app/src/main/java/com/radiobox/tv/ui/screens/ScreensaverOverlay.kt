package com.radiobox.tv.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.playback.SharedPlaybackState
import com.radiobox.tv.ui.theme.Purple
import com.radiobox.tv.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ClockTick(val time: String, val date: String, val minute: Int, val hour: Int)

@Composable
fun rememberClockTick(): State<ClockTick> = produceState(
    initialValue = ClockTick("--:--", "", 0, 0),
    producer = {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val fmtDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            val cal = java.util.Calendar.getInstance().apply { time = now }
            value = ClockTick(
                time = fmt.format(now),
                date = fmtDate.format(now),
                minute = cal.get(java.util.Calendar.MINUTE),
                hour = cal.get(java.util.Calendar.HOUR_OF_DAY),
            )
            kotlinx.coroutines.delay(5_000)
        }
    },
)

/**
 * Ambient screensaver. Everything on screen drifts slowly in a long cycle
 * so static pixels never burn into a plasma/LED panel.
 */
@Composable
fun ScreensaverOverlay(app: RadioBoxApp) {
    val tick by rememberClockTick()
    val current by app.player.current.collectAsState()
    val isPlaying by app.player.isPlaying.collectAsState()
    val streamTitle by SharedPlaybackState.streamTitle.collectAsState()

    // One slow global cycle: 0 -> 1 -> 0 over ~4 minutes.
    val drift = rememberInfiniteTransition(label = "drift")
    val phase by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 240_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "phase",
    )

    // Movement: a Lissajous glide — the clock block visibly travels an oval
    // path (±80dp horizontal, ±50dp vertical) and retraces it. Continuous at
    // every phase, so nothing ever jumps and no pixel stays put for long.
    val ang = phase * 2f * kotlin.math.PI.toFloat()
    val glideX = kotlin.math.sin(ang)
    val glideY = kotlin.math.sin(2f * ang + 1.1f)
    val clockAlpha = 0.72f + 0.28f * (0.5f + 0.5f * kotlin.math.sin(phase * Math.PI * 2).toFloat())

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1030).copy(alpha = 0.9f + 0.1f * phase),
                        Color(0xFF0E0F16),
                    ),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // ---- Clock block: slow diagonal drift ----
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .graphicsLayer {
                    translationX = glideX * 80.dp.toPx()
                    translationY = glideY * 50.dp.toPx()
                    alpha = clockAlpha
                },
        ) {
            // Minute dots: orbit gently around the clock
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { i ->
                    val lit = (tick.minute / 15) > i
                    val bob = if (lit) 1f else 0f
                    Box(
                        Modifier
                            .size(if (lit) 16.dp else 10.dp)
                            .graphicsLayer {
                                translationY = bob * 6f * (i + 1) * (phase - 0.5f) * 2f
                            }
                            .background(if (lit) Purple else Color(0xFF2A2D3E), CircleShape)
                    )
                }
            }

            Text(
                tick.time,
                fontSize = 110.sp,
                fontWeight = FontWeight.Thin,
                color = Color.White,
            )
            Text(
                tick.date,
                color = TextSecondary,
                style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.graphicsLayer { translationX = glideX * -24.dp.toPx() },
            )

            // ---- Now playing pill: slides horizontally, only when playing ----
            if (current != null && isPlaying) {
                Spacer(Modifier.height(26.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),                        modifier = Modifier
                            .graphicsLayer { translationX = glideX * 60.dp.toPx() }
                        .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(30.dp),
                    )
                    Column {
                        Text(current!!.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                        val st = streamTitle
                        if (!st.isNullOrBlank()) {
                            Text(st, color = TextSecondary, style = androidx.tv.material3.MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // ---- Hint fades out and back in ----
        Text(
            "לחץ על כל מקש בשלט כדי לחזור",
            color = TextSecondary.copy(alpha = 0.5f),
            style = androidx.tv.material3.MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .alpha(if (phase < 0.1f) 1f - phase * 10f else if (phase > 0.9f) (phase - 0.9f) * 10f else 0f),
        )
    }
}
