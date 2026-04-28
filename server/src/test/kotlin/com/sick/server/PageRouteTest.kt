package com.sick.server

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

class PageRouteTest {

    @Test
    fun `GET slash returns 200 with HTML content type`() = testApplication {
        application { installPageRoute() }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.contentType()?.match(ContentType.Text.Html) == true)
    }

    @Test
    fun `GET slash HTML contains join endpoint reference`() = testApplication {
        application { installPageRoute() }

        val body = client.get("/").bodyAsText()

        assertTrue(body.contains("/join"))
    }

    @Test
    fun `GET slash HTML contains buzz endpoint reference`() = testApplication {
        application { installPageRoute() }

        val body = client.get("/").bodyAsText()

        assertTrue(body.contains("/buzz"))
    }
}
