package com.sick.server

import com.sick.engine.GameEngine
import com.sick.server.routes.installBuzzRoute
import com.sick.server.routes.installJoinRoute
import com.sick.server.routes.installPageRoute
import com.sick.server.routes.installSkipRoute
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer

class GameServer(
    private val engine: GameEngine,
    val port: Int = 8080,
    private val buzzAllowed: () -> Boolean = { true },
) {
    private var server: ApplicationEngine? = null

    fun start() {
        if (server != null) {
            return
        }

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            installPageRoute()
            installBuzzRoute(engine, buzzAllowed)
            installSkipRoute(engine, buzzAllowed)
            installJoinRoute(engine)
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 500)
        server = null
    }
}
