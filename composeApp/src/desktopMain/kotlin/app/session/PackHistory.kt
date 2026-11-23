package app.session

import com.fasterxml.jackson.databind.ObjectMapper
import com.sick.engine.EngineTransition
import com.sick.event.StartGame
import com.sick.state.GamePhase
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

private val logger = KotlinLogging.logger {}

/** When a Pack was last Played and whether it is Finished. */
data class PackRecord(val lastPlayedAt: Long, val finished: Boolean = false)

/**
 * Played and Finished per Pack key (see docs/adr/0002), in one JSON file.
 * A save never touches the old file until the new one reads back the same;
 * a file that cannot be read is moved aside to `.corrupt` and the history starts empty.
 * If it cannot be moved aside either (locked, say), this run never saves, so the old file survives.
 */
class PackHistory(
    private val file: Path = Paths.get(System.getProperty("user.home"), ".sick", "pack-history.json"),
) {
    private val mapper = ObjectMapper()
    private var writable = true

    var records: Map<String, PackRecord> = load()
        private set

    /** A Pack is Played when a game with it starts, Finished when the game reaches its end. */
    fun record(key: String, transition: EngineTransition, now: Long) {
        if (transition.event == StartGame) update(key, PackRecord(now, records[key]?.finished ?: false))
        if (transition.phase == GamePhase.GameOver && transition.previousPhase != GamePhase.GameOver) {
            update(key, PackRecord(records[key]?.lastPlayedAt ?: now, finished = true))
        }
    }

    private fun update(key: String, record: PackRecord) {
        records = records + (key to record)
        if (!writable) return
        runCatching { save(records) }.onFailure { logger.error(it) { "Could not save pack history to $file" } }
    }

    private fun load(): Map<String, PackRecord> {
        if (!Files.exists(file)) return emptyMap()
        return try {
            read(file)
        } catch (e: Exception) {
            val aside = file.resolveSibling("${file.fileName}.corrupt")
            logger.error(e) { "Pack history $file is unreadable; moving it to $aside" }
            runCatching { Files.move(file, aside, REPLACE_EXISTING) }.onFailure {
                logger.error(it) { "Could not move $file aside; pack history is not saved this run" }
                writable = false
            }
            emptyMap()
        }
    }

    private fun save(records: Map<String, PackRecord>) {
        Files.createDirectories(file.parent)
        val tmp = file.resolveSibling("${file.fileName}.tmp")
        val json = records.mapValues { (_, r) -> mapOf("lastPlayedAt" to r.lastPlayedAt, "finished" to r.finished) }
        mapper.writeValue(tmp.toFile(), json)
        check(read(tmp) == records) { "$tmp does not read back what was written" }
        Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE)
    }

    private fun read(path: Path): Map<String, PackRecord> =
        mapper.readTree(path.toFile()).fields().asSequence().associate { (key, node) ->
            key to PackRecord(node.required("lastPlayedAt").longValue(), node.required("finished").booleanValue())
        }
}
