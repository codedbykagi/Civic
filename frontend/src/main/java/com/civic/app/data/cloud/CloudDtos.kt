package com.civic.app.data.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shapes for Supabase (GoTrue + PostgREST). Postgres columns are snake_case, so the mapping lives here
 * instead of leaking @SerialName into the :shared domain models.
 */

// ---------- auth ----------

@Serializable
data class SignUpBody(
    val email: String,
    val password: String,
    /** Picked up by the handle_new_user() trigger to seed profiles.display_name. */
    val data: SignUpMetadata? = null,
)

@Serializable
data class SignUpMetadata(@SerialName("display_name") val displayName: String)

@Serializable
data class PasswordGrantBody(val email: String, val password: String)

@Serializable
data class RefreshGrantBody(@SerialName("refresh_token") val refreshToken: String)

/** GoTrue token response. A sign-up with email confirmation on returns a user but no session. */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: GoTrueUser? = null,
)

@Serializable
data class GoTrueUser(
    val id: String,
    val email: String? = null,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null,
)

/** GoTrue and PostgREST both report failures as JSON; the fields vary, so all are optional. */
@Serializable
data class CloudErrorBody(
    val message: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    val error: String? = null,
    val msg: String? = null,
    val code: String? = null,
    val hint: String? = null,
    val details: String? = null,
)

// ---------- tables ----------

@Serializable
data class ProfileDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
)

/** Fields a user is allowed to change on their own profile (RLS enforces "own row only"). */
@Serializable
data class ProfilePatch(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
)

@Serializable
data class ReportDto(
    val id: String? = null,
    @SerialName("author_id") val authorId: String,
    val category: String,
    val description: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** ISO-8601 UTC; Postgres timestamptz. */
    @SerialName("captured_at") val capturedAt: String,
    @SerialName("created_at") val createdAt: String? = null,
    val status: String = "REPORTED",
    val upvotes: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("time_of_day") val timeOfDay: String? = null,
    val tags: List<String> = emptyList(),
    /** "public" or "private". Safety reports are private and are invisible to other users by RLS. */
    val visibility: String = "public",
    /** "<install uuid>:<room localId>" — makes re-sending the same report a no-op. */
    @SerialName("client_id") val clientId: String? = null,
    /** Joined profile row, present on feed reads (PostgREST embedding). */
    val profiles: ProfileDto? = null,
)

@Serializable
data class CommentDto(
    val id: String? = null,
    @SerialName("report_id") val reportId: String,
    @SerialName("author_id") val authorId: String,
    val text: String,
    @SerialName("created_at") val createdAt: String? = null,
    val profiles: ProfileDto? = null,
)

@Serializable
data class UpvoteDto(
    @SerialName("report_id") val reportId: String,
    @SerialName("voter_id") val voterId: String,
)
