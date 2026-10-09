package com.civic.app.data.cloud

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A failed cloud call. [message] is written to be shown to a user as-is; [status] is the HTTP code (0 = network).
 */
class CloudException(
    val status: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Shared JSON config: the server adds columns over time, and an unknown one must not crash the app. */
internal val cloudJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * The HTTP client used for every Supabase call, plus the one rule they all share: send the anon key as `apikey`
 * and a bearer token (the signed-in user's, or the anon key when there is no session).
 *
 * Kept separate from [com.civic.app.data.remote.ApiClient] because that one points at the old Ktor backend.
 */
class CloudHttp(val client: HttpClient, private val accessToken: suspend () -> String?) {

    suspend fun bearer(): String = accessToken() ?: CloudConfig.anonKey

    companion object {
        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) { json(cloudJson) }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 20_000
            }
        }
    }
}

/** Every Supabase request carries the project's anon key; the bearer decides which rows RLS lets through. */
internal fun HttpRequestBuilder.cloudAuth(token: String) {
    header("apikey", CloudConfig.anonKey)
    header(HttpHeaders.Authorization, "Bearer $token")
}

/** Parses a success body, or throws a [CloudException] carrying a message fit for the UI. */
internal suspend inline fun <reified T> HttpResponse.decode(): T {
    ensureSuccess()
    return body()
}

internal suspend fun HttpResponse.ensureSuccess() {
    if (status.value in 200..299) return
    val raw = runCatching { bodyAsText() }.getOrDefault("")
    val parsed = runCatching { cloudJson.decodeFromString<CloudErrorBody>(raw) }.getOrNull()
    val serverMessage = listOfNotNull(
        parsed?.message,
        parsed?.errorDescription,
        parsed?.msg,
        parsed?.error,
    ).firstOrNull { it.isNotBlank() }
    throw CloudException(status.value, friendlyMessage(status.value, serverMessage, parsed?.code))
}

/**
 * Turns Supabase's wording into something a person can act on. The raw text is kept as a fallback so a case we
 * have not seen yet still says something specific.
 */
internal fun friendlyMessage(status: Int, serverMessage: String?, code: String?): String {
    val lower = serverMessage?.lowercase().orEmpty()
    return when {
        lower.contains("invalid login credentials") -> "Wrong email or password."
        lower.contains("email not confirmed") ->
            "Confirm your email first — check your inbox for the link from Supabase."
        lower.contains("user already registered") || code == "user_already_exists" ->
            "That email already has an account. Try signing in instead."
        lower.contains("password should be at least") -> serverMessage!!
        lower.contains("row-level security") ->
            "The server refused that write. You can only change your own posts — see docs/BACKEND_SETUP.md."
        lower.contains("duplicate key") && lower.contains("username") ->
            "That username is taken. Pick another."
        lower.contains("duplicate key") -> "That was already saved."
        status == 401 || status == 403 -> "Your session expired. Sign in again."
        status == 404 -> "The server is missing a table or route — has infra/supabase/schema.sql been run?"
        status == 413 -> "That photo is too large to upload."
        status == 429 -> "Too many requests. Wait a moment and try again."
        status >= 500 -> "The server had a problem. Try again in a minute."
        serverMessage != null && serverMessage.isNotBlank() -> serverMessage
        else -> "Request failed (HTTP $status)."
    }
}

/** Wraps transport failures (no signal, DNS, timeout) in the same type as server errors, so callers catch one thing. */
internal suspend fun <T> cloudCall(block: suspend () -> T): T = try {
    block()
} catch (e: CloudException) {
    throw e
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    throw CloudException(0, "Can't reach the server. Check your connection.", e)
}
