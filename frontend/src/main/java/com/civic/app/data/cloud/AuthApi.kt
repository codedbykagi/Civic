package com.civic.app.data.cloud

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Supabase GoTrue. These calls are the only ones that run without a session, so they authenticate with the
 * anon key directly rather than going through [CloudHttp.bearer].
 */
class AuthApi(private val http: CloudHttp) {

    /**
     * Creates the account. The `handle_new_user` trigger in schema.sql creates the matching profile row.
     * When "Confirm email" is on in the Supabase dashboard the response has no tokens — the caller must then
     * tell the user to check their inbox instead of treating them as signed in.
     */
    suspend fun signUp(email: String, password: String, displayName: String): TokenResponse = cloudCall {
        http.client.post("${CloudConfig.authUrl}/signup") {
            cloudAuth(CloudConfig.anonKey)
            contentType(ContentType.Application.Json)
            setBody(SignUpBody(email.trim(), password, SignUpMetadata(displayName.trim())))
        }.decode()
    }

    suspend fun signIn(email: String, password: String): TokenResponse = cloudCall {
        http.client.post("${CloudConfig.authUrl}/token?grant_type=password") {
            cloudAuth(CloudConfig.anonKey)
            contentType(ContentType.Application.Json)
            setBody(PasswordGrantBody(email.trim(), password))
        }.decode()
    }

    /** Swaps a refresh token for a fresh access token. A failure here means the session is gone for good. */
    suspend fun refresh(refreshToken: String): TokenResponse = cloudCall {
        http.client.post("${CloudConfig.authUrl}/token?grant_type=refresh_token") {
            cloudAuth(CloudConfig.anonKey)
            contentType(ContentType.Application.Json)
            setBody(RefreshGrantBody(refreshToken))
        }.decode()
    }

    /** Best-effort: the local session is cleared whether or not the server is reachable. */
    suspend fun signOut(accessToken: String) {
        runCatching {
            http.client.post("${CloudConfig.authUrl}/logout") { cloudAuth(accessToken) }
        }
    }

    /** Sends a password-reset email. Needs SMTP configured in Supabase; the built-in sender is rate-limited. */
    suspend fun requestPasswordReset(email: String) = cloudCall {
        http.client.post("${CloudConfig.authUrl}/recover") {
            cloudAuth(CloudConfig.anonKey)
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to email.trim()))
        }.ensureSuccess()
    }
}
