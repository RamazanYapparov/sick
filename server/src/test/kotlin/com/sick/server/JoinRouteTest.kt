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
import kotlin.test.assertFalse
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
        assertTrue(response.bodyAsText().contains("reconnectToken"))
    }

    @Test
    fun `POST join with existing name and no token returns 403`() = testApplication {
        val engine = emptyEngine()
        engine.process(PlayerJoined("Alice"))
        application { installJoinRoute(engine) }

        val response = client.submitForm(
            url = "/join",
            formParameters = Parameters.build { append("name", "Alice") },
        )

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(1, engine.state.players.size)
    }

    @Test
    fun `POST join with issued reconnect token returns the same playerId`() = testApplication {
        val engine = emptyEngine()
        application { installJoinRoute(engine) }
        val first = client.submitForm(
            url = "/join",
            formParameters = Parameters.build { append("name", "Alice") },
        )
        val body = first.bodyAsText()
        val playerId = engine.state.players.single().id
        val token = reconnectTokenFrom(body)

        val reconnect = client.submitForm(
            url = "/join",
            formParameters = Parameters.build {
                append("name", "Alice")
                append("reconnectToken", token)
            },
        )

        assertEquals(HttpStatusCode.OK, reconnect.status)
        assertTrue(reconnect.bodyAsText().contains(playerId.toString()))
        assertEquals(1, engine.state.players.size)
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
    fun `POST join with existing name after game started requires reconnect token`() = testApplication {
        val engine = emptyEngine()
        application { installJoinRoute(engine) }
        val first = client.submitForm(
            url = "/join",
            formParameters = Parameters.build { append("name", "Alice") },
        )
        val token = reconnectTokenFrom(first.bodyAsText())
        val aliceId = engine.state.players.single().id
        engine.process(StartGame)

        val rejected = client.submitForm(
            url = "/join",
            formParameters = Parameters.build { append("name", "Alice") },
        )
        val accepted = client.submitForm(
            url = "/join",
            formParameters = Parameters.build {
                append("name", "Alice")
                append("reconnectToken", token)
            },
        )

        assertEquals(HttpStatusCode.Forbidden, rejected.status)
        assertFalse(rejected.bodyAsText().contains(aliceId.toString()))
        assertEquals(HttpStatusCode.OK, accepted.status)
        assertTrue(accepted.bodyAsText().contains(aliceId.toString()))
    }

    private fun reconnectTokenFrom(json: String): String =
        requireNotNull(Regex("\\\"reconnectToken\\\":\\\"([^\\\"]+)\\\"").find(json)?.groupValues?.get(1))
}
