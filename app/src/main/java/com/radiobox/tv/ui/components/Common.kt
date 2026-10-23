package com.radiobox.tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.radiobox.tv.ui.theme.Purple
import com.radiobox.tv.ui.theme.PurpleDark
import com.radiobox.tv.ui.theme.Teal
import com.radiobox.tv.ui.theme.TextPrimary
import com.radiobox.tv.ui.theme.TextSecondary

// ---------------------------------------------------------------- focus plumbing

/** Pressed-glow-free focus tracker: re-composes children with [focused] = true. */
@Composable
fun rememberFocused(): Pair<Boolean, Modifier> {
    var focused by remember { mutableStateOf(false) }
    return focused to Modifier.onFocusChanged { focused = it.isFocused }
}

/** Deterministic 2-color gradient derived from any string (station name, genre id…). */
fun gradientFor(seed: String): Brush {
    val colors = GRADIENTS
    var h = 1125899906842597L
    for (ch in seed) h = 31 * h + ch.code
    val a = colors[((h % colors.size) + colors.size).toInt() % colors.size]
    val b = colors[(((h / 7) % colors.size) + colors.size).toInt() % colors.size]
    return Brush.linearGradient(listOf(a.first, a.second, b.first))
}

private val GRADIENTS = listOf(
    Color(0xFF6D28D9) to Color(0xFFDB2777), // purple-pink
    Color(0xFF0E7490) to Color(0xFF2563EB), // cyan-blue
    Color(0xFFB45309) to Color(0xFFDC2626), // amber-red
    Color(0xFF047857) to Color(0xFF0D9488), // emerald-teal
    Color(0xFF7C3AED) to Color(0xFF4F46E5), // violet-indigo
    Color(0xFFBE185D) to Color(0xFF9333EA), // rose-purple
    Color(0xFF1D4ED8) to Color(0xFF0891B2), // blue-cyan
    Color(0xFFCA8A04) to Color(0xFFEA580C), // gold-orange
)

// ---------------------------------------------------------------- containers

/**
 * The one card container used across the app: scales up + glows with an accent
 * border when focused, so focus is obvious on a 10-foot TV.
 * [onLongClick] = long-press action; [onMenu] = remote MENU key action.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    accent: Color = Purple,
    onLongClick: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f),
        label = "cardScale",
    )
    val clickModifier =
        if (onLongClick != null) {
            Modifier.combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
        } else {
            Modifier.clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
        }
    val menuKeyModifier =
        if (onMenu != null) {
            Modifier.onKeyEvent { event ->
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP &&
                    event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_MENU
                ) {
                    onMenu(); true
                } else false
            }
        } else Modifier
    Box(
        modifier
            .onKeyEvent { event ->
                // Held DPAD_CENTER (real remote long-press) also triggers the action.
                if ((onLongClick != null || onMenu != null) &&
                    event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP &&
                    event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER &&
                    event.nativeKeyEvent.eventTime - event.nativeKeyEvent.downTime > 700
                ) {
                    (onMenu ?: onLongClick)?.invoke(); true
                } else false
            }
            .scale(scale)
            .shadow(
                elevation = if (focused) 22.dp else 4.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.55f),
                spotColor = accent.copy(alpha = 0.75f),
                clip = false,
            )
            .clip(shape)
            .background(if (focused) Color(0xFF232637) else Color(0xFF191B26))
            .then(
                if (focused) Modifier.border(2.dp, accent, shape)
                else Modifier.border(1.dp, Color(0xFF2A2D40), shape)
            )
            .then(clickModifier)
            .then(menuKeyModifier),
    ) {
        content()
    }
}

// ---------------------------------------------------------------- chrome

@Composable
fun LoadingBox(label: String = "טוען…") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.CircularProgressIndicator(color = Purple)
            Text(label, color = TextSecondary)
        }
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("😕 אוֹפּס", style = androidx.tv.material3.MaterialTheme.typography.titleLarge)
            Text(message, color = TextSecondary)
            if (onRetry != null) {
                androidx.tv.material3.Button(onClick = onRetry) { Text("נסה שוב") }
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color = Teal, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.22f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, style = androidx.tv.material3.MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Purple)
        )
        Text(
            text,
            modifier = Modifier.padding(start = 10.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}

@Composable
fun HeaderIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val (focused, focusMod) = rememberFocused()
    FocusableCard(onClick = onClick, shape = RoundedCornerShape(12.dp), accent = Teal) {
        Box(
            focusMod.padding(10.dp).size(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = desc, modifier = Modifier.size(24.dp), tint = if (focused) Color.White else TextSecondary)
        }
    }
}

/** Vertical accent bar shown on the focused row (episode lists etc.). */
@Composable
fun FocusBar(width: Dp = 4.dp, height: Dp = 40.dp) {
    Box(
        Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(Purple)
    )
}

