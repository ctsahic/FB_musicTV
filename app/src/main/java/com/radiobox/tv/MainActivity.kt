package com.radiobox.tv

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.radiobox.tv.ui.IdleTracker
import com.radiobox.tv.ui.ScreensaverController
import com.radiobox.tv.ui.AppRoot
import com.radiobox.tv.ui.theme.RadioBoxTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    /** Force Hebrew + RTL regardless of the TV system language. */
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("he")
        Locale.setDefault(locale) // Hebrew dates/number formatting everywhere
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app shows a clock/artwork ambient screen, so keep the panel awake while foregrounded.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Optional deep link: --es open podcasts|favorites|settings|stations [--es genre rock]
        val initial = when (intent?.getStringExtra("open")) {
            "podcasts" -> com.radiobox.tv.ui.Screen.Podcasts
            "favorites" -> com.radiobox.tv.ui.Screen.Favorites
            "settings" -> com.radiobox.tv.ui.Screen.Settings
            "stations" -> com.radiobox.tv.ui.Screen.Stations(
                com.radiobox.tv.core.Genres.byId(intent.getStringExtra("genre") ?: "rock")
            )
            else -> com.radiobox.tv.ui.Screen.Home
        }
        setContent {
            RadioBoxTheme {
                AppRoot(application as RadioBoxApp, initial)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        IdleTracker.touch()
        if (ScreensaverController.visible.value) {
            // Any key dismisses the screensaver and is swallowed once.
            ScreensaverController.hide()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        IdleTracker.touch()
        if (ScreensaverController.visible.value) {
            ScreensaverController.hide()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}
