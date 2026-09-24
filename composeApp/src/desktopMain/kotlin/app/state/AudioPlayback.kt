package app.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Live progress of the audio clip playing on the shared display window.
 *
 * Only the shared display owns a real audio player and writes here; the host
 * window renders this state as a read-only progress bar, so a clip is never
 * played twice. Written from vlcj callback threads (snapshot state is
 * thread-safe for single writes).
 */
@Stable
class AudioPlaybackState {
    /** Normalized URI of the clip being played, or `null` when nothing is loaded. */
    var uri: String? by mutableStateOf(null)
        private set
    var playing: Boolean by mutableStateOf(false)
        private set
    var currentMs: Double by mutableStateOf(0.0)
        private set
    var totalMs: Double by mutableStateOf(0.0)
        private set
    var errorMessage: String? by mutableStateOf(null)
        private set

    fun start(uri: String) {
        this.uri = uri
        playing = true
        currentMs = 0.0
        totalMs = 0.0
        errorMessage = null
    }

    fun updateCurrent(ms: Double) {
        currentMs = ms
    }

    fun updateTotal(ms: Double) {
        totalMs = ms
    }

    fun finish() {
        playing = false
    }

    fun fail(message: String) {
        errorMessage = message
        playing = false
    }

    /** Clears the state if it still belongs to [uri] (the player for it was disposed). */
    fun release(uri: String) {
        if (this.uri != uri) return
        this.uri = null
        playing = false
        currentMs = 0.0
        totalMs = 0.0
        errorMessage = null
    }
}
