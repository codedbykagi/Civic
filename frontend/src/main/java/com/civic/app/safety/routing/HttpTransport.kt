package com.civic.app.safety.routing

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable

/** Minimal HTTP seam so routers can be tested with canned responses. Non-2xx answers are returned, not thrown. */
interface HttpTransport {
    suspend fun get(url: String): HttpResponseData

    suspend fun postJson(url: String, json: String): HttpResponseData
}

data class HttpResponseData(val status: Int, val body: String)

/**
 * Ktor transport for the public FOSSGIS routing servers. It owns its client (not the app's ApiClient) because those
 * servers require an app-identifying User-Agent and Valhalla asks for an X-Client-Id, and it serialises requests
 * with at least [minIntervalMs] between starts to respect their 1 request/second limit. I/O errors and timeouts
 * propagate to the caller.
 */
class KtorHttpTransport(
    userAgent: String,
    clientId: String,
    private val minIntervalMs: Long = 1100,
) : HttpTransport, Closeable {
    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
        }
        install(UserAgent) { agent = userAgent }
        defaultRequest { header("X-Client-Id", clientId) }
    }
    private val gate = Mutex()
    private var lastStartMs: Long? = null

    override suspend fun get(url: String): HttpResponseData = paced { client.get(url) }

    override suspend fun postJson(url: String, json: String): HttpResponseData =
        paced { client.post(url) { setBody(TextContent(json, ContentType.Application.Json)) } }

    override fun close() = client.close()

    private suspend fun paced(request: suspend () -> HttpResponse): HttpResponseData = gate.withLock {
        lastStartMs?.let { delay(it + minIntervalMs - nowMs()) }
        lastStartMs = nowMs()
        val response = request()
        HttpResponseData(response.status.value, response.bodyAsText())
    }

    private fun nowMs() = System.nanoTime() / 1_000_000
}
