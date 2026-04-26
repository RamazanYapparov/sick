package com.sick.server

import com.sick.engine.GameEngine
import com.sick.event.PlayerJoined
import com.sick.event.PlayerBuzzed
import com.sick.event.QuestionSelected
import com.sick.event.SelectActivePlayer
import com.sick.event.StartGame
import com.sick.model.Answer
import com.sick.model.Content
import com.sick.model.Package
import com.sick.model.Question
import com.sick.model.Round
import com.sick.model.RoundType
import com.sick.model.Theme
import com.sick.server.routes.installBuzzRoute
import io.ktor.client.request.forms.submitForm
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val fixedQuestionId: UUID = UUID.nameUUIDFromBytes("q0".toByteArray())

private fun engineAtShowingQuestion(): Pair<GameEngine, UUID> {
    val pack = Package(
        name = "Test",
        logo = "",
        tags = emptyList(),
        author = "",
        rounds = listOf(
            Round(
                name = "R1",
                type = RoundType.Simple,
                themes = listOf(
                    Theme(
                        name = "T1",
                        questions = listOf(
                            Question(
                                id = fixedQuestionId,
                                price = 100,
                                type = Question.Type.Simple,
                                contents = listOf(Content.Text("Q")),
                                answer = Answer.Simple(right = listOf("A"), wrong = emptyList()),
                            )
                        ),
                    )
                ),
            )
        ),
    )
    val engine = GameEngine(pack)
    engine.process(PlayerJoined("Alice"))
    val playerId = engine.state.players.first().id
    engine.process(StartGame)
    engine.process(SelectActivePlayer(playerId))
    engine.process(QuestionSelected(fixedQuestionId))
    return engine to playerId
}

class BuzzRouteTest {

    @Test
    fun `POST buzz returns 200 when player buzzes in ShowingQuestion phase`() = testApplication {
        val (engine, playerId) = engineAtShowingQuestion()
        application { installBuzzRoute(engine) }

        val response = client.submitForm(
            url = "/buzz",
            formParameters = Parameters.build { append("playerId", playerId.toString()) },
        )

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(playerId, engine.state.answeringPlayerId)
    }

    @Test
    fun `POST buzz returns 400 when playerId is missing`() = testApplication {
        val (engine, _) = engineAtShowingQuestion()
        application { installBuzzRoute(engine) }

        val response = client.submitForm(url = "/buzz", formParameters = Parameters.Empty)

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertNull(engine.state.answeringPlayerId)
    }

    @Test
    fun `POST buzz returns 400 when playerId is not a valid UUID`() = testApplication {
        val (engine, _) = engineAtShowingQuestion()
        application { installBuzzRoute(engine) }

        val response = client.submitForm(
            url = "/buzz",
            formParameters = Parameters.build { append("playerId", "not-a-uuid") },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertNull(engine.state.answeringPlayerId)
    }

    @Test
    fun `POST buzz returns 400 when game is not in ShowingQuestion phase`() = testApplication {
        val pack = Package(name = "T", logo = "", tags = emptyList(), author = "", rounds = emptyList())
        val engine = GameEngine(pack)
        application { installBuzzRoute(engine) }

        val response = client.submitForm(
            url = "/buzz",
            formParameters = Parameters.build { append("playerId", UUID.randomUUID().toString()) },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertNull(engine.state.answeringPlayerId)
    }
}
