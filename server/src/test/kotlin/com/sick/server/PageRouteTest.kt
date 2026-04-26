package com.sick.server

import com.sick.engine.GameEngine
import com.sick.event.PlayerJoined
import com.sick.model.Package
import com.sick.server.routes.installPageRoute
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun emptyEngine() = GameEngine(
    Package(name = "T", logo = "", tags = emptyList(), author = "", rounds = emptyList())
)

class PageRouteTest {

    @Test
    fun `GET slash returns 200 with HTML content type`() = testApplication {
        application { installPageRoute(emptyEngine()) }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.contentType()?.match(ContentType.Text.Html) == true)
    }

    @Test
    fun `GET slash HTML contains buzz endpoint reference`() = testApplication {
        application { installPageRoute(emptyEngine()) }

        val body = client.get("/").bodyAsText()

        assertTrue(body.contains("/buzz"))
    }

    @Test
    fun `GET slash HTML lists current player names`() = testApplication {
        val engine = emptyEngine()
        engine.process(PlayerJoined("Alice"))
        engine.process(PlayerJoined("Bob"))
        application { installPageRoute(engine) }

        val body = client.get("/").bodyAsText()

        assertTrue(body.contains("Alice"))
        assertTrue(body.contains("Bob"))
    }
}
