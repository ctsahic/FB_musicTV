package com.radiobox.tv.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.radiobox.tv.RadioBoxApp
import com.radiobox.tv.ui.screens.ScreensaverOverlay
import com.radiobox.tv.ui.screens.PlayerScreen
import com.radiobox.tv.ui.screens.HomeScreen
import com.radiobox.tv.ui.screens.StationsScreen
import com.radiobox.tv.ui.screens.FavoritesScreen
import com.radiobox.tv.ui.screens.PodcastsScreen
import com.radiobox.tv.ui.screens.PodcastDetailScreen
import com.radiobox.tv.ui.screens.SettingsScreen
import com.radiobox.tv.ui.screens.CountryPickerScreen
import com.radiobox.tv.ui.theme.Purple
import com.radiobox.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun AppRoot(app: RadioBoxApp, initialScreen: Screen = Screen.Home) {
    val nav = remember { NavState().apply { if (initialScreen != Screen.Home) push(initialScreen) } }
    val activity = LocalContext.current as? Activity
    var showExitDialog by remember { mutableStateOf(false) }

    // BACK button behavior: dismiss screensaver -> close player -> pop screen -> exit dialog on Home.
    BackHandler(enabled = true) {
        when {
            ScreensaverController.visible.value -> ScreensaverController.hide()
            PlayerUi.open.value -> PlayerUi.hide()
            showExitDialog -> showExitDialog = false
            nav.current != Screen.Home -> nav.pop()
            else -> showExitDialog = true
        }
    }

    // Auto screensaver after N seconds of inactivity (configurable, 0 = off).
    val settings by app.settings.settings.collectAsState(initial = null)
    val s = settings
    LaunchedEffect(s?.screensaverEnabled, s?.screensaverSeconds) {
        if (s == null) return@LaunchedEffect
        while (true) {
            delay(1000)
            if (!ScreensaverController.visible.value &&
                s.screensaverEnabled &&
                s.screensaverSeconds > 0 &&
                IdleTracker.idleSeconds() >= s.screensaverSeconds
            ) {
                ScreensaverController.show()
            }
        }
    }

    val saverVisible by ScreensaverController.visible.collectAsState()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val screen = nav.current) {
            Screen.Home -> HomeScreen(app, nav)
            is Screen.Stations -> StationsScreen(app, nav, screen.genre)
            Screen.Favorites -> FavoritesScreen(app, nav)
            Screen.Podcasts -> PodcastsScreen(app, nav)
            is Screen.PodcastDetail -> PodcastDetailScreen(app, nav, screen.podcast)
            Screen.Settings -> SettingsScreen(app, nav)
            Screen.CountryPicker -> CountryPickerScreen(app, nav)
        }

        val playerOpen by PlayerUi.open.collectAsState()
        AnimatedVisibility(
            visible = playerOpen && !saverVisible,
            enter = fadeIn(androidx.compose.animation.core.tween(250)),
            exit = fadeOut(androidx.compose.animation.core.tween(250)),
        ) {
            PlayerScreen(app, onClose = { PlayerUi.hide() })
        }

        AnimatedVisibility(
            visible = saverVisible,
            enter = fadeIn(androidx.compose.animation.core.tween(700)),
            exit = fadeOut(androidx.compose.animation.core.tween(400)),
        ) {
            ScreensaverOverlay(app)
        }
    }

    if (showExitDialog) {
        ExitDialog(
            onDismiss = { showExitDialog = false },
            onExit = { activity?.finish() },
            onBackground = {
                showExitDialog = false
                activity?.moveTaskToBack(true)
            },
        )
    }
}

@Composable
private fun ExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit,
    onBackground: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Plain container (no onClick) so it never steals D-pad focus.
        Surface(modifier = Modifier.fillMaxWidth(0.55f)) {
            Column(
                Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("לצאת מהאפליקציה?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "הנגינה תמשיך לעבוד ברקע גם אם תצא",
                    color = TextSecondary,
                    style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
                )
                val firstButton = remember { FocusRequester() }
                LaunchedEffect(Unit) { runCatching { firstButton.requestFocus() } }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DialogButton("בטל", Modifier.weight(1f), onDismiss, focusRequester = firstButton)
                    DialogButton("ברקע", Modifier.weight(1f), onBackground)
                    DialogButton("אישור", Modifier.weight(1f), onExit, highlight = true)
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    highlight: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.then(
            if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
        ),
    ) {
        Box(
            Modifier
                .background(
                    if (highlight) Purple else Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(10.dp),
                )
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = if (highlight) Color.White else TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
