package app.ui.media

import io.github.oshai.kotlinlogging.KotlinLogging
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.log.LogEventListener
import uk.co.caprica.vlcj.log.LogLevel
import uk.co.caprica.vlcj.log.NativeLog
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

/**
 * Process-wide holder for the vlcj MediaPlayerFactory.
 *
 * Must be initialised exactly once on application startup, **before** the
 * first call to [get], by invoking [initialize]. The optional [nativePath]
 * should point to a directory containing the libVLC native libraries
 * (libvlc.dll / libvlc.dylib / libvlc.so.*) plus the `plugins/`
 * subfolder. The path is published via the `jna.library.path` system
 * property so JNA picks it up when libVLC is loaded.
 *
 * On initialisation a [NativeLog] is also configured so that native
 * libVLC diagnostic messages are forwarded to the Kotlin logger at the
 * corresponding level.
 */
object VlcSupport {

    private const val JNA_LIBRARY_PATH = "jna.library.path"

    private val factoryRef = AtomicReference<MediaPlayerFactory?>(null)
    private var nativeLog: NativeLog? = null

    @Synchronized
    fun initialize(nativePath: Path? = null) {
        if (factoryRef.get() != null) {
            logger.debug { "VlcSupport.initialize: already initialised" }
            return
        }
        try {
            if (nativePath != null) {
                System.setProperty(JNA_LIBRARY_PATH, nativePath.toString())
                logger.info { "VlcSupport: set jna.library.path to $nativePath" }
            }
            val discovery = NativeDiscovery()
            val factory = MediaPlayerFactory(discovery)
            factoryRef.set(factory)

            // --- Wire up native libVLC logging ---
            setupNativeLog(factory)

            val strategy = discovery.successfulStrategy()?.javaClass?.simpleName
            logger.info {
                "VlcSupport: MediaPlayerFactory ready " +
                    "(strategy=$strategy, path=${discovery.discoveredPath()})"
            }
        } catch (t: Throwable) {
            logger.error(t) { "VlcSupport: failed to initialise MediaPlayerFactory" }
            throw t
        }
    }

    private fun setupNativeLog(factory: MediaPlayerFactory) {
        try {
            val instance = factory.getLibVlcInstance().get()
            val nlog = NativeLog(instance)
            nlog.setLevel(LogLevel.DEBUG)
            nlog.addLogListener(object : LogEventListener {
                override fun log(
                    level: LogLevel,
                    message: String,
                    module: String,
                    line: Int?,
                    file: String?,
                    name: String?,
                    id: Int?,
                    header: String?,
                ) {
                    when (level) {
                        LogLevel.ERROR -> logger.error { "[VLC] [$module] $message" }
                        LogLevel.WARNING -> logger.warn { "[VLC] [$module] $message" }
                        else -> logger.debug { "[VLC] [$module] $message" }
                    }
                }
            })
            nativeLog = nlog
            logger.debug { "VlcSupport: native libVLC logging enabled" }
        } catch (t: Throwable) {
            logger.warn(t) { "VlcSupport: could not set up native logging" }
        }
    }

    fun get(): MediaPlayerFactory {
        return factoryRef.get()
            ?: error("VlcSupport.initialize() must be called before fetching MediaPlayerFactory")
    }

    fun isInitialized(): Boolean = factoryRef.get() != null

    @Synchronized
    fun shutdown() {
        nativeLog?.release()
        nativeLog = null
        factoryRef.getAndSet(null)?.release()
    }
}
