package com.civic.app.data.cloud

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** The `profiles` table over PostgREST. RLS restricts writes to the caller's own row. */
class ProfileApi(private val http: CloudHttp) {

    suspend fun fetch(userId: String): ProfileDto? = cloudCall {
        http.client.get("${CloudConfig.restUrl}/profiles") {
            cloudAuth(http.bearer())
            url.parameters.append("id", "eq.$userId")
            url.parameters.append("select", "*")
        }.decode<List<ProfileDto>>().firstOrNull()
    }

    suspend fun update(userId: String, patch: ProfilePatch): ProfileDto? = cloudCall {
        http.client.patch("${CloudConfig.restUrl}/profiles") {
            cloudAuth(http.bearer())
            url.parameters.append("id", "eq.$userId")
            contentType(ContentType.Application.Json)
            header("Prefer", "return=representation")
            setBody(patch)
        }.decode<List<ProfileDto>>().firstOrNull()
    }

    /**
     * Fallback for an account whose trigger did not fire (e.g. the project was created before schema.sql was run).
     * Normally `handle_new_user` has already done this.
     */
    suspend fun createIfMissing(profile: ProfileDto): ProfileDto? = cloudCall {
        http.client.post("${CloudConfig.restUrl}/profiles") {
            cloudAuth(http.bearer())
            contentType(ContentType.Application.Json)
            header("Prefer", "return=representation,resolution=merge-duplicates")
            setBody(profile)
        }.decode<List<ProfileDto>>().firstOrNull()
    }

    suspend fun isUsernameFree(username: String): Boolean = cloudCall {
        http.client.get("${CloudConfig.restUrl}/profiles") {
            cloudAuth(http.bearer())
            url.parameters.append("username", "eq.${username.lowercase()}")
            url.parameters.append("select", "*")
        }.decode<List<ProfileDto>>().isEmpty()
    }
}
