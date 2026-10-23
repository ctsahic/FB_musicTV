package com.radiobox.tv.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.core.Utils
import com.radiobox.tv.data.model.RBCountry
import com.radiobox.tv.ui.NavState
import com.radiobox.tv.ui.components.LoadingBox
import com.radiobox.tv.ui.theme.Purple
import com.radiobox.tv.ui.theme.TextSecondary

@Composable
fun CountryPickerScreen(app: RadioBoxApp, nav: NavState) {
    val settings by app.settings.settings.collectAsState(initial = com.radiobox.tv.data.repo.Settings())
    var search by remember { mutableStateOf("") }
    val countries by produceState<List<RBCountry>>(initialValue = emptyList()) {
        value = app.stations.countries().sortedByDescending { it.stationcount }
    }

    val filtered = countries.filter {
        it.name.contains(search.trim(), ignoreCase = true) || it.iso.contains(search.trim(), ignoreCase = true)
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
            Text("🌍  בחר מדינה", fontSize = 26.sp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.size(16.dp))

        Surface {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Purple),
                    decorationBox = { inner ->
                        if (search.isEmpty()) {
                            Text("חפש מדינה…", color = TextSecondary, fontSize = 18.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.size(14.dp))

        if (countries.isEmpty()) {
            LoadingBox("טוען רשימת מדינות…")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(filtered, key = { _, c -> c.iso }) { _, country ->
                    val isCurrent = country.iso == settings.countryCode
                    Surface(onClick = {
                        app.setCountry(country.iso)
                        nav.pop()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(Utils.flagEmoji(country.iso), fontSize = 24.sp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    country.name,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) Purple else Color.White,
                                )
                                Text("${country.stationcount} תחנות", color = TextSecondary, style = androidx.tv.material3.MaterialTheme.typography.labelSmall)
                            }
                            if (isCurrent) {
                                Text("נבחרה ✓", color = Purple, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
