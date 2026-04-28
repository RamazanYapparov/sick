package com.sick.server

import com.sick.engine.GameEngine
import com.sick.event.PlayerJoined
import com.sick.event.StartGame
import com.sick.model.Package
import com.sick.server.routes.installJoinRoute
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun emptyEngine(): GameEngine =
    GameEngine(Package(name = "T", logo = "", tags = emptyList(), author = "", rounds = emptyList()))

class JoinRouteTest {

    @Test
    fun `POST join creates player in Lobby and returns playerId`() = testApplication {
        val engine = emptyEngine()
        application { installJoinRoute(engine) }

        val response = client.submitForm(
            url = "/join",
            formParameters = Parameters.build { append("name", "Alice") },
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val player = engine.state.players.find { it.name == "Alice" }!!
        assertTrue(response.bodyAsText().contains(player.id.toString()))
    }

    @Test
    fun `POST join with existing name returns same playerId without creating duplicate`() = testApplication {
        val engine = emptyEngine()
        engine.process(PlayerJoined("Alice"))
        val aliceId = engine.state.players.first().id
        application { installJoinRoute(engine) }

        val response = client.submitForm(
            url = "/join",
            formParameters = Parameters.build { append("name", "Alice") },
        )

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, engine.state.players.size)
        assertTrue(response.bodyAsText().contains(aliceId.toString()))
    }

    @Test
    fun `POST join with blank name returns 400`() = testApplication {
        val engine = emptyEngine()
        application { installJoinRoute(engine) }

        val response = client.submitForm(
            url = "/join",
            formParameters = Parameters.build { append("name", "   ") },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(engine.state.players.isEmpty())
    }

    @Test
    fun `POST join with missing name returns 400`() = testApplication {
        val engine = emptyEngine()
        application { installJoinRoute(engine) }

        val response = client.submitForm(url = "/join", formParameters = Parameters.Empty)

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST join with unknown name after game started returns 403`() = testApplication {
        val engine = emptyEngine()
        engine.process(StartGame)
        application { installJoinRoute(engine) }

        val response = client.submitForm(
            url = "/join",
            formParameters = Parameters.build { append("name", "Bob") },
        )

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(engine.state.players.isEmpty())
    }

    @Test
    fun `POST join with existing name after game started returns playerId`() = testApplication {
        val engine = emptyEngine()
        engine.process(PlayerJoined("Alice"))
        val aliceId = engine.state.players.first().id
        engine.process(StartGame)
        application { installJoinRoute(engine) }

        val response = client.submitForm(
            url = "/join",
            formParameters = Parameters.build { append("name", "Alice") },
        )

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(aliceId.toString()))
    }
}
