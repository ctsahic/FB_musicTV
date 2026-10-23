package com.radiobox.tv.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.data.model.Podcast
import com.radiobox.tv.ui.NavState
import com.radiobox.tv.ui.Screen
import com.radiobox.tv.ui.components.ErrorBox
import com.radiobox.tv.ui.components.LoadingBox
import com.radiobox.tv.ui.components.PodcastCard
import com.radiobox.tv.ui.theme.Purple
import com.radiobox.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private val PRESETS = listOf("פודקאסט", "חדשות", "היסטוריה", "סטנד אפ", "כלכלה", "ספורט", "פסיכולוגיה")

/** De-duplicates podcasts by feedUrl (+name) so lists never contain duplicate keys. */
private fun List<Podcast>.deduped(): List<Podcast> =
    distinctBy { it.feedUrl.lowercase() + "|" + it.name }

@Composable
fun PodcastsScreen(app: RadioBoxApp, nav: NavState) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchEpoch by remember { mutableStateOf(0) }
    var showSearchDialog by remember { mutableStateOf(false) }

    // Hebrew highlights for the default view (deduped!)
    val highlights by produceState<List<Podcast>>(initialValue = emptyList()) {
        value = app.podcasts.hebrewHighlights().deduped()
    }

    LaunchedEffect(query, searchEpoch) {
        if (query.isBlank()) {
            loading = false
            results = emptyList()
            return@LaunchedEffect
        }
        delay(400) // debounce
        loading = true
        error = null
        try {
            results = app.podcasts.search(query.trim()).deduped()
            loading = false
        } catch (e: Exception) {
            error = e.message ?: "שגיאה"
            loading = false
        }
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
            Text("🎙  פודקאסטים בעברית", fontSize = 26.sp, modifier = Modifier.weight(1f))
            // Search opens only from this button
            Surface(onClick = { showSearchDialog = true }) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(if (query.isBlank()) "חיפוש" else "חיפוש: $query", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.size(14.dp))

        // Category chips (browse without typing)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEach { preset ->
                val selected = query == preset
                Surface(onClick = {
                    query = if (selected) "" else preset
                    searchEpoch++
                }) {
                    Text(
                        preset,
                        modifier = Modifier
                            .background(
                                if (selected) Purple else Color.White.copy(alpha = 0.07f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (selected) Color.White else TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.size(16.dp))

        val list = if (query.isBlank()) highlights else results
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox("שגיאת חיפוש: $error", onRetry = { searchEpoch++ })
            query.isBlank() && list.isEmpty() -> LoadingBox("טוען המלצות…")
            list.isEmpty() -> ErrorBox("לא נמצאו תוצאות עבור \"$query\"")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(list, key = { _, p -> p.feedUrl + "#" + p.name }) { _, p ->
                    PodcastCard(podcast = p, onClick = { nav.push(Screen.PodcastDetail(p)) })
                }
            }
        }
    }

    if (showSearchDialog) {
        SearchDialog(
            initial = query,
            onDismiss = { showSearchDialog = false },
            onSearch = { term ->
                query = term
                searchEpoch++
                showSearchDialog = false
            },
        )
    }
}

@Composable
private fun SearchDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Plain container (no onClick) so D-pad focus reaches the text field and buttons.
        Surface(modifier = Modifier.fillMaxWidth(0.6f)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("חיפוש פודקאסט", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Surface {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Purple),
                            decorationBox = { inner ->
                                if (text.isEmpty()) {
                                    Text("הקלד שם פודקאסט בעברית…", color = TextSecondary, fontSize = 18.sp)
                                }
                                inner()
                            },
                            modifier = Modifier.weight(1f).focusRequester(focus),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Box(Modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("ביטול", color = TextSecondary)
                        }
                    }
                    Surface(
                        onClick = { if (text.isNotBlank()) onSearch(text.trim()) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(Modifier.background(Purple, RoundedCornerShape(10.dp)).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("חפש", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
