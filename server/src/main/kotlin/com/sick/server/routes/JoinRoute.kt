package com.sick.server.routes

import com.sick.engine.GameEngine
import com.sick.event.PlayerJoined
import com.sick.state.GamePhase
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

private val logger = KotlinLogging.logger {}

fun Application.installJoinRoute(engine: GameEngine) {
    routing {
        post("/join") {
            val name = call.receiveParameters()["name"]?.trim()
            if (name.isNullOrBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, "Name is required")
            }

            val existing = engine.state.players.find { it.name == name }
            if (existing != null) {
                logger.info { "/join name=$name -> existing player ${existing.id}" }
                return@post call.respondText(
                    """{"playerId":"${existing.id}"}""",
                    ContentType.Application.Json,
                )
            }

            if (engine.phase != GamePhase.Lobby) {
                logger.warn { "/join name=$name rejected: game already started" }
                return@post call.respond(HttpStatusCode.Forbidden, "Game already started")
            }

            logger.info { "/join name=$name" }
            engine.process(PlayerJoined(name)).fold(
                ifLeft = { error ->
                    logger.warn { "/join name=$name rejected: ${error.message}" }
                    call.respond(HttpStatusCode.BadRequest, error.message)
                },
                ifRight = { newState ->
                    val player = newState.players.find { it.name == name }!!
                    logger.info { "/join name=$name -> playerId=${player.id}" }
                    call.respondText(
                        """{"playerId":"${player.id}"}""",
                        ContentType.Application.Json,
                    )
                },
            )
        }
    }
}
