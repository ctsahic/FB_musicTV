package com.radiobox.tv.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.radiobox.tv.core.Genre
import com.radiobox.tv.data.model.Podcast

sealed interface Screen {
    data object Home : Screen
    data class Stations(val genre: Genre) : Screen
    data object Favorites : Screen
    data object Podcasts : Screen
    data class PodcastDetail(val podcast: Podcast) : Screen
    data object Settings : Screen
    data object CountryPicker : Screen
}

class NavState {
    var stack: List<Screen> by mutableStateOf(listOf(Screen.Home))
        private set

    val current: Screen get() = stack.last()

    fun push(screen: Screen) {
        stack = stack + screen
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    fun resetToHome() {
        stack = listOf(Screen.Home)
    }
}
