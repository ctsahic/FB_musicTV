package com.radiobox.tv.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Small process-wide bridge between the playback service and the UI.
 * (Service and activity run in the same process; MediaController handles the rest.)
 */
object SharedPlaybackState {

    /** Live "now playing" title coming from ICY metadata of radio streams. */
    private val _streamTitle = MutableStateFlow<String?>(null)
    val streamTitle: StateFlow<String?> = _streamTitle

    fun setStreamTitle(value: String?) {
        _streamTitle.value = value?.takeIf { it.isNotBlank() }
    }

    /** True while the service is auto-reconnecting after a stream error. */
    private val _reconnecting = MutableStateFlow(false)
    val reconnecting: StateFlow<Boolean> = _reconnecting

    fun setReconnecting(value: Boolean) {
        _reconnecting.value = value
    }

    fun resetForNewMedia() {
        _streamTitle.value = null
        _reconnecting.value = false
    }
}
