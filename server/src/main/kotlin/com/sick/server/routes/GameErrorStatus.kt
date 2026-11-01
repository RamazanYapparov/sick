package com.sick.server.routes

import com.sick.engine.GameError
import com.sick.service.PlayerError
import io.ktor.http.HttpStatusCode

/**
 * 410 tells the phone its player id is not in the current game (the host reset
 * the game or loaded another pack), so the page goes back to the join screen.
 */
internal fun GameError.httpStatus(): HttpStatusCode =
    if (this is GameError.PlayerError && inner is PlayerError.NotFound) HttpStatusCode.Gone
    else HttpStatusCode.BadRequest
