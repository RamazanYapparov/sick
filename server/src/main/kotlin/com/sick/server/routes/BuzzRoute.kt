package com.sick.server.routes

import com.sick.engine.GameEngine
import com.sick.event.PlayerBuzzed
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun Application.installBuzzRoute(engine: GameEngine, buzzAllowed: () -> Boolean) {
    routing {
        post("/buzz") {
            if (!buzzAllowed()) {
                logger.debug { "/buzz rejected: game is paused" }
                return@post call.respond(HttpStatusCode.ServiceUnavailable, "Game is paused")
            }

            val playerIdValue = call.receiveParameters()["playerId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing playerId")

            val playerId = try {
                UUID.fromString(playerIdValue)
            } catch (_: IllegalArgumentException) {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid playerId")
            }

            logger.info { "/buzz playerId=$playerId" }
            engine.process(PlayerBuzzed(playerId)).fold(
                ifLeft = { error ->
                    logger.warn { "/buzz playerId=$playerId rejected: ${error.message}" }
                    call.respond(HttpStatusCode.BadRequest, error.message)
                },
                ifRight = {
                    logger.info { "/buzz playerId=$playerId OK" }
                    call.respond(HttpStatusCode.OK)
                },
            )
        }
    }
}
