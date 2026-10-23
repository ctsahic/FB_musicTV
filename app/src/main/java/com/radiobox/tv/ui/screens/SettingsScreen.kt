package com.radiobox.tv.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.core.Utils
import com.radiobox.tv.ui.NavState
import com.radiobox.tv.ui.Screen
import com.radiobox.tv.ui.ScreensaverController
import com.radiobox.tv.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: RadioBoxApp, nav: NavState) {
    val settings by app.settings.settings.collectAsState(initial = com.radiobox.tv.data.repo.Settings())
    val blocked by app.favorites.blockedStations.collectAsState(initial = emptySet())
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
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
            Text("⚙️  הגדרות", fontSize = 26.sp)
        }

        // Country
        SettingsRow("מדינה", "התחנות יוצגו מהמדינה הנבחרת") {
            Surface(onClick = { nav.push(Screen.CountryPicker) }) {
                Text(
                    "${Utils.flagEmoji(settings.countryCode)}  ${settings.countryCode}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Screensaver
        SettingsRow("שומר מסך אוטומטי", "מופיע לאחר חוסר פעילות; כל מקש מחזיר למסך") {
            ChoiceChips(
                options = listOf(
                    "כבוי" to 0, "דקה" to 60, "3 דק'" to 180, "5 דק'" to 300, "10 דק'" to 600, "30 דק'" to 1800,
                ),
                selected = settings.screensaverSeconds,
            ) { app.setScreensaverSeconds(it) }
        }

        // Min quality
        SettingsRow("איכות מינימלית", "מוצגות רק תחנות מעל קצב הסיביות הזה") {
            ChoiceChips(
                options = listOf("128k+" to 128, "192k+" to 192, "320k+" to 320, "הכל" to 0),
                selected = settings.minBitrate,
            ) { app.setMinBitrate(it) }
        }

        // Buffer
        SettingsRow("גודל באפר", "באפר גדול = רציפות טובה יותר ברשת חלשה (מופעל בנגינה הבאה)") {
            ChoiceChips(
                options = listOf("30 שנ'" to 30, "60 שנ'" to 60, "120 שנ'" to 120, "180 שנ'" to 180),
                selected = settings.bufferMinSeconds,
            ) { app.setBufferSeconds(it) }
        }

        // Manual screensaver
        SettingsRow("שומר מסך עכשיו", "הצג שעון ואמנות התחנה מיד") {
            Surface(onClick = { ScreensaverController.show() }) {
                Text("הצג שומר מסך", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }

        // Blocked stations management
        if (blocked.isNotEmpty()) {
            SettingsRow("תחנות מוסתרות (${blocked.size / 2})", "לחיצה מחזירה תחנה לרשימות הקטגוריות") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    blocked.filter { it.length > 3 }
                        .sorted()
                        .forEach { nameKey ->
                            Surface(onClick = {
                                scope.launch {
                                    app.favorites.toggleBlocked(
                                        com.radiobox.tv.data.model.Station(uuid = "", name = nameKey, url = ""),
                                        nameKey,
                                    )
                                }
                            }) {
                                Text(
                                    "✕  $nameKey",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = Color.White,
                                )
                            }
                        }
                }
            }
        }

        SettingsRow("אודות", "רדיו־בוקס TV 1.0 · תחנות: radio-browser.info · פודקאסטים: iTunes") {
            Text("💜", fontSize = 24.sp)
        }

        Spacer(Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(subtitle, color = TextSecondary, style = androidx.tv.material3.MaterialTheme.typography.bodySmall)
        content()
    }
}

@Composable
private fun ChoiceChips(options: List<Pair<String, Int>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (label, value) ->
            val isSel = value == selected
            Surface(onClick = { onSelect(value) }) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isSel) Color.Black else Color.White,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
