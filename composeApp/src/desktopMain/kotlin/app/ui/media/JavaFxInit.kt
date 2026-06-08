package app.ui.media

import javafx.application.Platform

object JfxInit {
    @Volatile private var started = false

    fun ensureStarted() {
        if (started) return
        synchronized(this) {
            if (started) return
            try {
                Platform.startup {
                    Platform.setImplicitExit(false)
                }
            } catch (_: IllegalStateException) {
                Platform.setImplicitExit(false)
            }
            started = true
        }
    }
}
