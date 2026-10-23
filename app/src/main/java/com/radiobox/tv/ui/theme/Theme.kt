package com.radiobox.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val Purple = Color(0xFF8B5CF6)
val PurpleDark = Color(0xFF5B21B6)
val Teal = Color(0xFF2DD4BF)
val Bg = Color(0xFF0E0F16)
val Surface1 = Color(0xFF181A24)
val Surface2 = Color(0xFF232637)
val TextPrimary = Color(0xFFF4F4F8)
val TextSecondary = Color(0xFF9CA0B3)
val Danger = Color(0xFFF87171)

@Composable
fun RadioBoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            onPrimary = Color.White,
            secondary = Teal,
            onSecondary = Color.Black,
            background = Bg,
            onBackground = TextPrimary,
            surface = Surface1,
            onSurface = TextPrimary,
            surfaceVariant = Surface2,
            onSurfaceVariant = TextSecondary,
            error = Danger,
        ),
        content = content,
    )
}

val CardShape = RoundedCornerShape(14)
