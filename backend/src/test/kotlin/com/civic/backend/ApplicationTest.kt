package com.civic.backend

import com.civic.backend.plugins.configureRouting
import com.civic.backend.plugins.configureSerialization
import com.civic.shared.api.ApiRoutes
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Routing only. The empty config replaces application.yaml so the test does not start a database: `module()`
 * calls `configureDatabase()`, and a test that needs Postgres to answer /health is not testing routing.
 */
class ApplicationTest {

    private fun routingOnly(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        environment { config = MapApplicationConfig() }
        application {
            configureSerialization()
            configureRouting()
        }
        block(client)
    }

    @Test
    fun healthCheckReturnsOk() = routingOnly { client ->
        val response = client.get(ApiRoutes.HEALTH)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun feedReturnsAJsonArray() = routingOnly { client ->
        val response = client.get(ApiRoutes.REPORTS)
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().trimStart().startsWith("["))
    }
}
