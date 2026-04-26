package com.sick.server.routes

import com.sick.engine.GameEngine
import com.sick.event.PlayerBuzzed
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.UUID

fun Application.installBuzzRoute(engine: GameEngine) {
    routing {
        post("/buzz") {
            val playerIdValue = call.receiveParameters()["playerId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing playerId")

            val playerId = try {
                UUID.fromString(playerIdValue)
            } catch (_: IllegalArgumentException) {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid playerId")
            }

            engine.process(PlayerBuzzed(playerId)).fold(
                ifLeft = { error -> call.respond(HttpStatusCode.BadRequest, error.message) },
                ifRight = { call.respond(HttpStatusCode.OK) },
            )
        }
    }
}
