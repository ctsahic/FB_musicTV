package com.radiobox.tv.ui

import kotlinx.coroutines.flow.MutableStateFlow

/** Tracks the last user input timestamp; reset from MainActivity key/touch dispatch. */
object IdleTracker {
    @Volatile
    var lastInteractionMs: Long = System.currentTimeMillis()
        private set

    fun touch() {
        lastInteractionMs = System.currentTimeMillis()
    }

    fun idleSeconds(nowMs: Long = System.currentTimeMillis()): Long =
        (nowMs - lastInteractionMs) / 1000
}

/** Controls the in-app ambient screensaver overlay. */
object ScreensaverController {
    val visible = MutableStateFlow(false)

    fun show() {
        visible.value = true
    }

    fun hide() {
        visible.value = false
        IdleTracker.touch()
    }
}

/** Whether the full-screen player overlay is open. */
object PlayerUi {
    val open = MutableStateFlow(false)

    fun show() {
        open.value = true
    }

    fun hide() {
        open.value = false
        IdleTracker.touch()
    }
}