// ---------------------------------------------------------------- equalizer

/** Simple animated equalizer shown while audio is playing. */
@Composable
fun EqualizerBars(modifier: Modifier = Modifier, playing: Boolean, barCount: Int = 5) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        repeat(barCount) { i ->
            val height by transition.animateFloat(
                initialValue = 8f,
                targetValue = 34f,
                animationSpec = if (playing) {
                    infiniteRepeatable(
                        animation = androidx.compose.animation.core.keyframes {
                            durationMillis = 600 + i * 90
                            8f at 0
                            34f at (250 + i * 40)
                            14f at (450 + i * 60)
                            30f at (550 + i * 70)
                            8f at (600 + i * 90)
                        },
                        repeatMode = RepeatMode.Restart,
                    )
                } else {
                    infiniteRepeatable(animation = androidx.compose.animation.core.keyframes { durationMillis = 1; 8f at 0 })
                },
                label = "bar$i",
            )
            Box(
                Modifier
                    .width(6.dp)
                    .height(height.dp.coerceAtLeast(6.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i % 2 == 0) Purple else Teal)
            )
        }
    }
}

/** Compact equalizer for tight spaces. */
@Composable
fun MiniEqualizer(playing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mini-eq")
    val p by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "p",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(10, 16, 12).forEach { h ->
            Box(
                Modifier
                    .width(3.dp)
                    .height((h * (if (playing) p else 0.5f)).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Teal)
            )
        }
    }
}

// ---------------------------------------------------------------- logo

/**
 * Station/podcast logo with a beautiful gradient + initials fallback.
 * Handles both missing URLs and URLs that fail to load (dead favicon links) —
 * no more blank gray boxes.
 */
@Composable
fun LogoBox(url: String?, modifier: Modifier = Modifier, fallbackText: String = "📻") {
    val initials = remember(fallbackText) { initialsOf(fallbackText) }
    var loadFailed by remember(url) { mutableStateOf(false) }
    val showFallback = url.isNullOrBlank() || loadFailed
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor),
        contentAlignment = Alignment.Center,
    ) {
        if (showFallback) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(gradientFor(fallbackText))
            ) {
                Text(
                    initials,
                    modifier = Modifier.align(Alignment.Center),
                    style = androidx.tv.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
        } else {
            coil.compose.AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                // Fit (not Crop): wide KAN/banner logos would lose their text when cropped.
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                onError = { loadFailed = true },
            )
        }
    }
}

/** Leading brand words skipped when deriving fallback initials (כאן רשת ב' → ב, רדיו סהר → סה). */
private val NAME_PREFIXES = setOf(
    "כאן", "רדיו", "גלי", "רשת", "קול", "התחנה", "תחנת", "ערוץ", "חדשות", "radio", "kan",
)

private fun initialsOf(name: String): String {
    val words = name.split(Regex("[\\s\\-–—_·,.:()\\[\\]׳'\"״]+"))
        .filter { it.isNotBlank() }
    val significant = words.dropWhile { it.lowercase() in NAME_PREFIXES }.ifEmpty { words }
    val first = significant.firstOrNull().orEmpty()
    // Frequency-number names ("103FM", "88FM", "13") → show the number itself.
    if (first.any { it.isDigit() }) {
        return first.takeWhile { it.isLetterOrDigit() }.take(3).uppercase().ifBlank { "📻" }
    }
    val letters = first.filter { it.isLetter() }
    if (letters.length >= 2) return letters.take(2).uppercase()
    // Single-letter word (e.g. "ב'" in כאן רשת ב') → append the next word's first letter.
    val second = significant.getOrNull(1)?.filter { it.isLetter() }.orEmpty()
    return (letters + second.take(1)).ifBlank { "📻" }.uppercase()
}

val SurfaceColor = Color(0xFF232637)
