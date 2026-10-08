package com.civic.backend

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun healthCheckReturnsOk() = testApplication {
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
