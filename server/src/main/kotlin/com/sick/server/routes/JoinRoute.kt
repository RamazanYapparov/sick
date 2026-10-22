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
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

fun Application.installJoinRoute(engine: GameEngine) {
    val reconnectTokens = ConcurrentHashMap<UUID, String>()
    routing {
        post("/join") {
            val parameters = call.receiveParameters()
            val name = parameters["name"]?.trim()
            if (name.isNullOrBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, "Name is required")
            }

            val existing = engine.state.players.find { it.name == name }
            if (existing != null) {
                val expectedToken = reconnectTokens[existing.id]
                val providedToken = parameters["reconnectToken"]
                if (expectedToken == null || providedToken == null || !tokensEqual(expectedToken, providedToken)) {
                    logger.warn { "/join name=$name rejected: reconnect credentials do not match" }
                    return@post call.respond(HttpStatusCode.Forbidden, "Name is already in use")
                }
                logger.info { "/join name=$name -> existing player ${existing.id}" }
                return@post call.respondText(
                    credentialsJson(existing.id, expectedToken),
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
                    val reconnectToken = generateReconnectToken()
                    reconnectTokens[player.id] = reconnectToken
                    logger.info { "/join name=$name -> playerId=${player.id}" }
                    call.respondText(
                        credentialsJson(player.id, reconnectToken),
                        ContentType.Application.Json,
                    )
                },
            )
        }
    }
}

private val secureRandom = SecureRandom()

private fun generateReconnectToken(): String =
    ByteArray(32).also(secureRandom::nextBytes).let(Base64.getUrlEncoder().withoutPadding()::encodeToString)

private fun tokensEqual(expected: String, provided: String): Boolean =
    MessageDigest.isEqual(
        expected.toByteArray(StandardCharsets.UTF_8),
        provided.toByteArray(StandardCharsets.UTF_8),
    )

private fun credentialsJson(playerId: UUID, reconnectToken: String): String =
    """{"playerId":"$playerId","reconnectToken":"$reconnectToken"}"""
